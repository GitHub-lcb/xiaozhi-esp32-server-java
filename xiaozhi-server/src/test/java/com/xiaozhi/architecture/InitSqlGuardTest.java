package com.xiaozhi.architecture;

import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfo;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIf;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 在空库上用只有库级权限的账号把 db/init.sql 真跑一遍，再核对跑完的表结构装得下每个 DO 的字段。
 * <p>
 * 脚本完整执行只发生在新建库时：开发者自己的库早就建好了，脚本里引用不存在的表或列、
 * 写死的库名、需要全局权限的语句，本地都不会暴露，只有别人照文档执行时才炸。
 *
 * <p>连接信息走 XIAOZHI_INIT_SQL_TEST_URL / _USERNAME / _PASSWORD，CI 的 backend 工作流挂了 MySQL service。
 * 本地没配就跳过，CI 里没配直接失败，免得工作流改坏了还假绿。
 *
 * <p>只能指向空库，且每次跑前都要重建：脚本带 DROP TABLE，指向有数据的库会把数据删光，所以非空库直接拒跑。
 * 建账号、建库那段带 bootstrap-only 标记，测试会整段跳过（CI 账号没有全局权限，库名也不是 xiaozhi）。
 */
@EnabledIf(value = "shouldRun", disabledReason = "未配置 XIAOZHI_INIT_SQL_TEST_URL，跳过初始化脚本校验")
class InitSqlGuardTest {

    private static final String URL = System.getenv("XIAOZHI_INIT_SQL_TEST_URL");
    private static final String BOOTSTRAP_START = "-- >>> bootstrap-only";
    private static final String BOOTSTRAP_END = "-- <<< bootstrap-only";

    private static Connection connection;

    /** CI 里不看有没有配库一律执行，没配就在下面断言失败 */
    static boolean shouldRun() {
        return System.getenv("CI") != null || (URL != null && !URL.isBlank());
    }

    @BeforeAll
    static void applyInitSql() throws SQLException {
        assertThat(URL)
            .as("CI 里没配 XIAOZHI_INIT_SQL_TEST_URL，初始化脚本校验会被静默跳过")
            .isNotBlank();

        connection = DriverManager.getConnection(URL,
            System.getenv("XIAOZHI_INIT_SQL_TEST_USERNAME"),
            System.getenv("XIAOZHI_INIT_SQL_TEST_PASSWORD"));

        assertThat(query("SELECT TABLE_NAME, '' FROM information_schema.TABLES WHERE TABLE_SCHEMA = DATABASE()"))
            .as("初始化校验只能指向空库，db/init.sql 会 DROP TABLE")
            .isEmpty();

        List<String> statements = statements(stripBootstrap(readInitSql()));
        try (Statement statement = connection.createStatement()) {
            for (String sql : statements) {
                statement.execute(sql);
            }
        }
        assertThat(statements)
            .as("db/init.sql 里没解析出语句，本类规则会退化成空真")
            .hasSizeGreaterThan(10);
    }

    @AfterAll
    static void closeConnection() throws SQLException {
        if (connection != null) {
            connection.close();
        }
    }

    @Test
    void initSqlCreatesEveryTable() throws SQLException {
        Set<String> tables = query(
            "SELECT TABLE_NAME, '' FROM information_schema.TABLES WHERE TABLE_SCHEMA = DATABASE()").keySet();

        assertThat(tables)
            .as("db/init.sql 跑完没建出应有的表")
            .contains("sys_user", "sys_auth_role", "sys_permission", "sys_device", "sys_config", "sys_message");
        assertThat(tables)
            .as("建出来的表明显偏少，脚本大概率没跑全")
            .hasSizeGreaterThanOrEqualTo(10);
    }

    /**
     * 脚本全跑通还不够：DO 加了字段却没改 db/init.sql，新库照样缺列，要到查询时才报 Unknown column。
     */
    @Test
    void schemaCoversEveryDataObjectField() throws SQLException {
        Map<String, Set<String>> columnsByTable = query(
            "SELECT TABLE_NAME, COLUMN_NAME FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE()");

        List<Class<?>> dataObjects = new ClassFileImporter()
            .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
            .importPackages("com.xiaozhi")
            .stream()
            .filter(javaClass -> javaClass.isAnnotatedWith(TableName.class))
            .<Class<?>>map(JavaClass::reflect)
            .toList();
        assertThat(dataObjects)
            .as("没扫到足够的 DO，本条规则会假绿")
            .hasSizeGreaterThanOrEqualTo(10);

        // 与生产一致：map-underscore-to-camel-case 关着，字段名就是列名
        MybatisConfiguration configuration = new MybatisConfiguration();
        configuration.setMapUnderscoreToCamelCase(false);
        MapperBuilderAssistant assistant = new MapperBuilderAssistant(configuration, "");

        List<String> missing = new ArrayList<>();
        for (Class<?> dataObject : dataObjects) {
            TableInfo tableInfo = TableInfoHelper.initTableInfo(assistant, dataObject);
            Set<String> columns = columnsByTable.getOrDefault(tableInfo.getTableName(), Set.of());
            List<String> expected = new ArrayList<>();
            if (tableInfo.havePK()) {
                expected.add(tableInfo.getKeyColumn());
            }
            tableInfo.getFieldList().forEach(field -> expected.add(field.getColumn()));
            expected.stream()
                .map(column -> column.replace("`", ""))
                .filter(column -> !columns.contains(column))
                .forEach(column -> missing.add(
                    tableInfo.getTableName() + "." + column + "（" + dataObject.getSimpleName() + "）"));
        }
        assertThat(missing)
            .as("DO 里有、db/init.sql 建出来的库里没有的列")
            .isEmpty();
    }

    /** db/init.sql 里建账号/建库那段需要全局权限且写死库名，自动化测试整段跳过 */
    private static String stripBootstrap(String sql) {
        StringBuilder kept = new StringBuilder();
        boolean skipping = false;
        for (String line : sql.split("\\R")) {
            if (line.contains(BOOTSTRAP_START)) {
                skipping = true;
                continue;
            }
            if (line.contains(BOOTSTRAP_END)) {
                skipping = false;
                continue;
            }
            if (!skipping) {
                kept.append(line).append('\n');
            }
        }
        return kept.toString();
    }

    /** 脚本一行一条语句（mysqldump 风格），按「分号 + 换行」切开即可 */
    private static List<String> statements(String sql) {
        List<String> list = new ArrayList<>();
        for (String chunk : sql.split(";\\s*\\R")) {
            String trimmed = chunk.strip();
            if (!trimmed.isEmpty()) {
                list.add(trimmed);
            }
        }
        return list;
    }

    private static String readInitSql() {
        try {
            return Files.readString(initSqlFile());
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /** surefire 的工作目录是模块根；从仓库根跑时是当前目录 */
    private static Path initSqlFile() {
        Path fromModuleRoot = Paths.get("../db/init.sql");
        return Files.isRegularFile(fromModuleRoot) ? fromModuleRoot : Paths.get("db/init.sql");
    }

    /** 查两列结果，按第一列分组收第二列 */
    private static Map<String, Set<String>> query(String sql) throws SQLException {
        Map<String, Set<String>> grouped = new HashMap<>();
        try (Statement statement = connection.createStatement();
             ResultSet resultSet = statement.executeQuery(sql)) {
            while (resultSet.next()) {
                grouped.computeIfAbsent(resultSet.getString(1), key -> new HashSet<>()).add(resultSet.getString(2));
            }
        }
        return grouped;
    }
}
