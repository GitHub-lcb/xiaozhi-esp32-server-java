import { http } from './request'
import api from './api'
import type { KnowledgeBase, KnowledgeDoc, KnowledgeHit, KnowledgeQuery } from '@/types/knowledge'

export function queryKnowledgeBases(params: Partial<KnowledgeQuery>) {
  return http.getPage<KnowledgeBase>(api.knowledge, params)
}

export function addKnowledgeBase(data: Partial<KnowledgeBase>) {
  return http.post(api.knowledge, data)
}

export function updateKnowledgeBase(data: Partial<KnowledgeBase>) {
  return http.put(`${api.knowledge}/${data.knowledgeId}`, data)
}

export function deleteKnowledgeBase(knowledgeId: number) {
  return http.delete(`${api.knowledge}/${knowledgeId}`)
}

export function queryKnowledgeDocs(knowledgeId: number) {
  return http.get<KnowledgeDoc[]>(`${api.knowledge}/${knowledgeId}/doc`)
}

export function uploadKnowledgeDoc(knowledgeId: number, file: File) {
  const form = new FormData()
  form.append('file', file)
  return http.post(`${api.knowledge}/${knowledgeId}/doc`, form, {
    headers: { 'Content-Type': 'multipart/form-data' },
  })
}

export function deleteKnowledgeDoc(docId: number) {
  return http.delete(`${api.knowledge}/doc/${docId}`)
}

export function searchKnowledge(data: { knowledgeId?: number; query: string; topK?: number }) {
  return http.post<KnowledgeHit[]>(api.knowledge + '/search', data)
}
