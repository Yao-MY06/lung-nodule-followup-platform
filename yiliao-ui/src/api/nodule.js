import request from './request'

// nodule / exam（specs/modules/nodule.md §3）
export const createReport = (data) => request.post('/nodule/exam/reports', data)
export const pageReports = (params) => request.get('/nodule/exam/reports', { params })
export const extractReport = (id) => request.post(`/nodule/exam/reports/${id}/extract`)
export const confirmReport = (id, newNodule = false, isNewFlag) =>
  request.post(`/nodule/exam/reports/${id}/confirm`, null, { params: { newNodule, isNewFlag } })
export const listNodules = (patientId) => request.get(`/nodule/patients/${patientId}/nodules`)
export const getTrend = (noduleId) => request.get(`/nodule/nodules/${noduleId}/trend`)
export const getCompare = (noduleId) => request.get(`/nodule/nodules/${noduleId}/compare`)
export const createSnapshot = (data) => request.post('/nodule/snapshots', data)
