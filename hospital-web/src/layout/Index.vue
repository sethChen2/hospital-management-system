<template>
  <el-container class="layout">
    <el-aside width="210px" class="layout-aside">
      <div class="logo">
        <i class="el-icon-first-aid-kit"></i>
        <span>医院管理系统</span>
      </div>
      <el-menu
        :default-active="activeMenu"
        router
        background-color="#2b3648"
        text-color="#c0c4cc"
        active-text-color="#ffffff"
        class="layout-menu"
      >
        <el-menu-item v-for="item in menus" :key="item.path" :index="item.path">
          <i :class="item.icon"></i>
          <span slot="title">{{ item.title }}</span>
        </el-menu-item>
      </el-menu>
    </el-aside>

    <el-container>
      <el-header class="layout-header">
        <div class="header-left">
          <span class="role-tag">{{ roleName }}</span>
          <span class="welcome">欢迎您，{{ name }}</span>
        </div>
        <div class="header-right">
          <el-button type="text" icon="el-icon-user" @click="goProfile">个人中心</el-button>
          <el-button type="text" icon="el-icon-switch-button" @click="handleLogout">
            退出登录
          </el-button>
        </div>
      </el-header>

      <el-main class="layout-main">
        <router-view />
      </el-main>
    </el-container>
  </el-container>
</template>

<script>
import { logout } from '@/api/user'

const ROLE_NAME = { ADMIN: '管理员', DOCTOR: '医生', PATIENT: '患者' }

export default {
  name: 'LayoutIndex',
  computed: {
    role() {
      return this.$store.getters.role
    },
    name() {
      return this.$store.getters.name || this.$store.getters.userInfo?.account || ''
    },
    roleName() {
      return ROLE_NAME[this.role] || ''
    },
    /** 当前角色路由下的可见子菜单 */
    menus() {
      const parent = this.$router.options.routes.find(
        r => r.meta && r.meta.role === this.role
      )
      if (!parent) return []
      const base = '/' + (parent.path || '').replace(/^\//, '')
      return (parent.children || [])
        .filter(c => !(c.meta && c.meta.hidden))
        .map(c => ({
          path: base + '/' + c.path,
          title: c.meta.title,
          icon: c.meta.icon
        }))
    },
    activeMenu() {
      return this.$route.path
    }
  },
  methods: {
    goProfile() {
      const map = { ADMIN: '/admin/profile', DOCTOR: '/doctor/profile', PATIENT: '/patient/profile' }
      this.$router.push(map[this.role] || '/login')
    },
    handleLogout() {
      this.$confirm('确定退出登录吗？', '提示', { type: 'warning' })
        .then(() => logout().catch(() => {}))
        .then(() => {
          this.$store.dispatch('clearLogin')
          this.$message.success('已退出登录')
          this.$router.push('/login')
        })
        .catch(() => {})
    }
  }
}
</script>

<style scoped>
.layout {
  height: 100%;
}

.layout-aside {
  background-color: #2b3648;
  overflow-x: hidden;
}

.logo {
  height: 60px;
  line-height: 60px;
  text-align: center;
  color: #fff;
  font-size: 17px;
  font-weight: 600;
  letter-spacing: 1px;
  border-bottom: 1px solid #364254;
}

.logo i {
  margin-right: 6px;
  color: #409eff;
}

.layout-menu {
  border-right: none;
  padding-top: 6px;
}

.layout-menu ::v-deep .el-menu-item {
  height: 44px;
  line-height: 44px;
  margin: 3px 10px;
  border-radius: var(--radius-sm);
}

.layout-menu ::v-deep .el-menu-item i {
  color: inherit;
}

.layout-menu ::v-deep .el-menu-item:hover {
  background-color: rgba(255, 255, 255, 0.07) !important;
  color: #fff !important;
}

.layout-menu ::v-deep .el-menu-item.is-active {
  background: linear-gradient(
    90deg,
    rgba(64, 158, 255, 0.32) 0%,
    rgba(64, 158, 255, 0.06) 100%
  ) !important;
  color: #fff !important;
  box-shadow: inset 3px 0 0 var(--brand);
}

.layout-header {
  height: 60px;
  background: #fff;
  display: flex;
  align-items: center;
  justify-content: space-between;
  box-shadow: 0 1px 4px rgba(0, 21, 41, 0.08);
}

.role-tag {
  display: inline-block;
  padding: 2px 10px;
  margin-right: 10px;
  border-radius: 10px;
  font-size: 12px;
  color: #409eff;
  background: #ecf5ff;
  border: 1px solid #d9ecff;
}

.welcome {
  color: #606266;
}

.layout-main {
  padding: 0;
  background: #f0f2f5;
  overflow-y: auto;
}
</style>
