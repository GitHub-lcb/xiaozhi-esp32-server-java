import { createRouter, createWebHistory } from 'vue-router'
import type { RouteRecordRaw } from 'vue-router'
import MainLayout from '../layouts/MainLayout.vue'

const routes: RouteRecordRaw[] = [
  {
    path: '/login',
    name: 'login',
    component: () => import('../views/LoginView.vue'),
    meta: {
      title: 'router.title.login',
      requiresAuth: false,
    },
  },
  {
    path: '/register',
    name: 'register',
    component: () => import('../views/RegisterView.vue'),
    meta: {
      title: 'router.title.register',
      requiresAuth: false,
    },
  },
  {
    path: '/forget',
    name: 'forget',
    component: () => import('../views/ForgetView.vue'),
    meta: {
      title: 'router.title.forget',
      requiresAuth: false,
    },
  },

  // 主应用路由
  {
    path: '/',
    component: MainLayout,
    // redirect: '/dashboard', // 在路由守卫中动态处理
    children: [
      {
        path: 'dashboard',
        name: 'dashboard',
        component: () => import('../views/DashboardView.vue'),
        meta: {
          title: 'router.title.dashboard',
          icon: 'DashboardOutlined',
          requiresAuth: true,
          permission: 'system:dashboard',
        },
      },
      {
        path: 'user',
        name: 'user',
        component: () => import('../views/UserView.vue'),
        meta: {
          title: 'router.title.user',
          parent: 'router.parent.settings',
          icon: 'TeamOutlined',
          requiresAuth: true,
          permission: 'system:user',
        },
      },
      {
        path: 'device',
        name: 'device',
        component: () => import('../views/DeviceView.vue'),
        meta: {
          title: 'router.title.device',
          parent: 'router.parent.deviceCenter',
          icon: 'RobotOutlined',
          requiresAuth: true,
          permission: 'system:device',
        },
      },
      {
        path: 'portal',
        name: 'portal',
        component: () => import('../views/UserPortalView.vue'),
        meta: {
          title: 'router.title.portal',
          parent: 'router.parent.experience',
          icon: 'AppstoreOutlined',
          requiresAuth: true,
          permission: 'system:dashboard',
        },
      },
      {
        path: 'plugin',
        name: 'plugin',
        component: () => import('../views/PluginView.vue'),
        meta: {
          title: 'router.title.plugin',
          parent: 'router.parent.settings',
          icon: 'ApiOutlined',
          requiresAuth: true,
          permission: 'system:config',
        },
      },
      {
        path: 'device-group',
        name: 'device-group',
        component: () => import('../views/DeviceGroupView.vue'),
        meta: {
          title: 'router.title.deviceGroup',
          parent: 'router.parent.deviceCenter',
          icon: 'DeploymentUnitOutlined',
          requiresAuth: true,
          permission: 'system:device',
        },
      },
      {
        path: 'music',
        name: 'music',
        component: () => import('../views/MusicView.vue'),
        meta: {
          title: 'router.title.music',
          parent: 'router.parent.agent',
          icon: 'CustomerServiceOutlined',
          requiresAuth: true,
          permission: 'system:file',
        },
      },
      {
        path: 'theme-resource',
        name: 'theme-resource',
        component: () => import('../views/ThemeResourceView.vue'),
        meta: {
          title: 'router.title.themeResource',
          parent: 'router.parent.agent',
          icon: 'ShopOutlined',
          requiresAuth: true,
          permission: 'system:role:api:list',
        },
      },
      {
        path: 'voice-clone',
        name: 'voice-clone',
        component: () => import('../views/VoiceCloneView.vue'),
        meta: {
          title: 'router.title.voiceClone',
          parent: 'router.parent.agent',
          icon: 'AudioOutlined',
          requiresAuth: true,
          permission: 'system:voice-clone',
        },
      },
      {
        path: 'memory-graph',
        name: 'memory-graph',
        component: () => import('../views/MemoryGraphView.vue'),
        meta: {
          title: 'router.title.memoryGraph',
          parent: 'router.parent.agent',
          icon: 'ShareAltOutlined',
          requiresAuth: true,
          permission: 'system:memory-graph',
        },
      },
      {
        path: 'memory-fact',
        name: 'memory-fact',
        component: () => import('../views/MemoryFactView.vue'),
        meta: {
          title: 'router.title.memoryFact',
          parent: 'router.parent.agent',
          icon: 'DatabaseOutlined',
          requiresAuth: true,
          permission: 'system:memory-fact',
        },
      },
      {
        path: 'voiceprint',
        name: 'voiceprint',
        component: () => import('../views/VoiceprintView.vue'),
        meta: {
          title: 'router.title.voiceprint',
          parent: 'router.parent.agent',
          icon: 'SoundOutlined',
          requiresAuth: true,
          permission: 'system:voiceprint',
        },
      },
      {
        path: 'knowledge',
        name: 'knowledge',
        component: () => import('../views/KnowledgeView.vue'),
        meta: {
          title: 'router.title.knowledge',
          parent: 'router.parent.agent',
          icon: 'BookOutlined',
          requiresAuth: true,
          permission: 'system:knowledge',
        },
      },
      {
        path: 'reminder',
        name: 'reminder',
        component: () => import('../views/ReminderView.vue'),
        meta: {
          title: 'router.title.reminder',
          parent: 'router.parent.message',
          icon: 'ClockCircleOutlined',
          requiresAuth: true,
          permission: 'system:reminder',
        },
      },
      {
        path: 'push',
        name: 'push',
        component: () => import('../views/PushView.vue'),
        meta: {
          title: 'router.title.push',
          parent: 'router.parent.message',
          icon: 'NotificationOutlined',
          requiresAuth: true,
          permission: 'system:push',
        },
      },
      {
        path: 'firmware',
        name: 'firmware',
        component: () => import('../views/FirmwareView.vue'),
        meta: {
          title: 'router.title.firmware',
          parent: 'router.parent.deviceCenter',
          icon: 'CloudUploadOutlined',
          requiresAuth: true,
          permission: 'system:firmware',
        },
      },
      {
        path: 'role',
        name: 'role',
        component: () => import('../views/RoleView.vue'),
        meta: {
          title: 'router.title.role',
          parent: 'router.parent.agent',
          icon: 'UserAddOutlined',
          requiresAuth: true,
          permission: 'system:role',
        },
      },
      {
        path: 'template',
        name: 'template',
        component: () => import('../views/TemplateView.vue'),
        meta: {
          title: 'router.title.template',
          parent: 'router.parent.agent',
          icon: 'SnippetsOutlined',
          requiresAuth: true,
          permission: 'system:prompt-template',
          hideInMenu: true
        },
      },
      // 对话记录：逐条聊天记录与对话摘要两个 Tab
      {
        path: 'memory/chat',
        name: 'memory-chat',
        component: () => import('../views/MemoryManagementView.vue'),
        meta: {
          title: 'router.title.conversationRecords',
          parent: 'router.parent.memoryManagement',
          requiresAuth: true,
          permission: 'system:role',
        },
      },
      // 配置管理
      {
        path: 'config/model',
        name: 'config-model',
        component: () => import('../views/config/ModelConfigView.vue'),
        meta: {
          title: 'router.title.modelConfig',
          parent: 'router.parent.configManagement',
          requiresAuth: true,
          permission: 'system:config',
        },
      },
      {
        path: 'config/agent',
        name: 'config-agent',
        component: () => import('../views/config/AgentView.vue'),
        meta: {
          title: 'router.title.agent',
          parent: 'router.parent.configManagement',
          requiresAuth: true,
          permission: 'system:config:agent',
        },
      },
      {
        path: 'config/stt',
        name: 'config-stt',
        component: () => import('../views/config/SttConfigView.vue'),
        meta: {
          title: 'router.title.sttConfig',
          parent: 'router.parent.configManagement',
          requiresAuth: true,
          permission: 'system:config',
        },
      },
      {
        path: 'config/tts',
        name: 'config-tts',
        component: () => import('../views/config/TtsConfigView.vue'),
        meta: {
          title: 'router.title.ttsConfig',
          parent: 'router.parent.configManagement',
          requiresAuth: true,
          permission: 'system:config',
        },
      },
      {
        path: 'config/oss',
        name: 'config-oss',
        component: () => import('../views/config/OssConfigView.vue'),
        meta: {
          title: 'router.title.ossConfig',
          parent: 'router.parent.configManagement',
          requiresAuth: true,
          permission: 'system:config',
        },
      },
      // Web 聊天
      {
        path: 'chat',
        name: 'chat',
        component: () => import('../views/ChatView.vue'),
        meta: {
          title: 'router.title.chat',
          parent: 'router.parent.experience',
          icon: 'MessageOutlined',
          requiresAuth: true,
          permission: 'system:chat',
        },
      },
      {
        path: 'auth-role',
        name: 'auth-role',
        component: () => import('../views/AuthRoleView.vue'),
        meta: {
          title: 'router.title.authRole',
          parent: 'router.parent.settings',
          icon: 'SafetyCertificateOutlined',
          requiresAuth: true,
          permission: 'system:auth-role',
        },
      },
      // 个人中心
      {
        path: 'setting/account',
        name: 'setting-account',
        component: () => import('../views/setting/AccountView.vue'),
        meta: {
          title: 'router.title.account',
          parent: 'router.parent.settings',
          requiresAuth: true,
          permission: 'system:setting',
        },
      },
    ],
  },

  // 异常页面
  {
    path: '/403',
    name: '403',
    component: () => import('../views/exception/403.vue'),
    meta: {
      title: 'router.title.error403',
      requiresAuth: false,
    },
  },
  {
    path: '/404',
    name: '404',
    component: () => import('../views/exception/404.vue'),
    meta: {
      title: 'router.title.error404',
      requiresAuth: false,
    },
  },

  // 捕获所有未匹配的路由
  {
    path: '/:pathMatch(.*)*',
    redirect: '/404',
  },
]

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes,
})

export default router
