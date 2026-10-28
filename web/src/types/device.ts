import type { PageQueryParams } from './api'

/**
 * 设备信息，字段对应后端 DeviceResp
 */
export interface Device {
  lastLogin?: string
  deviceId: string
  sessionId?: string
  deviceName?: string
  roleId?: number
  roleName?: string
  state?: string
  code?: string
  audioPath?: string
  wifiName?: string
  ip?: string
  chipModelName?: string
  type?: string
  version?: string
  channel?: string
  channelOnline?: boolean
  mcpList?: string
  location?: string
  createTime?: string
  updateTime?: string
  editable?: boolean // 表格编辑状态
}

/**
 * 自检单项结果
 */
export interface SelfCheckItem {
  key: string
  name: string
  status: 'pass' | 'warn' | 'fail'
  detail: string
  suggestion?: string | null
}

/**
 * 设备接入一键自检结果
 */
export interface DeviceSelfCheck {
  deviceId: string
  deviceName?: string
  summary: 'pass' | 'warn' | 'fail'
  passed: number
  warned: number
  failed: number
  checkTime: string
  items: SelfCheckItem[]
}

/** 设备截图记录 */
export interface DeviceScreenshot {
  shotId: number
  deviceId: string
  commandId?: number
  imageUrl?: string
  state: string
  message?: string
  createTime?: string
}

/** 远程控制动作 */
export interface DeviceCommandRequest {
  action: 'reboot' | 'volume' | 'custom'
  volume?: number
  toolName?: string
  arguments?: string
}

/** 远程控制指令记录 */
export interface DeviceCommand {
  commandId: number
  deviceId: string
  action: string
  toolName: string
  arguments?: string
  state: string
  result?: string
  createTime?: string
}

/**
 * 设备查询参数
 */
export interface DeviceQueryParams extends PageQueryParams {
  deviceId?: string
  deviceName?: string
  roleName?: string
  state?: string | number
}


