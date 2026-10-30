import { http } from './request'
import api from './api'
import type {
  MusicAudioUrl,
  MusicFile,
  MusicPlayLog,
  MusicPlayQuery,
  MusicQuery,
  MusicStats,
  MusicSyncResult,
  MusicUpdate,
} from '@/types/music'

/** 分页查询曲目，keyword 同时模糊匹配歌名/歌手/别名/文件名 */
export function queryMusic(params: Partial<MusicQuery>) {
  return http.getPage<MusicFile>(api.music, params)
}

/** 曲库概览统计 */
export function queryMusicStats() {
  return http.get<MusicStats>(`${api.music}/stats`)
}

/** 分页查询点播记录 */
export function queryMusicPlays(params: Partial<MusicPlayQuery>) {
  return http.getPage<MusicPlayLog>(`${api.music}/plays`, params)
}

/** 编辑曲目：title 必填，state 只允许 "1"/"0" */
export function updateMusic(musicId: number, data: MusicUpdate) {
  return http.put<MusicFile>(`${api.music}/${musicId}`, data)
}

/** 删除曲目：后端删除磁盘文件并把记录置为下架 */
export function deleteMusic(musicId: number) {
  return http.delete(`${api.music}/${musicId}`)
}

/** 同步音乐目录：新增文件登记，缺失文件下架 */
export function syncMusic() {
  return http.post<MusicSyncResult>(`${api.music}/sync`)
}

/** 取试听签名地址，前端再交给 getResourceUrl 拼成可访问 URL */
export function getMusicAudioUrl(musicId: number) {
  return http.get<MusicAudioUrl>(`${api.music}/${musicId}/audio-url`)
}

/** 上传曲目（multipart 字段 file，仅 .mp3 / playlist.txt 由后端校验） */
export function uploadMusic(file: File) {
  const form = new FormData()
  form.append('file', file)
  return http.postMultipart<string>(api.music, form)
}
