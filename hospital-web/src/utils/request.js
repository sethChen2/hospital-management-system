import axios from 'axios'
import { Message, MessageBox } from 'element-ui'
import store from '@/store'
import router from '@/router'

// 统一 axios 实例：所有请求带 /api 前缀（由 devServer 代理到后端）
const service = axios.create({
  baseURL: process.env.VUE_APP_BASE_API,
  timeout: 20000
})

/* ---------------- 请求拦截：携带 token ---------------- */
service.interceptors.request.use(
  config => {
    const token = store.getters.token
    if (token) {
      // 后端 JwtInterceptor 读取的请求头名固定为 token
      config.headers['token'] = token
    }
    return config
  },
  error => Promise.reject(error)
)

/* ---------------- 响应拦截：统一处理 R{status,msg,data} ---------------- */
service.interceptors.response.use(
  response => {
    // 文件下载（Excel / PDF）直接返回完整响应，交由调用方处理
    if (response.config.responseType === 'blob') {
      return response
    }

    const res = response.data
    // 非标准包装（理论上不会出现）直接透传
    if (res == null || res.status === undefined) {
      return res
    }

    if (res.status === 200) {
      return res
    }

    // 401：登录失效 —— 清空本地状态并回登录页
    if (res.status === 401) {
      store.dispatch('clearLogin')
      MessageBox.alert(res.msg || '登录已过期，请重新登录', '提示', {
        confirmButtonText: '去登录',
        callback: () => router.push('/login')
      })
      return Promise.reject(new Error(res.msg || '未登录'))
    }

    // 其余业务异常（含 403 越权）：只提示，不打断页面
    Message.error(res.msg || '请求失败')
    return Promise.reject(new Error(res.msg || '请求失败'))
  },
  error => {
    const status = error.response && error.response.status
    if (status === 401) {
      store.dispatch('clearLogin')
      router.push('/login')
      return Promise.reject(error)
    }
    Message.error(
      (error.response && error.response.data && error.response.data.msg) ||
        error.message ||
        '网络异常，请稍后重试'
    )
    return Promise.reject(error)
  }
)

export default service
