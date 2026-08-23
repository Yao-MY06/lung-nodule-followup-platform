import request from './request'

// auth（specs/modules/auth.md §3）
export const login = (data) => request.post('/auth/login', data)
export const logout = (data) => request.post('/auth/logout', data)
export const me = () => request.post('/auth/me')
