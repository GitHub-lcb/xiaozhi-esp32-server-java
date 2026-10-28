import { http } from './request'
import api from './api'

export interface DeviceGroup {
  groupId: number
  deviceIds: string[]
}

export function queryDeviceGroup(groupId: number) {
  return http.get<DeviceGroup[]>(api.deviceGroup, { groupId })
}

export function createDeviceGroup(groupName: string) {
  return http.post<number>(api.deviceGroup, null, { params: { groupName } })
}

export function addGroupMember(groupId: number, deviceId: string) {
  return http.post(`${api.deviceGroup}/${groupId}/member`, null, { params: { deviceId } })
}

export function removeGroupMember(groupId: number, deviceId: string) {
  return http.delete(`${api.deviceGroup}/${groupId}/member/${deviceId}`)
}
