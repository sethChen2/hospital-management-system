import Vue from 'vue'
import VueRouter from 'vue-router'

Vue.use(VueRouter)

// 解决重复点击同一路由时的 NavigationDuplicated 报错
const originalPush = VueRouter.prototype.push
VueRouter.prototype.push = function push(location) {
  return originalPush.call(this, location).catch(err => err)
}

const Layout = () => import('@/layout/Index.vue')

const routes = [
  { path: '/login', name: 'Login', component: () => import('@/views/Login.vue') },
  { path: '/register', name: 'Register', component: () => import('@/views/Register.vue') },
  {
    path: '/admin',
    component: Layout,
    redirect: '/admin/statistics',
    meta: { role: 'ADMIN', title: '管理员' },
    children: [
      {
        path: 'statistics',
        name: 'AdminStatistics',
        component: () => import('@/views/admin/Statistics.vue'),
        meta: { title: '运营统计', icon: 'el-icon-data-line' }
      },
      {
        path: 'doctor',
        name: 'AdminDoctor',
        component: () => import('@/views/admin/DoctorManage.vue'),
        meta: { title: '医生管理', icon: 'el-icon-user' }
      },
      {
        path: 'patient',
        name: 'AdminPatient',
        component: () => import('@/views/admin/PatientManage.vue'),
        meta: { title: '患者管理', icon: 'el-icon-user-solid' }
      },
      {
        path: 'order',
        name: 'AdminOrder',
        component: () => import('@/views/admin/OrderManage.vue'),
        meta: { title: '挂号管理', icon: 'el-icon-tickets' }
      },
      {
        path: 'schedule',
        name: 'AdminSchedule',
        component: () => import('@/views/admin/ScheduleManage.vue'),
        meta: { title: '排班管理', icon: 'el-icon-date' }
      },
      {
        path: 'drug',
        name: 'AdminDrug',
        component: () => import('@/views/admin/DrugManage.vue'),
        meta: { title: '药品管理', icon: 'el-icon-first-aid-kit' }
      },
      {
        path: 'check',
        name: 'AdminCheck',
        component: () => import('@/views/admin/CheckManage.vue'),
        meta: { title: '检查项目', icon: 'el-icon-s-claim' }
      },
      {
        path: 'bed',
        name: 'AdminBed',
        component: () => import('@/views/admin/BedManage.vue'),
        meta: { title: '病床管理', icon: 'el-icon-house' }
      },
      {
        path: 'profile',
        name: 'AdminProfile',
        component: () => import('@/views/Profile.vue'),
        meta: { title: '个人中心', icon: 'el-icon-setting' }
      }
    ]
  },
  {
    path: '/doctor',
    component: Layout,
    redirect: '/doctor/order',
    meta: { role: 'DOCTOR', title: '医生' },
    children: [
      {
        path: 'order',
        name: 'DoctorOrder',
        component: () => import('@/views/doctor/OrderList.vue'),
        meta: { title: '就诊管理', icon: 'el-icon-tickets' }
      },
      {
        path: 'diagnosis/:registerId',
        name: 'DoctorDiagnosis',
        component: () => import('@/views/doctor/Diagnosis.vue'),
        meta: { title: '接诊工作台', hidden: true }
      },
      {
        path: 'schedule',
        name: 'DoctorSchedule',
        component: () => import('@/views/doctor/MySchedule.vue'),
        meta: { title: '我的排班', icon: 'el-icon-date' }
      },
      {
        path: 'history',
        name: 'DoctorHistory',
        component: () => import('@/views/doctor/PatientHistory.vue'),
        meta: { title: '患者历史', icon: 'el-icon-search' }
      },
      {
        path: 'profile',
        name: 'DoctorProfile',
        component: () => import('@/views/Profile.vue'),
        meta: { title: '个人中心', icon: 'el-icon-setting' }
      }
    ]
  },
  {
    path: '/patient',
    component: Layout,
    redirect: '/patient/appointment',
    meta: { role: 'PATIENT', title: '患者' },
    children: [
      {
        path: 'appointment',
        name: 'PatientAppointment',
        component: () => import('@/views/patient/Appointment.vue'),
        meta: { title: '预约挂号', icon: 'el-icon-plus' }
      },
      {
        path: 'orders',
        name: 'PatientOrders',
        component: () => import('@/views/patient/MyOrders.vue'),
        meta: { title: '我的挂号', icon: 'el-icon-tickets' }
      },
      {
        path: 'order/:registerId',
        name: 'PatientOrderDetail',
        component: () => import('@/views/patient/OrderDetail.vue'),
        meta: { title: '挂号详情', hidden: true }
      },
      {
        path: 'bed',
        name: 'PatientBed',
        component: () => import('@/views/patient/BedInfo.vue'),
        meta: { title: '住院信息', icon: 'el-icon-house' }
      },
      {
        path: 'profile',
        name: 'PatientProfile',
        component: () => import('@/views/Profile.vue'),
        meta: { title: '个人中心', icon: 'el-icon-setting' }
      }
    ]
  },
  { path: '/', redirect: '/login' },
  { path: '/404', name: 'NotFound', component: () => import('@/views/NotFound.vue') },
  { path: '*', redirect: '/404' }
]

const router = new VueRouter({
  mode: 'hash',
  routes
})

/** 角色对应的首页 */
export function roleHome(role) {
  if (role === 'ADMIN') return '/admin/statistics'
  if (role === 'DOCTOR') return '/doctor/order'
  if (role === 'PATIENT') return '/patient/appointment'
  return '/login'
}

/* ---------------- 路由守卫：登录校验 + 角色校验 ---------------- */
router.beforeEach((to, from, next) => {
  const token = localStorage.getItem('hospital_token')
  const role = localStorage.getItem('hospital_role')

  document.title = (to.meta && to.meta.title ? to.meta.title + ' - ' : '') + '医院管理系统'

  // 公开页面
  if (to.path === '/login' || to.path === '/register') {
    if (token) {
      return next(roleHome(role))
    }
    return next()
  }

  // 未登录：一律弹回登录页
  if (!token) {
    return next('/login')
  }

  // 角色不匹配：弹回本角色首页（例如患者手输 /admin/statistics）
  const matched = to.matched.find(r => r.meta && r.meta.role)
  if (matched && matched.meta.role !== role) {
    return next(roleHome(role))
  }

  next()
})

export default router
