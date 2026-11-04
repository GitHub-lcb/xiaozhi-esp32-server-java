import { beforeEach, describe, expect, it, vi } from 'vitest'
import { flushPromises, shallowMount } from '@vue/test-utils'
import { message } from 'ant-design-vue'

const operationLogApiMock = vi.hoisted(() => ({
  queryOperationLogs: vi.fn(),
}))

vi.mock('@/services/operationLog', () => operationLogApiMock)

import OperationLogView from '../OperationLogView.vue'
import type { OperationLog } from '@/types/operationLog'

/** 与 ApiResponse<PageData> 对齐的成功响应 */
function page<T>(list: T[], total: number) {
  return { code: 200, message: 'success', data: { list, total, pageNo: 1, pageSize: 10 } }
}

const logFixture: OperationLog = {
  id: 1,
  userId: 7,
  username: 'admin',
  module: 'device',
  operation: '重启设备',
  method: 'POST',
  url: '/api/device/1/reboot',
  handler: 'DeviceController.reboot',
  params: '{"deviceId":1}',
  success: true,
  costMs: 25,
  createTime: '2026-11-03 12:00:00',
}

// <script setup> 的内部状态不对外暴露，测试里按实际形状声明后取用
interface OperationLogViewState {
  rows: OperationLog[]
  total: number
  loading: boolean
  detailVisible: boolean
  detail: OperationLog | null
  timeRange: [string, string] | null
  query: {
    pageNo: number
    pageSize: number
    module: string
    operation: string
    userId?: number
    success?: boolean
  }
  loadData: () => Promise<void>
  handleSearch: () => void
  handleReset: () => void
  onTableChange: (p: { current?: number; pageSize?: number }) => void
  openDetail: (record: OperationLog) => void
}

async function mountView() {
  const wrapper = shallowMount(OperationLogView)
  await flushPromises()
  return wrapper.vm as unknown as OperationLogViewState
}

describe('OperationLogView 操作日志查询页', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    operationLogApiMock.queryOperationLogs.mockResolvedValue(page([logFixture], 1))
  })

  it('首屏加载：按默认分页调用查询接口并渲染列表', async () => {
    const view = await mountView()

    expect(operationLogApiMock.queryOperationLogs).toHaveBeenCalledTimes(1)
    expect(operationLogApiMock.queryOperationLogs).toHaveBeenCalledWith(
      expect.objectContaining({ pageNo: 1, pageSize: 10 }),
    )
    expect(view.rows).toHaveLength(1)
    expect(view.total).toBe(1)
    expect(view.loading).toBe(false)
    expect(message.error).not.toHaveBeenCalled()
  })

  it('筛选：模块/操作去首尾空格后传给后端，用户ID、结果、时间范围一并透传且回到第一页', async () => {
    const view = await mountView()

    view.query.module = ' device '
    view.query.operation = ' 重启 '
    view.query.userId = 7
    view.query.success = false
    view.timeRange = ['2026-11-01 00:00:00', '2026-11-03 23:59:59']
    view.query.pageNo = 3
    view.handleSearch()
    await flushPromises()

    expect(view.query.pageNo).toBe(1)
    expect(operationLogApiMock.queryOperationLogs).toHaveBeenLastCalledWith({
      pageNo: 1,
      pageSize: 10,
      module: 'device',
      operation: '重启',
      userId: 7,
      success: false,
      startTime: '2026-11-01 00:00:00',
      endTime: '2026-11-03 23:59:59',
    })
  })

  it('筛选：空条件不拼进查询串（保持 undefined 而不是空串）', async () => {
    const view = await mountView()

    view.query.module = '   '
    view.query.operation = ''
    view.query.userId = undefined
    view.query.success = undefined
    view.timeRange = null
    view.handleSearch()
    await flushPromises()

    expect(operationLogApiMock.queryOperationLogs).toHaveBeenLastCalledWith({
      pageNo: 1,
      pageSize: 10,
      module: undefined,
      operation: undefined,
      userId: undefined,
      success: undefined,
      startTime: undefined,
      endTime: undefined,
    })
  })

  it('分页：切换页码与每页条数后带上新参数重新查询', async () => {
    const view = await mountView()
    operationLogApiMock.queryOperationLogs.mockClear()

    view.onTableChange({ current: 2, pageSize: 20 })
    await flushPromises()

    expect(view.query.pageNo).toBe(2)
    expect(view.query.pageSize).toBe(20)
    expect(operationLogApiMock.queryOperationLogs).toHaveBeenCalledWith(
      expect.objectContaining({ pageNo: 2, pageSize: 20 }),
    )
  })

  it('详情：打开弹窗后 params / errorMsg 原样保留，供 <pre> 直接展示', async () => {
    const view = await mountView()
    const failedLog: OperationLog = {
      ...logFixture,
      id: 2,
      success: false,
      params: '{"deviceId":  1,  "volume": 80}',
      errorMsg: '设备离线',
      costMs: 3001,
    }

    view.openDetail(failedLog)

    expect(view.detailVisible).toBe(true)
    expect(view.detail?.id).toBe(2)
    expect(view.detail?.params).toBe('{"deviceId":  1,  "volume": 80}')
    expect(view.detail?.errorMsg).toBe('设备离线')
    expect(view.detail?.costMs).toBe(3001)
  })

  it('重置：清空全部筛选条件并回到第一页重新查询', async () => {
    const view = await mountView()

    view.query.module = 'device'
    view.query.operation = ' reboot '
    view.query.userId = 7
    view.query.success = true
    view.timeRange = ['2026-11-01 00:00:00', '2026-11-03 23:59:59']
    view.query.pageNo = 5
    view.handleReset()
    await flushPromises()

    expect(view.query.module).toBe('')
    expect(view.query.operation).toBe('')
    expect(view.query.userId).toBeUndefined()
    expect(view.query.success).toBeUndefined()
    expect(view.query.pageNo).toBe(1)
    expect(view.timeRange).toBeNull()
    expect(operationLogApiMock.queryOperationLogs).toHaveBeenLastCalledWith({
      pageNo: 1,
      pageSize: 10,
      module: undefined,
      operation: undefined,
      userId: undefined,
      success: undefined,
      startTime: undefined,
      endTime: undefined,
    })
  })

  it('加载失败：提示错误并保持列表为空，不抛出未捕获异常', async () => {
    operationLogApiMock.queryOperationLogs.mockRejectedValueOnce(new Error('network down'))

    const view = await mountView()

    expect(message.error).toHaveBeenCalledWith('operationLog.loadFailed')
    expect(view.rows).toEqual([])
    expect(view.total).toBe(0)
    expect(view.loading).toBe(false)
  })
})
