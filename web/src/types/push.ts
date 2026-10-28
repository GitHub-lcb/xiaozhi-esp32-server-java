import type { PageQueryParams } from './api'

/** 主动推送消息 */
export interface PushMessage {
  pushId?: number
  title: string
  content: string
  targetType: 'device' | 'user'
  targetValue: string
  state?: string
  totalCount?: number
  sentCount?: number
  offlineCount?: number
  sentTime?: string
  createTime?: string
}

export interface PushMessageQuery extends PageQueryParams {
  title?: string
  state?: string
}
