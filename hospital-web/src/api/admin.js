import request from '@/utils/request'

/* ---------------- 认证 ---------------- */
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

/* ---------------- 运营统计 ---------------- */
export function getStatistics() {
  return request({ url: '/admin/getStatistics', method: 'get' })
}

/* ---------------- 医生管理 ---------------- */
export function findAllDoctors(params) {
  return request({ url: '/admin/findAllDoctors', method: 'get', params })
}
export function addDoctor(data) {
  return request({ url: '/admin/addDoctor', method: 'post', data })
}
export function modifyDoctor(data) {
  return request({ url: '/admin/modifyDoctor', method: 'post', data })
}
export function deleteDoctor(doctorId) {
  return request({ url: '/admin/deleteDoctor', method: 'post', params: { doctorId } })
}
export function exportDoctor(params) {
  return request({ url: '/admin/exportDoctor', method: 'get', params, responseType: 'blob' })
}
export function importDoctor(data) {
  return request({
    url: '/admin/importDoctor',
    method: 'post',
    data,
    headers: { 'Content-Type': 'multipart/form-data' }
  })
}
export function findDepartments() {
  return request({ url: '/admin/findDepartments', method: 'get' })
}

/* ---------------- 患者管理 ---------------- */
export function findAllPatients(params) {
  return request({ url: '/admin/findAllPatients', method: 'get', params })
}
export function deletePatient(userId) {
  return request({ url: '/admin/deletePatient', method: 'post', params: { userId } })
}

/* ---------------- 挂号管理 ---------------- */
export function findAllOrders(params) {
  return request({ url: '/admin/findAllOrders', method: 'get', params })
}
export function findOrder(registerId) {
  return request({ url: '/admin/findOrder', method: 'get', params: { registerId } })
}
export function deleteOrder(registerId) {
  return request({ url: '/admin/deleteOrder', method: 'post', params: { registerId } })
}

/* ---------------- 药品管理 ---------------- */
export function findAllDrugs(params) {
  return request({ url: '/admin/findAllDrugs', method: 'get', params })
}
export function findAllDrugList() {
  return request({ url: '/admin/findAllDrugList', method: 'get' })
}
export function addDrug(data) {
  return request({ url: '/admin/addDrug', method: 'post', data })
}
export function modifyDrug(data) {
  return request({ url: '/admin/modifyDrug', method: 'post', data })
}
export function deleteDrug(drugId) {
  return request({ url: '/admin/deleteDrug', method: 'post', params: { drugId } })
}

/* ---------------- 检查项目管理 ---------------- */
export function findAllChecks(params) {
  return request({ url: '/admin/findAllChecks', method: 'get', params })
}
export function findAllCheckList() {
  return request({ url: '/admin/findAllCheckList', method: 'get' })
}
export function addCheck(data) {
  return request({ url: '/admin/addCheck', method: 'post', data })
}
export function modifyCheck(data) {
  return request({ url: '/admin/modifyCheck', method: 'post', data })
}
export function deleteCheck(checkId) {
  return request({ url: '/admin/deleteCheck', method: 'post', params: { checkId } })
}

/* ---------------- 病床管理 ---------------- */
export function findAllBeds(params) {
  return request({ url: '/admin/findAllBeds', method: 'get', params })
}
export function findFreeBeds() {
  return request({ url: '/admin/findFreeBeds', method: 'get' })
}
export function addBed(data) {
  return request({ url: '/admin/addBed', method: 'post', data })
}
export function modifyBed(data) {
  return request({ url: '/admin/modifyBed', method: 'post', data })
}
export function deleteBed(bedId) {
  return request({ url: '/admin/deleteBed', method: 'post', params: { bedId } })
}

/* ---------------- 排班管理 ---------------- */
export function findAllSchedules(params) {
  return request({ url: '/admin/findAllSchedules', method: 'get', params })
}
export function addSchedule(data) {
  return request({ url: '/admin/addSchedule', method: 'post', data })
}
export function modifySchedule(data) {
  return request({ url: '/admin/modifySchedule', method: 'post', data })
}
export function deleteSchedule(scheduleId) {
  return request({ url: '/admin/deleteSchedule', method: 'post', params: { scheduleId } })
}
