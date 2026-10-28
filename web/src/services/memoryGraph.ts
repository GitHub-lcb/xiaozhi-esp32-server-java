import { http } from './request'
import api from './api'

export function queryGraphRelations(limit = 100) {
  return http.get<string[]>(api.memoryGraph, { limit })
}

export function addGraphRelation(data: { fromName: string; relation: string; toName: string }) {
  return http.post(api.memoryGraph + '/relation', data)
}
