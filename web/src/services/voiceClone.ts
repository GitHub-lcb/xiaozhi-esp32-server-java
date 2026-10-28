import { http } from './request'
import api from './api'
import type { VoiceClone } from '@/types/voiceClone'

export function queryVoiceClones() {
  return http.get<VoiceClone[]>(api.voiceClone)
}

export function createVoiceClone(voiceName: string, file: File) {
  const form = new FormData()
  form.append('voiceName', voiceName)
  form.append('file', file)
  return http.post(api.voiceClone, form, { headers: { 'Content-Type': 'multipart/form-data' } })
}

export function deleteVoiceClone(cloneId: number) {
  return http.delete(`${api.voiceClone}/${cloneId}`)
}
