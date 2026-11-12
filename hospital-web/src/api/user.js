import request from '@/utils/request'

/* ---------------- 认证与个人信息（三角色通用） ---------------- */
export function login(data) {
  return request({ url: '/user/login', method: 'post', data })
}

export function addPatient(data) {
  return request({ url: '/user/addPatient', method: 'post', data })
}

export function findBackPassword(data) {
  return request({ url: '/user/findBackPassword', method: 'post', data })
}

export function getInfo() {
  return request({ url: '/user/getInfo', method: 'get' })
}

export function modifyInfo(data) {
  return request({ url: '/user/modifyInfo', method: 'post', data })
}

export function modifyPassword(data) {
  return request({ url: '/user/modifyPassword', method: 'post', data })
}

export function logout() {
  return request({ url: '/user/logout', method: 'post' })
}
