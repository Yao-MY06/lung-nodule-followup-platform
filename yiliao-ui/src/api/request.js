import axios from 'axios'
import { ElMessage } from 'element-plus'
import router from '../router'

// 统一请求实例：走 vite 代理到网关 8080；Result{code,msg,data} 解包（specs/global/10 §2）
const request = axios.create({ baseURL: '/api', timeout: 30000 })

request.interceptors.request.use((config) => {
  const token = localStorage.getItem('accessToken')
  if (token) {
    config.headers.Authorization = `Bearer ${token}`
  }
  return config
})

request.interceptors.response.use(
  (response) => {
    const result = response.data
    if (result && typeof result.code === 'number') {
      if (result.code === 0) {
        return result.data
      }
      ElMessage.error(result.msg || `请求失败(${result.code})`)
      return Promise.reject(new Error(result.msg))
    }
    return result
  },
  (error) => {
    const status = error.response?.status
    if (status === 401) {
      localStorage.removeItem('accessToken')
      localStorage.removeItem('userInfo')
      router.push('/login')
      ElMessage.error('登录已过期，请重新登录')
    } else {
      ElMessage.error(error.response?.data?.msg || error.message || '网络异常')
    }
    return Promise.reject(error)
  }
)

export default request
