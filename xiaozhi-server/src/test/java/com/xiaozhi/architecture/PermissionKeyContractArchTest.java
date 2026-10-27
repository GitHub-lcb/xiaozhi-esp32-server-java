package com.xiaozhi.architecture;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 权限键是「Java 注解字面量」与「db/init.sql 里的字符串」两头对齐的，编译器一个字都拦不住，
 * 两边各改各的不会有任何报错——只会变成运行期的 403，或者一条谁也没在用的权限。
 */
class PermissionKeyContractArchTest {

    private static final Pattern LINE_COMMENT = Pattern.compile("--[^\\n]*");
    /** 匹配初始化脚本里以 system: 开头的权限键字面量 */
    private static final Pattern SQL_PERMISSION_KEY = Pattern.compile("'(system:[A-Za-z0-9:_.\\-]+)'");

    private static JavaClasses serverClasses;
    private static Set<String> declaredInInitSql;

    @BeforeAll
    static void loadSources() {
        serverClasses = new ClassFileImporter()
            .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
            .importPackages("com.xiaozhi");
        declaredInInitSql = readInitSqlPermissionKeys();
    }

    @Test
    void initSqlIsActuallyReadable() {
        assertThat(declaredInInitSql)
            .as("读不到 db/init.sql 里的权限键，本类其余规则会退化成空真")
            .hasSizeGreaterThan(40);
    }

    /**
     * 代码里挂的每个权限键都必须在 db/init.sql 里建过。
     * 漏建的那个端点对所有人（含 admin）恒定 403，而且只有真跑到才会发现。
     */
    @Test
    void everyPermissionKeyInCodeIsDeclaredInInitSql() {
        Set<String> usedInCode = permissionKeysUsedInCode();

        assertThat(usedInCode)
            .as("一个 @SaCheckPermission 都没扫到，判定面已经失效")
            .isNotEmpty();
        assertThat(usedInCode)
            .as("这些权限键只存在于 Java 注解里，sys_permission 从来没有过对应行，端点会恒定 403")
            .isSubsetOf(declaredInInitSql);
    }

    private Set<String> permissionKeysUsedInCode() {
        Set<String> keys = new LinkedHashSet<>();
        serverClasses.stream()
            .flatMap(javaClass -> javaClass.getMethods().stream())
            .forEach(method -> method.tryGetAnnotationOfType(SaCheckPermission.class)
                .ifPresent(annotation -> keys.addAll(Arrays.asList(annotation.value()))));
        serverClasses.forEach(javaClass -> javaClass.tryGetAnnotationOfType(SaCheckPermission.class)
            .ifPresent(annotation -> keys.addAll(Arrays.asList(annotation.value()))));
        return keys;
    }

    /**
     * 把 db/init.sql 里的 INSERT / DELETE 按顺序 replay 一遍，得出**最终**还存在的权限键。
     * <p>
     * 不能只把所有 {@code 'system:...'} 字面量收成一个集合：DELETE 语句里也带着这些字面量，
     * 那样一来「把一条仍在用的权限删了」这件事，本类会完全无感——集合里它还在。
     */
    private static Set<String> readInitSqlPermissionKeys() {
        String sql;
        try {
            sql = Files.readString(initSqlFile());
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }

        Set<String> keys = new LinkedHashSet<>();
        // 先去掉行注释，否则被注释掉的 INSERT 会被当成真的声明
        String stripped = LINE_COMMENT.matcher(sql).replaceAll("");
        for (String statement : stripped.split(";")) {
            Matcher matcher = SQL_PERMISSION_KEY.matcher(statement);
            boolean removing = statement.stripLeading().regionMatches(true, 0, "DELETE", 0, 6);
            while (matcher.find()) {
                if (removing) {
                    keys.remove(matcher.group(1));
                } else {
                    keys.add(matcher.group(1));
                }
            }
        }
        return keys;
    }

    /** surefire 的工作目录是模块根；从仓库根跑时是当前目录 */
    private static Path initSqlFile() {
        Path fromModuleRoot = Paths.get("../db/init.sql");
        return Files.isRegularFile(fromModuleRoot) ? fromModuleRoot : Paths.get("db/init.sql");
    }
}
