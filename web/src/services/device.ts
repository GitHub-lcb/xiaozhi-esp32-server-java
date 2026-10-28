import { http } from './request'
import api from './api'
import type {
  Device,
  DeviceCommand,
  DeviceCommandRequest,
  DeviceQueryParams,
  DeviceScreenshot,
  DeviceSelfCheck,
} from '@/types/device'

/**
 * 查询设备列表
 */
export function queryDevices(params: Partial<DeviceQueryParams>) {
  return http.getPage<Device>(api.device, params)
}

/**
 * 添加设备。
 * 后端回写新建的设备，调用方要用返回的 deviceId 再去绑定智能体，所以这里给出数据类型
 */
export function addDevice(code: string) {
  return http.post<Partial<Device>>(api.device, { code })
}

/**
 * 更新设备信息
 */
export function updateDevice(data: Partial<Device>) {
  return http.put(`${api.device}/${data.deviceId}`, data)
}

/**
 * 删除设备
 */
export function deleteDevice(deviceId: string) {
  return http.delete(`${api.device}/${deviceId}`)
}

/**
 * 设备接入一键自检
 */
export function selfCheckDevice(deviceId: string) {
  return http.get<DeviceSelfCheck>(`${api.device}/${deviceId}/self-check`)
}

/**
 * 下发设备控制指令（重启 / 音量 / 自定义工具）
 */
export function sendDeviceCommand(deviceId: string, data: DeviceCommandRequest) {
  return http.post<DeviceCommand>(`${api.device}/${deviceId}/command`, data)
}

/**
 * 触发一次设备截图
 */
export function captureDeviceScreenshot(deviceId: string) {
  return http.post<DeviceScreenshot>(`${api.device}/${deviceId}/screenshot`, {})
}

/**
 * 查询设备截图记录
 */
export function queryDeviceScreenshots(params: { deviceId?: string; pageNo?: number; pageSize?: number }) {
  return http.getPage<DeviceScreenshot>(`${api.device}/screenshot-log`, params)
}

/**
 * 查询设备指令记录
 */
export function queryDeviceCommands(params: { deviceId?: string; pageNo?: number; pageSize?: number }) {
  return http.getPage<DeviceCommand>(`${api.device}/command-log`, params)
}

/**
 * 清除设备记忆
 */
export function clearDeviceMemory(deviceId: string) {
  return http.delete(api.message.root, { deviceId })
}
