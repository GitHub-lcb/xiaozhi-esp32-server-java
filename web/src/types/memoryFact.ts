import type { PageQueryParams } from './api'

/** 长期记忆事实 */
export interface MemoryFact {
  factId?: number
  memberName?: string
  content: string
  category?: string
  source?: string
  state?: string
  createTime?: string
}

export interface MemoryFactQuery extends PageQueryParams {
  content?: string
  memberName?: string
}
