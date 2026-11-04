import type { PageQueryParams } from './api'

/**
 * 操作日志条目（对应 sys_operation_log 表，由 @AuditLog 切面异步落库）
 * 时间字段后端已格式化为 yyyy-MM-dd HH:mm:ss 字符串，前端不再二次转换
 */
export interface OperationLog {
  id: number
  /** 操作人 ID */
  userId?: number
  /** 操作人用户名，后端按 userId 关联查询后回填 */
  username?: string
  /** 业务模块，如 device / role / config */
  module?: string
  /** 操作描述，如「重启设备」 */
  operation?: string
  /** HTTP 方法 */
  method?: string
  /** 请求地址 */
  url?: string
  /** 后端处理方法的全名 */
  handler?: string
  /** 请求参数 JSON（敏感字段后端已脱敏，超长会截断） */
  params?: string
  /** 是否成功 */
  success?: boolean
  /** 失败时的错误信息 */
  errorMsg?: string
  /** 耗时（毫秒） */
  costMs?: number
  /** 操作时间 yyyy-MM-dd HH:mm:ss */
  createTime?: string
}

/**
 * 操作日志查询参数
 * module / operation 为模糊匹配，userId 为精确匹配，success 用布尔值筛选，时间范围由前端拼成字符串
 */
export interface OperationLogQuery extends PageQueryParams {
  module?: string
  operation?: string
  userId?: number
  success?: boolean
  /** 开始时间 yyyy-MM-dd HH:mm:ss */
  startTime?: string
  /** 结束时间 yyyy-MM-dd HH:mm:ss */
  endTime?: string
}
