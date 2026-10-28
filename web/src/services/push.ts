import { http } from './request'
import api from './api'
import type { PushMessage, PushMessageQuery } from '@/types/push'

export function queryPushMessages(params: Partial<PushMessageQuery>) {
  return http.getPage<PushMessage>(api.push, params)
}

export function addPushMessage(data: Partial<PushMessage>) {
  return http.post(api.push, data)
}

export function sendPushMessage(pushId: number) {
  return http.post(`${api.push}/${pushId}/send`, {})
}

export function deletePushMessage(pushId: number) {
  return http.delete(`${api.push}/${pushId}`)
}
