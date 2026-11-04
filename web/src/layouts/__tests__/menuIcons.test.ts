import { describe, expect, it, vi } from 'vitest'

// 路由表静态引 MainLayout，本用例只关心 meta，不需要真实布局
vi.mock('@/layouts/MainLayout.vue', () => ({ default: { template: '<div />' } }))

import router from '@/router'
import { parentMenuIconMap } from '@/composables/useMenu'
import { resolveMenuIcon } from '../menuIcons'

/**
 * 侧边栏图标覆盖守卫。
 * <p>
 * 菜单来自路由 meta.icon（父分组图标来自 useMenu.parentMenuIconMap），
 * 图标还必须登记在 menuIcons 白名单里才会被 tree-shaking 保留；漏一处就是「菜单没图标」。
 * 这类问题不会让构建失败，只会在页面上悄悄少一个图标，所以用测试钉住。
 */
describe('侧边栏菜单图标', () => {
  const mainRoute = router.getRoutes().find((route) => route.path === '/')
  const menuChildren = (mainRoute?.children ?? []).filter((route) => {
    const meta = route.meta ?? {}
    return Boolean(meta.title) && !meta.hideInMenu && !meta.showInUserHeader
  })

  it('路由表里能解析出菜单项（防止规则假绿）', () => {
    expect(menuChildren.length).toBeGreaterThan(10)
  })

  it('每个菜单路由都有已登记的图标', () => {
    const missing = menuChildren
      .filter((route) => !route.meta?.icon || !resolveMenuIcon(route.meta.icon))
      .map((route) => `${String(route.name ?? route.path)}(icon=${String(route.meta?.icon ?? 'NONE')})`)

    expect(missing, '菜单缺图标，或图标名没登记到 menuIcons 白名单').toEqual([])
  })

  it('父菜单分组图标全部已登记', () => {
    const missing = Object.entries(parentMenuIconMap)
      .filter(([, icon]) => !resolveMenuIcon(icon))
      .map(([key, icon]) => `${key} -> ${icon}`)

    expect(missing, '父菜单分组图标没登记到 menuIcons 白名单').toEqual([])
  })
})
