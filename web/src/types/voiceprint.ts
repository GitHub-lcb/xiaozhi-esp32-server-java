export interface Voiceprint {
  voiceprintId?: number
  memberName: string
  sampleSeconds?: number
  state?: string
  createTime?: string
}

export interface VoiceprintMatch {
  voiceprintId?: number
  memberName?: string
  score?: number
  matched: boolean
}
