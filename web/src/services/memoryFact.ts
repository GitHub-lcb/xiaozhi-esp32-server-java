import { http } from './request'
import api from './api'
import type { MemoryFact, MemoryFactQuery } from '@/types/memoryFact'

export function queryMemoryFacts(params: Partial<MemoryFactQuery>) {
  return http.getPage<MemoryFact>(api.memoryFact, params)
}

export function addMemoryFact(data: Partial<MemoryFact>) {
  return http.post(api.memoryFact, data)
}

export function extractMemoryFacts(params: { deviceId: string; roleId: number; memberName?: string }) {
  return http.post<MemoryFact[]>(api.memoryFact + '/extract', null, { params })
}

export function deleteMemoryFact(factId: number) {
  return http.delete(`${api.memoryFact}/${factId}`)
}
