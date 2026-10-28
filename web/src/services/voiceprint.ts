import { http } from './request'
import api from './api'
import type { Voiceprint, VoiceprintMatch } from '@/types/voiceprint'

export function queryVoiceprints() {
  return http.get<Voiceprint[]>(api.voiceprint)
}

export function registerVoiceprint(memberName: string, file: File) {
  const form = new FormData()
  form.append('memberName', memberName)
  form.append('file', file)
  return http.post(api.voiceprint, form, { headers: { 'Content-Type': 'multipart/form-data' } })
}

export function deleteVoiceprint(voiceprintId: number) {
  return http.delete(`${api.voiceprint}/${voiceprintId}`)
}

export function matchVoiceprint(file: File) {
  const form = new FormData()
  form.append('file', file)
  return http.post<VoiceprintMatch>(api.voiceprint + '/match', form, {
    headers: { 'Content-Type': 'multipart/form-data' },
  })
}
