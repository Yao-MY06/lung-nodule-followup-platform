import request from './request'

// patient（specs/modules/patient.md §3）
export const pageArchives = (params) => request.get('/patient/archives', { params })
// 患者端：按当前登录人自查档案（服务端强制本人数据）
export const getMyArchive = () => request.get('/patient/archives/my')
export const getArchive = (id) => request.get(`/patient/archives/${id}`)
export const createArchive = (data) => request.post('/patient/archives', data)
export const changeStage = (id, stageLabel) => request.put(`/patient/archives/${id}/stage`, null, { params: { stageLabel } })
export const changeDoctor = (id, doctorId) => request.put(`/patient/archives/${id}/doctor`, null, { params: { doctorId } })
