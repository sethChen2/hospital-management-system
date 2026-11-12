/**
 * 医生端接口封装
 *
 * 所有请求都经 utils/request，自动在请求头带上 token（拦截器统一处理）。
 * 这里只负责声明 URL 和传参方式：
 * - GET 查询用 params（拼在 URL 上）
 * - POST 提交用 data（放在请求体里）
 */
import request from '@/utils/request'

/* ---------------- 字典 ---------------- */
export function findAllDrugList() {
  return request({ url: '/doctor/findAllDrugList', method: 'get' })
}
export function findAllCheckList() {
  return request({ url: '/doctor/findAllCheckList', method: 'get' })
}

/* ---------------- 医生工作台 ---------------- */
export function findMySchedules(params) {
  return request({ url: '/doctor/findMySchedules', method: 'get', params })
}
export function findMyOrders(params) {
  return request({ url: '/doctor/findMyOrders', method: 'get', params })
}
export function findPatient(patientUserId) {
  return request({ url: '/doctor/findPatient', method: 'get', params: { patientUserId } })
}
export function findOrder(registerId) {
  return request({ url: '/doctor/findOrder', method: 'get', params: { registerId } })
}
export function submitDiagnosis(data) {
  return request({ url: '/doctor/submitDiagnosis', method: 'post', data })
}
export function addPrescriptions(data) {
  return request({ url: '/doctor/addPrescriptions', method: 'post', data })
}
export function findPrescriptions(registerId) {
  return request({ url: '/doctor/findPrescriptions', method: 'get', params: { registerId } })
}
export function deletePrescription(prescriptionId) {
  return request({
    url: '/doctor/deletePrescription',
    method: 'post',
    params: { prescriptionId }
  })
}
export function addRegisterChecks(data) {
  return request({ url: '/doctor/addRegisterChecks', method: 'post', data })
}
export function findRegisterChecks(registerId) {
  return request({ url: '/doctor/findRegisterChecks', method: 'get', params: { registerId } })
}
export function applyBed(data) {
  return request({ url: '/doctor/applyBed', method: 'post', data })
}
export function finishRegister(registerId) {
  return request({ url: '/doctor/finishRegister', method: 'post', params: { registerId } })
}
export function findHistoryRegisters(patientUserId) {
  return request({
    url: '/doctor/findHistoryRegisters',
    method: 'get',
    params: { patientUserId }
  })
}
