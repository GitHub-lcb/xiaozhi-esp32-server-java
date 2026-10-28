import { http } from './request'
import api from './api'
import type { Reminder, ReminderQuery } from '@/types/reminder'

export function queryReminders(params: Partial<ReminderQuery>) {
  return http.getPage<Reminder>(api.reminder, params)
}

export function addReminder(data: Partial<Reminder>) {
  return http.post(api.reminder, data)
}

export function cancelReminder(reminderId: number) {
  return http.post(`${api.reminder}/${reminderId}/cancel`, {})
}

export function deleteReminder(reminderId: number) {
  return http.delete(`${api.reminder}/${reminderId}`)
}
