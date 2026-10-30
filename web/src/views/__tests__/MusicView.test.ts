import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { flushPromises, shallowMount } from '@vue/test-utils'
import { createPinia, setActivePinia } from 'pinia'
import { message } from 'ant-design-vue'

const musicApiMock = vi.hoisted(() => ({
  queryMusic: vi.fn(),
  queryMusicStats: vi.fn(),
  queryMusicPlays: vi.fn(),
  updateMusic: vi.fn(),
  deleteMusic: vi.fn(),
  syncMusic: vi.fn(),
  getMusicAudioUrl: vi.fn(),
  uploadMusic: vi.fn(),
}))

vi.mock('@/services/music', () => musicApiMock)
vi.mock('@/services/api', () => ({ default: { music: '/file/music' } }))
vi.mock('@/utils/resource', () => ({
  getResourceUrl: (path?: string) => (path ? `https://cdn.example.com/${path}` : undefined),
}))

import MusicView from '../MusicView.vue'
import { useUserStore } from '@/store/user'
import type { User } from '@/types/user'
import type { MusicFile, MusicPlayLog, MusicStats } from '@/types/music'

/** 写接口成功响应 data 为 null 时也按成功处理，形状与 ApiResponse 对齐 */
function ok<T>(data: T) {
  return { code: 200, message: 'success', data }
}

function page<T>(list: T[], total: number) {
  return { code: 200, message: 'success', data: { list, total, pageNo: 1, pageSize: 10 } }
}

const musicFixture: MusicFile = {
  musicId: 1,
  fileName: '周杰伦 - 稻香.mp3',
  title: '稻香',
  artist: '周杰伦',
  aliases: '稻香,周杰伦',
  durationSec: 223,
  sizeBytes: 3670016,
  playCount: 12,
  state: '1',
  createTime: '2026-10-30 12:00:00',
  updateTime: '2026-10-30 12:00:00',
}

const statsFixture: MusicStats = {
  total: 3,
  enabled: 2,
  disabled: 1,
  totalPlayCount: 33,
  totalDurationSec: 669,
  totalSizeBytes: 104857600,
  todayPlayCount: 5,
}

const playFixture: MusicPlayLog = {
  logId: 100,
  deviceId: 'aa:bb:cc',
  deviceName: '客厅音箱',
  musicId: 1,
  songName: '稻香.mp3',
  musicTitle: '稻香',
  keyword: '播放稻香',
  createTime: '2026-10-30 12:30:00',
}

// <script setup> 的内部状态不对外暴露，测试里按实际形状声明后取用
interface MusicViewState {
  rows: MusicFile[]
  total: number
  stats: MusicStats
  plays: MusicPlayLog[]
  playsTotal: number
  loading: boolean
  loadError: boolean
  playsError: boolean
  emptyText: string
  canPreview: boolean
  canUpdate: boolean
  canDelete: boolean
  canSync: boolean
  canUpload: boolean
  query: {
    pageNo: number
    pageSize: number
    keyword: string
    state: string
    sortBy: string
    sortOrder: 'asc' | 'desc'
  }
  playQuery: { pageNo: number; pageSize: number }
  editVisible: boolean
  editingId?: number
  editForm: { title: string; artist: string; aliases: string; enabled: boolean }
  previewVisible: boolean
  previewUrl: string
  previewName: string
  refresh: () => Promise<void>
  handleSearch: () => Promise<void>
  onTableChange: (p: { current?: number; pageSize?: number }, filters: unknown, sorter: unknown) => Promise<void>
  openEdit: (record: MusicFile) => void
  saveEdit: () => Promise<void>
  toggleState: (record: MusicFile) => Promise<void>
  handleDelete: (record: MusicFile) => Promise<void>
  handleSync: () => Promise<void>
  openPreview: (record: MusicFile) => Promise<void>
  beforeUpload: (file: File) => unknown
  uploadOne: (file: File) => Promise<boolean>
  customUpload: (options: {
    file: File
    onSuccess?: (body?: unknown) => void
    onError?: (error: Error) => void
  }) => void
}

let pinia: ReturnType<typeof createPinia>

async function mountView() {
  const wrapper = shallowMount(MusicView, { global: { plugins: [pinia] } })
  await flushPromises()
  return wrapper.vm as unknown as MusicViewState
}

function setAdmin() {
  useUserStore().setUserInfo({ userId: 1, username: 'admin', name: 'Admin', isAdmin: '1' } as User)
}

function setNormalUser() {
  useUserStore().setUserInfo({ userId: 2, username: 'guest', name: 'Guest', isAdmin: '0' } as User)
}

describe('MusicView 曲库管理页', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    localStorage.clear()
    pinia = createPinia()
    setActivePinia(pinia)
    setAdmin()

    musicApiMock.queryMusic.mockResolvedValue(page([musicFixture], 1))
    musicApiMock.queryMusicStats.mockResolvedValue(ok(statsFixture))
    musicApiMock.queryMusicPlays.mockResolvedValue(page([playFixture], 1))
    musicApiMock.updateMusic.mockResolvedValue(ok(musicFixture))
    musicApiMock.deleteMusic.mockResolvedValue(ok(null))
    musicApiMock.syncMusic.mockResolvedValue(ok({ added: 2, removed: 1, total: 3 }))
    musicApiMock.getMusicAudioUrl.mockResolvedValue(
      ok({ url: 'uploads/music/周杰伦 - 稻香.mp3?exp=1&sig=abc' }),
    )
    musicApiMock.uploadMusic.mockResolvedValue(ok('new-song.mp3'))
  })

  // store 里的 useStorage 会监听 storage 事件，上一个用例的实例不销毁就会把旧身份同步回来
  afterEach(() => {
    useUserStore().$dispose()
    vi.restoreAllMocks()
  })

  it('加载成功：列表、统计、点播记录一起渲染', async () => {
    const view = await mountView()

    expect(musicApiMock.queryMusic).toHaveBeenCalledWith(
      expect.objectContaining({ pageNo: 1, pageSize: 10, sortBy: 'title', sortOrder: 'asc' }),
    )
    expect(view.rows).toHaveLength(1)
    expect(view.total).toBe(1)
    expect(view.stats.total).toBe(3)
    expect(view.stats.totalPlayCount).toBe(33)
    expect(view.plays).toHaveLength(1)
    expect(view.playsTotal).toBe(1)
    expect(view.loadError).toBe(false)
    expect(message.error).not.toHaveBeenCalled()
  })

  it('加载失败：进入错误态，重试后恢复列表', async () => {
    musicApiMock.queryMusic.mockRejectedValueOnce(new Error('network down'))
    const view = await mountView()

    expect(view.loadError).toBe(true)
    expect(view.rows).toEqual([])
    expect(view.total).toBe(0)

    musicApiMock.queryMusic.mockResolvedValue(page([musicFixture], 1))
    await view.refresh()
    await flushPromises()

    expect(view.loadError).toBe(false)
    expect(view.rows).toHaveLength(1)
    expect(view.total).toBe(1)
  })

  it('搜索与筛选：回到第一页并带上关键词，空态区分「曲库为空」和「搜索无结果」', async () => {
    musicApiMock.queryMusic.mockResolvedValue(page([], 0))
    const view = await mountView()

    expect(view.emptyText).toBe('music.empty')

    view.query.keyword = ' 稻香 '
    await view.handleSearch()
    expect(view.query.pageNo).toBe(1)
    expect(musicApiMock.queryMusic).toHaveBeenLastCalledWith(
      expect.objectContaining({ pageNo: 1, keyword: '稻香' }),
    )
    expect(view.emptyText).toBe('music.emptySearch')

    view.query.keyword = ''
    view.query.state = '1'
    await view.handleSearch()
    expect(musicApiMock.queryMusic).toHaveBeenLastCalledWith(
      expect.objectContaining({ state: '1', keyword: undefined }),
    )
    expect(view.emptyText).toBe('music.emptySearch')
  })

  it('点播记录加载失败：错误态可重试恢复', async () => {
    musicApiMock.queryMusicPlays.mockRejectedValueOnce(new Error('network down'))
    const view = await mountView()

    expect(view.playsError).toBe(true)
    expect(view.plays).toEqual([])

    musicApiMock.queryMusicPlays.mockResolvedValue(page([playFixture], 1))
    await (view as unknown as { loadPlays: () => Promise<void> }).loadPlays()
    await flushPromises()

    expect(view.playsError).toBe(false)
    expect(view.plays).toHaveLength(1)
  })

  it('分页与排序：透传 pageNo/pageSize/sortBy/sortOrder，取消排序回默认', async () => {
    const view = await mountView()

    await view.onTableChange({ current: 2, pageSize: 20 }, {}, { columnKey: 'playCount', order: 'descend' })
    expect(view.query.pageNo).toBe(2)
    expect(view.query.pageSize).toBe(20)
    expect(view.query.sortBy).toBe('playCount')
    expect(view.query.sortOrder).toBe('desc')
    expect(musicApiMock.queryMusic).toHaveBeenLastCalledWith(
      expect.objectContaining({ pageNo: 2, pageSize: 20, sortBy: 'playCount', sortOrder: 'desc' }),
    )

    await view.onTableChange({ current: 1, pageSize: 10 }, {}, { columnKey: 'title', order: null })
    expect(view.query.sortBy).toBe('title')
    expect(view.query.sortOrder).toBe('asc')
  })

  it('编辑保存：回填表单、提交后关闭弹窗并刷新列表与统计', async () => {
    const view = await mountView()
    view.openEdit(musicFixture)

    expect(view.editVisible).toBe(true)
    expect(view.editingId).toBe(1)
    expect(view.editForm.title).toBe('稻香')
    expect(view.editForm.artist).toBe('周杰伦')
    expect(view.editForm.enabled).toBe(true)

    const listCalls = musicApiMock.queryMusic.mock.calls.length
    view.editForm.title = ' 稻香 (Live) '
    view.editForm.aliases = '稻香,现场版'
    await view.saveEdit()
    await flushPromises()

    expect(musicApiMock.updateMusic).toHaveBeenCalledWith(1, {
      title: '稻香 (Live)',
      artist: '周杰伦',
      aliases: '稻香,现场版',
      state: '1',
    })
    expect(message.success).toHaveBeenCalledWith('music.saveSuccess')
    expect(view.editVisible).toBe(false)
    expect(musicApiMock.queryMusic.mock.calls.length).toBe(listCalls + 1)
    expect(musicApiMock.queryMusicStats.mock.calls.length).toBe(listCalls + 1)
  })

  it('编辑保存：歌名必填，失败时弹窗保持打开', async () => {
    const view = await mountView()
    view.openEdit(musicFixture)

    view.editForm.title = '   '
    await view.saveEdit()
    expect(message.warning).toHaveBeenCalledWith('music.titleRequired')
    expect(musicApiMock.updateMusic).not.toHaveBeenCalled()

    view.editForm.title = '稻香'
    musicApiMock.updateMusic.mockRejectedValueOnce(new Error('network down'))
    await view.saveEdit()
    await flushPromises()

    expect(message.error).toHaveBeenCalledWith('music.saveFailed')
    expect(view.editVisible).toBe(true)
  })

  it('上下架：已上架的下架、已下架的上架，提示对应文案', async () => {
    const view = await mountView()

    await view.toggleState(musicFixture)
    expect(musicApiMock.updateMusic).toHaveBeenCalledWith(
      1,
      expect.objectContaining({ title: '稻香', state: '0' }),
    )
    expect(message.success).toHaveBeenCalledWith('music.disabledSuccess')

    musicApiMock.updateMusic.mockClear()
    await view.toggleState({ ...musicFixture, state: '0' })
    expect(musicApiMock.updateMusic).toHaveBeenCalledWith(
      1,
      expect.objectContaining({ state: '1' }),
    )
    expect(message.success).toHaveBeenCalledWith('music.enabledSuccess')
  })

  it('删除：确认后按 musicId 删除并刷新，失败时提示错误', async () => {
    const view = await mountView()
    const listCalls = musicApiMock.queryMusic.mock.calls.length

    await view.handleDelete(musicFixture)
    await flushPromises()

    expect(musicApiMock.deleteMusic).toHaveBeenCalledWith(1)
    expect(message.success).toHaveBeenCalledWith('music.deleteSuccess')
    expect(musicApiMock.queryMusic.mock.calls.length).toBe(listCalls + 1)

    musicApiMock.deleteMusic.mockRejectedValueOnce(new Error('network down'))
    await view.handleDelete(musicFixture)
    await flushPromises()
    expect(message.error).toHaveBeenCalledWith('music.deleteFailed')
  })

  it('上传：只放行 .mp3，逐首上传成功后刷新列表', async () => {
    const view = await mountView()
    const listCalls = musicApiMock.queryMusic.mock.calls.length

    const mp3 = new File(['audio'], 'new-song.mp3', { type: 'audio/mpeg' })
    const txt = new File(['text'], 'readme.txt', { type: 'text/plain' })
    expect(view.beforeUpload(mp3)).toBe(true)
    expect(view.beforeUpload(txt)).toBe(false)
    expect(message.warning).toHaveBeenCalledWith('music.uploadInvalid')

    await view.uploadOne(mp3)
    await flushPromises()

    expect(musicApiMock.uploadMusic).toHaveBeenCalledWith(mp3)
    expect(message.success).toHaveBeenCalledWith(expect.stringContaining('music.uploadSuccess'))
    expect(musicApiMock.queryMusic.mock.calls.length).toBe(listCalls + 1)

    musicApiMock.uploadMusic.mockRejectedValueOnce(new Error('network down'))
    await expect(view.uploadOne(mp3)).resolves.toBe(false)
    expect(message.error).toHaveBeenCalledWith(expect.stringContaining('music.uploadFailed'))
  })

  it('上传：customRequest 逐个文件回调 onSuccess/onError', async () => {
    const view = await mountView()
    const mp3 = new File(['audio'], 'new-song.mp3', { type: 'audio/mpeg' })
    const onSuccess = vi.fn()
    const onError = vi.fn()

    view.customUpload({ file: mp3, onSuccess, onError })
    await flushPromises()
    expect(musicApiMock.uploadMusic).toHaveBeenCalledWith(mp3)
    expect(onSuccess).toHaveBeenCalled()
    expect(onError).not.toHaveBeenCalled()

    musicApiMock.uploadMusic.mockRejectedValueOnce(new Error('network down'))
    const failedSuccess = vi.fn()
    const failedError = vi.fn()
    view.customUpload({ file: mp3, onSuccess: failedSuccess, onError: failedError })
    await flushPromises()
    expect(failedSuccess).not.toHaveBeenCalled()
    expect(failedError).toHaveBeenCalled()
  })

  it('同步目录：展示新增/下架数量并刷新', async () => {
    const view = await mountView()
    const listCalls = musicApiMock.queryMusic.mock.calls.length

    await view.handleSync()
    await flushPromises()

    expect(musicApiMock.syncMusic).toHaveBeenCalled()
    expect(message.success).toHaveBeenCalledWith('music.syncSuccess:{"added":2,"removed":1}')
    expect(musicApiMock.queryMusic.mock.calls.length).toBe(listCalls + 1)

    musicApiMock.syncMusic.mockRejectedValueOnce(new Error('network down'))
    await view.handleSync()
    expect(message.error).toHaveBeenCalledWith('music.syncFailed')
  })

  it('试听：用签名地址经 getResourceUrl 拼出 src，关闭弹窗即清空', async () => {
    const view = await mountView()

    await view.openPreview(musicFixture)
    expect(musicApiMock.getMusicAudioUrl).toHaveBeenCalledWith(1)
    expect(view.previewVisible).toBe(true)
    expect(view.previewName).toBe('稻香')
    expect(view.previewUrl).toBe(
      'https://cdn.example.com/uploads/music/周杰伦 - 稻香.mp3?exp=1&sig=abc',
    )

    view.previewVisible = false
    await flushPromises()
    expect(view.previewUrl).toBe('')

    musicApiMock.getMusicAudioUrl.mockRejectedValueOnce(new Error('network down'))
    await view.openPreview(musicFixture)
    expect(message.error).toHaveBeenCalledWith('music.previewFailed')
    expect(view.previewVisible).toBe(false)
  })

  it('写操作按钮按 userStore.hasPermission 控制，admin 自动放行', async () => {
    const adminView = await mountView()
    expect(adminView.canPreview).toBe(true)
    expect(adminView.canUpdate).toBe(true)
    expect(adminView.canDelete).toBe(true)
    expect(adminView.canSync).toBe(true)
    expect(adminView.canUpload).toBe(true)

    setNormalUser()
    await flushPromises()
    expect(adminView.canPreview).toBe(false)
    expect(adminView.canUpdate).toBe(false)
    expect(adminView.canDelete).toBe(false)
    expect(adminView.canSync).toBe(false)
    expect(adminView.canUpload).toBe(false)
  })
})
