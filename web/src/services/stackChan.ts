import { http } from './request'
import api from './api'

/** App Store 条目，与后端 StackChanAppAdminResp 对齐（主键字段是 id） */
export interface StackChanApp {
  id?: number
  appName: string
  appIconUrl?: string
  description?: string
  firmwareUrl?: string
  state?: number
  createTime?: string
  updateTime?: string
}

export interface StackChanUploadResult {
  path?: string
  url: string
}

export interface StackChanDance {
  id?: number
  mac?: string
  danceName: string
  musicUrl?: string
  danceData?: unknown
}

export interface StackChanPostComment {
  id?: number
  postId?: number
  mac?: string
  name?: string
  content?: string
  createdAt?: string
}

export interface StackChanPost {
  id?: number
  mac?: string
  name?: string
  contentText?: string
  contentImage?: string
  createdAt?: string
  postCommentList?: StackChanPostComment[]
}

export interface StackChanPano {
  id?: number
  mac?: string
  panoUrl: string
  createdAt?: string
  updatedAt?: string
}

export interface StackChanFriend {
  id?: number
  macA: string
  macB: string
  createTime?: string
}

// ==================== App Store ====================

export function queryStackChanApps() {
  return http.get<StackChanApp[]>(api.stackChan.adminApps)
}

export function createStackChanApp(data: StackChanApp) {
  return http.post<StackChanApp>(api.stackChan.adminApps, data)
}

export function updateStackChanApp(data: StackChanApp) {
  return http.put<StackChanApp>(api.stackChan.adminApps, data)
}

export function deleteStackChanApp(id: number) {
  return http.deleteBody<boolean>(api.stackChan.adminApps, { id })
}

export function uploadStackChanFile(file: File, directory: 'icons' | 'firmware' | 'apps') {
  const form = new FormData()
  form.append('file', file)
  form.append('directory', directory)
  return http.postMultipart<StackChanUploadResult>(api.stackChan.upload, form, { timeout: 300000 })
}

// ==================== 跳舞 ====================

export function queryStackChanDances(mac?: string) {
  return http.get<StackChanDance[]>(api.stackChan.adminDances, mac ? { mac } : {})
}

export function createStackChanDance(data: StackChanDance) {
  return http.post<StackChanDance>(api.stackChan.adminDances, data)
}

export function updateStackChanDance(data: StackChanDance) {
  return http.put<StackChanDance>(api.stackChan.adminDances, data)
}

export function deleteStackChanDance(id: number) {
  return http.deleteBody<boolean>(api.stackChan.adminDances, { id })
}

// ==================== 社区 ====================

export function queryStackChanPosts(page = 1, pageSize = 20) {
  return http.get<StackChanPost[]>(api.stackChan.adminPosts, { page, pageSize })
}

export function deleteStackChanPost(id: number) {
  return http.deleteBody<boolean>(api.stackChan.adminPosts, { id })
}

// ==================== 全景 ====================

export function queryStackChanPanos() {
  return http.get<StackChanPano[]>(api.stackChan.adminPanos)
}

export function createStackChanPano(data: { mac: string; url: string }) {
  return http.post<StackChanPano>(api.stackChan.adminPanos, data)
}

export function deleteStackChanPano(id: number) {
  return http.deleteBody<boolean>(api.stackChan.adminPanos, { id })
}

// ==================== 好友 ====================

export function queryStackChanFriends() {
  return http.get<StackChanFriend[]>(api.stackChan.adminFriends)
}

export function deleteStackChanFriend(id: number) {
  return http.deleteBody<boolean>(api.stackChan.adminFriends, { id })
}
