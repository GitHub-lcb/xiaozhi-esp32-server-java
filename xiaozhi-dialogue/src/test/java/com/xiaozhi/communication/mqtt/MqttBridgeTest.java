package com.xiaozhi.communication.mqtt;

import com.xiaozhi.channel.service.DeviceChannelService;
import com.xiaozhi.common.model.bo.DeviceChannelBO;
import com.xiaozhi.communication.common.ChatSession;
import com.xiaozhi.communication.common.MessageHandler;
import com.xiaozhi.communication.common.SessionManager;
import com.xiaozhi.communication.domain.HelloMessage;
import io.moquette.broker.Server;
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.net.ServerSocket;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Properties;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BooleanSupplier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * MQTT 通道端到端用例：内嵌 Moquette broker 起一个真实 MQTT 环境，
 * 模拟设备发 hello，断言桥把消息喂给同一套 MessageHandler，并把会话登记成在线；
 * 再从会话下发一条 JSON，断言设备真的在主题上收到了。
 */
class MqttBridgeTest {

    private static final String DEVICE_ID = "aa:bb:cc:dd:ee:ff";
    private static final String HELLO_JSON = "{\"type\":\"hello\",\"version\":1,\"transport\":\"mqtt\","
        + "\"audio_params\":{\"format\":\"opus\",\"sample_rate\":16000,\"channels\":1,\"frame_duration\":60}}";

    private static Server broker;
    private static int port;
    private static String brokerUrl;

    private MqttBridge bridge;
    private MessageHandler messageHandler;
    private SessionManager sessionManager;
    private DeviceChannelService deviceChannelService;
    private MqttClient deviceClient;
    private final AtomicReference<ChatSession> sessionRef = new AtomicReference<>();

    @BeforeAll
    static void startBroker() throws Exception {
        try (ServerSocket socket = new ServerSocket(0)) {
            port = socket.getLocalPort();
        }
        brokerUrl = "tcp://127.0.0.1:" + port;
        Properties props = new Properties();
        props.setProperty("port", String.valueOf(port));
        props.setProperty("host", "127.0.0.1");
        props.setProperty("allow_anonymous", "true");
        // Moquette 用 '/' 拆持久化路径，Windows 的反斜杠会让它 substring(0,-1) 越界
        props.setProperty("persistent_store",
            Files.createTempDirectory("moquette-test").toString().replace('\\', '/') + "/store.h2");
        broker = new Server();
        broker.startServer(props);
    }

    @AfterAll
    static void stopBroker() {
        if (broker != null) {
            broker.stopServer();
        }
    }

    @BeforeEach
    void setUp() throws Exception {
        sessionRef.set(null);
        MqttProperties properties = new MqttProperties();
        properties.setEnabled(true);
        properties.setBrokerUrl(brokerUrl);
        properties.setClientId("test-bridge-" + port);
        properties.setTopicTemplate("devices/p2p/{deviceId}");

        messageHandler = mock(MessageHandler.class);
        sessionManager = mock(SessionManager.class);
        deviceChannelService = mock(DeviceChannelService.class);
        // 真实 SessionManager 会把会话登记下来供后续查找，这里用桩模拟同样的语义
        doAnswer(invocation -> {
            sessionRef.set(invocation.getArgument(0));
            return null;
        }).when(messageHandler).afterConnection(any(ChatSession.class), anyString());
        when(sessionManager.getSessionByDeviceId(DEVICE_ID)).thenAnswer(invocation -> sessionRef.get());

        bridge = new MqttBridge(properties, messageHandler, sessionManager, deviceChannelService);
        bridge.start();

        deviceClient = new MqttClient(brokerUrl, "test-device-" + port, new MemoryPersistence());
        deviceClient.connect();
        deviceClient.subscribe(properties.topicFor(DEVICE_ID), 1);
    }

    @AfterEach
    void tearDown() throws Exception {
        if (deviceClient != null && deviceClient.isConnected()) {
            deviceClient.disconnect();
            deviceClient.close();
        }
        if (bridge != null) {
            bridge.stop();
        }
    }

    @Test
    void deviceHelloOpensSessionAndRegistersChannelOnline() {
        awaitSessionOpened();

        verify(messageHandler, timeout(5000)).handleMessage(any(HelloMessage.class), anyString());
        verify(deviceChannelService, timeout(5000))
            .markOnline(eq(DEVICE_ID), eq(DeviceChannelBO.CHANNEL_MQTT), anyString());
        assertThat(sessionRef.get()).isInstanceOf(MqttChatSession.class);
    }

    @Test
    void downlinkTextReachesDeviceTopic() throws Exception {
        BlockingQueue<String> received = new LinkedBlockingQueue<>();
        deviceClient.setCallback(new MqttCallback() {
            @Override
            public void connectionLost(Throwable cause) {
            }

            @Override
            public void messageArrived(String topic, MqttMessage message) {
                received.add(new String(message.getPayload(), StandardCharsets.UTF_8));
            }

            @Override
            public void deliveryComplete(IMqttDeliveryToken token) {
            }
        });

        awaitSessionOpened();
        sessionRef.get().sendTextMessage("{\"type\":\"tts\",\"state\":\"start\"}");

        // 设备自己也订阅了同一主题，会先收到自己发的 hello，这里要等到 tts 那条
        String payload = awaitPayload(received, "tts", 5000);
        assertThat(payload).isNotNull().contains("\"tts\"");
    }

    @Test
    void nonDeviceTopicIsIgnored() throws Exception {
        deviceClient.publish("other/p2p/" + DEVICE_ID,
            new MqttMessage(HELLO_JSON.getBytes(StandardCharsets.UTF_8)));
        Thread.sleep(800);

        assertThat(sessionRef.get()).isNull();
        verify(messageHandler, never()).afterConnection(any(ChatSession.class), eq(DEVICE_ID));
    }

    /** broker 的订阅在 connectComplete 回调里完成，早发的消息会丢，重试到会话建立为止 */
    private void awaitSessionOpened() {
        awaitTrue(() -> {
            if (sessionRef.get() != null) {
                return true;
            }
            try {
                deviceClient.publish("devices/p2p/" + DEVICE_ID,
                    new MqttMessage(HELLO_JSON.getBytes(StandardCharsets.UTF_8)));
            } catch (Exception e) {
                return false;
            }
            sleepQuietly(150);
            return sessionRef.get() != null;
        }, 8000);
        assertThat(sessionRef.get()).as("MQTT 桥应在收到 hello 后建立会话").isNotNull();
        verify(messageHandler, atLeastOnce()).afterConnection(any(ChatSession.class), eq(DEVICE_ID));
    }

    private String awaitPayload(BlockingQueue<String> received, String keyword, long timeoutMillis) throws Exception {
        long deadline = System.currentTimeMillis() + timeoutMillis;
        while (System.currentTimeMillis() < deadline) {
            String payload = received.poll(200, TimeUnit.MILLISECONDS);
            if (payload != null && payload.contains(keyword)) {
                return payload;
            }
        }
        return null;
    }

    private void awaitTrue(BooleanSupplier condition, long timeoutMillis) {
        long deadline = System.currentTimeMillis() + timeoutMillis;
        while (System.currentTimeMillis() < deadline) {
            if (condition.getAsBoolean()) {
                return;
            }
            sleepQuietly(100);
        }
    }

    private void sleepQuietly(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
