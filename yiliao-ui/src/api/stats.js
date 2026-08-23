import request from './request'

// statistics（specs/modules/statistics.md §3）
export const overview = () => request.get('/stats/overview')
export const noduleDistribution = () => request.get('/stats/nodule-distribution')
export const followupRate = (months = 6) => request.get('/stats/followup-rate', { params: { months } })
export const doctorWorkload = () => request.get('/stats/doctor-workload')
