import type { PageQueryParams } from './api'

/** 语音提醒/闹钟 */
export interface Reminder {
  reminderId?: number
  deviceId: string
  title: string
  content: string
  remindTime: string
  repeatType: 'once' | 'daily' | 'weekday'
  state?: string
  lastFireTime?: string
  createTime?: string
}

export interface ReminderQuery extends PageQueryParams {
  deviceId?: string
  state?: string
}
