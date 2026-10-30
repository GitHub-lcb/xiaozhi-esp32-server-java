import type { PageQueryParams } from './api'

/** 曲库列表/编辑接口的排序字段白名单，与后端 MusicController 保持一致 */
export type MusicSortField = 'title' | 'playCount' | 'createTime' | 'durationSec'

export type MusicSortOrder = 'asc' | 'desc'

/** 曲目（后端 MusicResp） */
export interface MusicFile {
  musicId: number
  fileName: string
  title?: string
  artist?: string
  /** 英文逗号分隔的别名 */
  aliases?: string
  durationSec?: number
  sizeBytes?: number
  playCount?: number
  /** "1" 已上架 / "0" 已下架 */
  state?: string
  createTime?: string
  updateTime?: string
}

/** 曲库统计（后端 MusicStatsResp） */
export interface MusicStats {
  total: number
  enabled: number
  disabled: number
  totalPlayCount: number
  totalDurationSec: number
  totalSizeBytes: number
  todayPlayCount: number
}

/** 点播记录（后端 MusicPlayLogResp） */
export interface MusicPlayLog {
  logId: number
  deviceId?: string
  /** 后端按 deviceId 补齐，查不到时给 null */
  deviceName?: string | null
  musicId?: number
  songName?: string
  /** 后端按 musicId 补齐，查不到时给 null */
  musicTitle?: string | null
  keyword?: string
  createTime?: string
}

/** 曲库列表查询参数 */
export interface MusicQuery extends PageQueryParams {
  keyword?: string
  state?: string
  sortBy?: MusicSortField
  sortOrder?: MusicSortOrder
}

/** 点播记录查询参数 */
export interface MusicPlayQuery extends PageQueryParams {
  keyword?: string
  deviceId?: string
}

/** 编辑曲目请求体（后端 MusicUpdateReq） */
export interface MusicUpdate {
  title: string
  artist?: string
  aliases?: string
  state?: string
}

/** 同步目录结果（后端 MusicSyncResp） */
export interface MusicSyncResult {
  added: number
  removed: number
  total: number
}

/** 试听地址（后端 audio-url 响应） */
export interface MusicAudioUrl {
  url: string
}
