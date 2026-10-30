import { http } from './request'

export interface MusicFile {
  name: string
  size: number
  updateTime: string
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

export function deleteMusic(name: string) {
  return http.delete<void>('/file/music', { params: { name } })
}
