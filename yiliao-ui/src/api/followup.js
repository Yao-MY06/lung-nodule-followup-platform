import request from './request'

// followup（specs/modules/followup.md §3）
export const workbench = (params) => request.get('/followup/workbench', { params })
export const nextFollowup = (patientId) => request.get(`/followup/patients/${patientId}/next`)
export const timeline = (patientId) => request.get(`/followup/plans/${patientId}`)
export const adjustTask = (id, params) => request.put(`/followup/tasks/${id}/adjust`, null, { params })
export const submitRecord = (id, data) => request.put(`/followup/tasks/${id}/record`, data)
export const generatePlan = (data) => request.post('/followup/plans/generate', data)
export const regeneratePlan = (patientId, scene, input, reason) =>
  request.post(`/followup/plans/${patientId}/regenerate`, input, { params: { scene, reason } })
export const reportSymptom = (patientId, data) => request.post(`/followup/patients/${patientId}/symptoms`, data)
