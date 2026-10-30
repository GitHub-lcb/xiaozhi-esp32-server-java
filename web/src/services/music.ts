import { http } from './request'

export interface MusicFile {
  musicId?: number
  fileName: string
  title?: string
  artist?: string
  durationSec?: number
  sizeBytes?: number
  playCount?: number
  state?: string
  createTime?: string
}

export interface MusicPlayLog {
  logId: number
  deviceId?: string
  keyword?: string
  songName?: string
  createTime?: string
}

export function listMusic() {
  return http.get<MusicFile[]>('/file/music')
}

export function uploadMusic(file: File) {
  const form = new FormData()
  form.append('file', file)
  return http.post<string>('/file/music', form, {
    headers: { 'Content-Type': 'multipart/form-data' },
  })
}

export function listMusicPlays(limit = 20) {
  return http.get<MusicPlayLog[]>('/file/music/plays', { limit })
}

export function deleteMusic(name: string) {
  return http.delete<void>('/file/music', { params: { name } })
}
