package com.xiaozhi.architecture;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * 发布镜像的流水线。
 * <p>
 * 原来这里写死了上游仓库名 `joey-zhou/xiaozhi-esp32-server-java`，fork 之后（本仓是
 * `GitHub-lcb/xiaozhi-esp32-server-java`）这条断言必然失败——它把「只从本仓库发布」的意图
 * 错误地表达成了「只从某个特定仓库发布」。
 * <p>
 * 现在的判据是**自洽性**：镜像推到哪里，守卫就必须拦住哪里。
 * <ul>
 *   <li>镜像地址必须用 `${{ github.repository_owner }}` 动态拼，不能写死 owner——写死了 fork 会往别人的命名空间推；</li>
 *   <li>发布守卫必须存在，且仓库名与本仓一致（默认从 git remote 推导，可用 `XIAOZHI_RELEASE_REPO` 覆盖）。</li>
 * </ul>
 */
class ImagePublishGuardTest {

    /** 从 if: github.repository == 'owner/repo' 里取出 repo（纯字符串扫描，避免正则二次转义） */
    static String guardedRepository(String yaml) {
        for (String line : yaml.split("\n")) {
            String trimmed = line.trim();
            if (!trimmed.startsWith("if:") || !trimmed.contains("github.repository ==")) {
                continue;
            }
            int firstQuote = trimmed.indexOf("\u0027");
            int lastQuote = trimmed.lastIndexOf("\u0027");
            if (firstQuote >= 0 && lastQuote > firstQuote) {
                return trimmed.substring(firstQuote + 1, lastQuote);
            }
        }
        return null;
    }
    /** 写死 owner 的镜像行：images: ghcr.io/some-owner/...（不含变量表达式） */
    static boolean hasHardCodedOwner(String yaml) {
        for (String line : yaml.split("\n")) {
            String trimmed = line.trim();
            if (trimmed.startsWith("images:") && trimmed.contains("ghcr.io/")) {
                String rest = trimmed.substring(trimmed.indexOf("ghcr.io/") + "ghcr.io/".length());
                if (!rest.startsWith("$") && rest.contains("/")) {
                    return true;
                }
            }
        }
        return false;
    }

    @Test
    void imagesArePublishedOnlyFromTheRepositoryThatOwnsTheWorkflow() throws IOException {
        // 测试的工作目录是 xiaozhi-server 模块
        Path workflow = Path.of("..", ".github", "workflows", "docker.yml");
        assumeTrue(Files.exists(workflow), "本仓不带发布流水线");

        String yaml = Files.readString(workflow, StandardCharsets.UTF_8);

        assertThat(yaml).as("流水线必须真的推送镜像").contains("push: true");

        assertThat(hasHardCodedOwner(yaml))
            .as("镜像地址必须用 github.repository_owner 动态拼接，不能写死某个账号，否则 fork 会推到别人的命名空间")
            .isFalse();

        String guardedRepository = guardedRepository(yaml);
        assertThat(guardedRepository)
            .as("缺少发布守卫：if: github.repository == '<owner>/<repo>'，任何 fork 都会往 GHCR 推镜像")
            .isNotNull();

        Optional<String> expected = expectedRepository();
        if (expected.isPresent()) {
            assertThat(guardedRepository)
                .as("守卫里的仓库与当前仓库不一致：改了仓库名却忘了改 docker.yml，流水线永远不触发或是往错的地方发")
                .isEqualToIgnoringCase(expected.get());
        }
    }

    /**
     * 当前仓库的 owner/repo：优先读环境变量，其次从 git remote 推导；都拿不到就不做这条强断言。
     */
    private Optional<String> expectedRepository() {
        String fromEnv = System.getenv("XIAOZHI_RELEASE_REPO");
        if (fromEnv != null && fromEnv.contains("/")) {
            return Optional.of(fromEnv.trim());
        }
        try {
            Process process = new ProcessBuilder("git", "remote", "get-url", "origin")
                .redirectErrorStream(true)
                .start();
            if (!process.waitFor(10, TimeUnit.SECONDS)) {
                process.destroyForcibly();
                return Optional.empty();
            }
            String url = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8).trim();
            return Optional.ofNullable(parseOwnerRepo(url));
        } catch (IOException | InterruptedException e) {
            if (e instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            return Optional.empty();
        }
    }

    /** 支持 git@github.com:owner/repo.git 与 https://github.com/owner/repo.git */
    static String parseOwnerRepo(String remoteUrl) {
        if (remoteUrl == null || remoteUrl.isBlank()) {
            return null;
        }
        String cleaned = remoteUrl.trim();
        int scheme = cleaned.indexOf("://");
        if (scheme >= 0) {
            cleaned = cleaned.substring(scheme + 3);
            int hostEnd = cleaned.indexOf('/');
            if (hostEnd < 0) {
                return null;
            }
            cleaned = cleaned.substring(hostEnd + 1);
        } else {
            int at = cleaned.indexOf('@');
            if (at >= 0) {
                cleaned = cleaned.substring(at + 1);
            }
            int colon = cleaned.indexOf(':');
            if (colon >= 0) {
                cleaned = cleaned.substring(colon + 1);
            }
        }
        if (cleaned.endsWith(".git")) {
            cleaned = cleaned.substring(0, cleaned.length() - 4);
        }
        return cleaned.isBlank() || !cleaned.contains("/") ? null : cleaned;
    }
}
