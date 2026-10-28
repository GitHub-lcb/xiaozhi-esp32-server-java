import { http } from './request'
import api from './api'
import type {
  Firmware,
  FirmwareQuery,
  FirmwareRelease,
  FirmwareReleaseQuery,
  FirmwareUpgradeLog,
  FirmwareUpgradeLogQuery,
} from '@/types/firmware'

export function queryFirmwares(params: Partial<FirmwareQuery>) {
  return http.getPage<Firmware>(api.firmware.root, params)
}

export function addFirmware(data: Partial<Firmware>) {
  return http.post(api.firmware.root, data)
}

export function updateFirmware(data: Partial<Firmware>) {
  return http.put(`${api.firmware.root}/${data.firmwareId}`, data)
}

export function deleteFirmware(firmwareId: number) {
  return http.delete(`${api.firmware.root}/${firmwareId}`)
}

export function queryReleases(params: Partial<FirmwareReleaseQuery>) {
  return http.getPage<FirmwareRelease>(api.firmware.release, params)
}

export function addRelease(data: Partial<FirmwareRelease>) {
  return http.post(api.firmware.release, data)
}

export function updateRelease(data: Partial<FirmwareRelease>) {
  return http.put(`${api.firmware.release}/${data.releaseId}`, data)
}

export function changeReleaseState(releaseId: number, state: string) {
  return http.post(`${api.firmware.release}/${releaseId}/state`, { state })
}

export function deleteRelease(releaseId: number) {
  return http.delete(`${api.firmware.release}/${releaseId}`)
}

export function queryUpgradeLogs(params: Partial<FirmwareUpgradeLogQuery>) {
  return http.getPage<FirmwareUpgradeLog>(api.firmware.upgradeLog, params)
}
