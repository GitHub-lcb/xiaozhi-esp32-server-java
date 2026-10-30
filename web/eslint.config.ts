import { globalIgnores } from 'eslint/config'
import { defineConfigWithVueTs, vueTsConfigs } from '@vue/eslint-config-typescript'
import pluginVue from 'eslint-plugin-vue'
import pluginOxlint from 'eslint-plugin-oxlint'
import skipFormatting from '@vue/eslint-config-prettier/skip-formatting'

// To allow more languages other than `ts` in `.vue` files, uncomment the following lines:
// import { configureVueProject } from '@vue/eslint-config-typescript'
// configureVueProject({ scriptLangs: ['ts', 'tsx'] })
// More info at https://github.com/vuejs/eslint-config-typescript/#advanced-setup

export default defineConfigWithVueTs(
  {
    name: 'app/files-to-lint',
    files: ['**/*.{ts,mts,tsx,vue}'],
  },

  // public 下是运行时静态资源，不受本项目规则约束
  globalIgnores([
    '**/dist/**',
    '**/dist-ssr/**',
    '**/coverage/**',
    'public/**',
  ]),

  pluginVue.configs['flat/essential'],
  vueTsConfigs.recommended,
  ...pluginOxlint.configs['flat/recommended'],
  skipFormatting,

  {
    // 异常页按 HTTP 状态码命名是路由约定，改成多词反而对不上 /403 /404
    name: 'app/exception-pages',
    files: ['src/views/exception/*.vue'],
    rules: {
      'vue/multi-word-component-names': 'off',
    },
  },

  {
    // typescript-eslint 8.57 起 no-unused-vars 的 caughtErrors 默认值从 none 变成 all，
    // 仓库里 53 处既有 `catch (error) {}`（.vue 里错误已被统一拦截器弹过）会因此全部报错。
    // 这里对 .vue 显式保持旧行为；.ts 的未使用检查本来由 oxlint 负责，不在这里重新打开。
    // 批量改成 `catch {}` 留作独立清理，不在本次曲库改动里混。
    name: 'app/unused-catch-binding',
    files: ['**/*.vue'],
    rules: {
      '@typescript-eslint/no-unused-vars': ['error', { caughtErrors: 'none' }],
    },
  },
)
