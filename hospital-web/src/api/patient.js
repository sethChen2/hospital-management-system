import request from '@/utils/request'

/* ---------------- 患者端 ---------------- */
export function findDepartments() {
  return request({ url: '/patient/findDepartments', method: 'get' })
}
export function findDoctorsByDepartment(params) {
  return request({ url: '/patient/findDoctorsByDepartment', method: 'get', params })
}
export function findSchedulesByDoctor(params) {
  return request({ url: '/patient/findSchedulesByDoctor', method: 'get', params })
}
export function addOrder(data) {
  return request({ url: '/patient/addOrder', method: 'post', data })
}
export function findMyOrders(params) {
  return request({ url: '/patient/findMyOrders', method: 'get', params })
}
export function findOrderDetail(registerId) {
  return request({ url: '/patient/findOrderDetail', method: 'get', params: { registerId } })
}
export function cancelOrder(registerId) {
  return request({ url: '/patient/cancelOrder', method: 'post', params: { registerId } })
}
export function payOrder(registerId) {
  return request({ url: '/patient/payOrder', method: 'post', params: { registerId } })
}
export function findPrescriptions(registerId) {
  return request({ url: '/patient/findPrescriptions', method: 'get', params: { registerId } })
}
export function findChecks(registerId) {
  return request({ url: '/patient/findChecks', method: 'get', params: { registerId } })
}
export function findBed() {
  return request({ url: '/patient/findBed', method: 'get' })
}

/**
 * 报告单下载地址：浏览器直接打开链接无法带自定义请求头，
 * 故后端把 /patient/pdf 放行，并通过 token 查询参数校验归属。
 */
export function reportPdfUrl(registerId, token) {
  const base = process.env.VUE_APP_BASE_API
  let url = base + '/patient/pdf?registerId=' + registerId
  if (token) {
    url += '&token=' + encodeURIComponent(token)
  }
  return url
}
