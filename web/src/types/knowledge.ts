import type { PageQueryParams } from './api'

/** 知识库 */
export interface KnowledgeBase {
  knowledgeId?: number
  name: string
  description?: string
  roleId?: number
  embeddingConfigId?: number
  state?: string
  createTime?: string
}

export interface KnowledgeDoc {
  docId: number
  knowledgeId: number
  fileName: string
  fileType?: string
  fileSize?: number
  chunkCount?: number
  state: string
  errorMsg?: string
  createTime?: string
}

export interface KnowledgeHit {
  chunkId: number
  docId: number
  knowledgeId: number
  score?: number
  content: string
}

export interface KnowledgeQuery extends PageQueryParams {
  name?: string
}
