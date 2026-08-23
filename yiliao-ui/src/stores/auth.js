import { defineStore } from 'pinia'
import { login as loginApi, logout as logoutApi } from '../api/auth'

// 登录态（specs/global/10 §5）：access token 本地存储，401 由 request 拦截器统一踢回登录页
export const useAuthStore = defineStore('auth', {
  state: () => ({
    accessToken: localStorage.getItem('accessToken') || '',
    refreshToken: localStorage.getItem('refreshToken') || '',
    user: JSON.parse(localStorage.getItem('userInfo') || 'null')
  }),
  getters: {
    isLoggedIn: (state) => !!state.accessToken,
    isPatient: (state) => (state.user?.userType ?? 0) === 4,
    roles: (state) => state.user?.roles || []
  },
  actions: {
    async login(username, password) {
      const data = await loginApi({ username, password })
      this.accessToken = data.accessToken
      this.refreshToken = data.refreshToken
      this.user = { userId: data.userId, realName: data.realName, userType: data.userType, roles: data.roles }
      localStorage.setItem('accessToken', data.accessToken)
      localStorage.setItem('refreshToken', data.refreshToken)
      localStorage.setItem('userInfo', JSON.stringify(this.user))
    },
    async logout() {
      try {
        await logoutApi({ refreshToken: this.refreshToken })
      } finally {
        this.clear()
      }
    },
    clear() {
      this.accessToken = ''
      this.refreshToken = ''
      this.user = null
      localStorage.removeItem('accessToken')
      localStorage.removeItem('refreshToken')
      localStorage.removeItem('userInfo')
    }
  }
})
