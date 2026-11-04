import { http } from './request'
import api from './api'
import type { OperationLog, OperationLogQuery } from '@/types/operationLog'

/**
 * 分页查询操作日志
 * 筛选条件全部走查询串，分页与 pageSize 上限由后端兜底（pageSize 夹在 1..100）
 */
export function queryOperationLogs(params: Partial<OperationLogQuery>) {
  return http.getPage<OperationLog>(api.operationLog, params)
}
