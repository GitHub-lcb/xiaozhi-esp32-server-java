import type { PageQueryParams } from './api'

/** 固件版本 */
export interface Firmware {
  firmwareId?: number
  version: string
  chipModel?: string
  fileUrl: string
  fileSize?: number
  md5?: string
  releaseNotes?: string
  state?: string
  createTime?: string
  updateTime?: string
}

export interface FirmwareQuery extends PageQueryParams {
  version?: string
  chipModel?: string
  state?: string
}

/** 升级任务：把某个固件按策略投放给设备 */
export interface FirmwareRelease {
  releaseId?: number
  firmwareId: number
  firmwareVersion?: string
  releaseName: string
  strategy: 'all' | 'percent' | 'whitelist'
  percent?: number
  whitelist?: string
  startTime?: string | null
  endTime?: string | null
  state?: string
  createTime?: string
  updateTime?: string
}

export interface FirmwareReleaseQuery extends PageQueryParams {
  releaseName?: string
  firmwareId?: number
  state?: string
}

/** 升级记录 */
export interface FirmwareUpgradeLog {
  logId: number
  releaseId?: number
  releaseName?: string
  firmwareId?: number
  deviceId: string
  chipModel?: string
  fromVersion?: string
  toVersion?: string
  ip?: string
  status: string
  createTime?: string
}

export interface FirmwareUpgradeLogQuery extends PageQueryParams {
  deviceId?: string
  releaseId?: number
  status?: string
}
