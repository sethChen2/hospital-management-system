<template>
  <div class="login-page auth-page" :style="authBgStyle">
    <div class="login-box auth-card">
      <div class="auth-brand">
        <div class="auth-logo">
          <i class="el-icon-first-aid-kit"></i>
        </div>
        <h1>医院管理系统</h1>
        <p>Hospital Management System</p>
      </div>

      <el-form ref="form" :model="form" :rules="rules" class="login-form" @submit.native.prevent>
        <el-form-item prop="account">
          <el-input
            v-model="form.account"
            prefix-icon="el-icon-user"
            placeholder="请输入账号"
            clearable
            @keyup.enter.native="handleLogin"
          />
        </el-form-item>
        <el-form-item prop="password">
          <el-input
            v-model="form.password"
            type="password"
            prefix-icon="el-icon-lock"
            placeholder="请输入密码"
            show-password
            @keyup.enter.native="handleLogin"
          />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :loading="loading" class="auth-submit" @click="handleLogin">
            登 录
          </el-button>
        </el-form-item>
      </el-form>

      <div class="login-links">
        <el-button type="text" @click="dialogVisible = true">忘记密码？</el-button>
        <el-button type="text" @click="$router.push('/register')">患者注册</el-button>
      </div>

      <div class="login-tip">
        演示账号：admin / 123456（管理员），doctor01 / 123456（医生），patient01 / 123456（患者）
      </div>
    </div>

    <!-- 找回密码：账号 + 身份证 -->
    <el-dialog title="找回密码" :visible.sync="dialogVisible" width="420px">
      <el-form ref="backForm" :model="backForm" :rules="backRules" label-width="80px">
        <el-form-item label="账号" prop="account">
          <el-input v-model="backForm.account" placeholder="请输入账号" />
        </el-form-item>
        <el-form-item label="身份证" prop="idcard">
          <el-input v-model="backForm.idcard" placeholder="请输入身份证号" />
        </el-form-item>
        <el-form-item label="新密码" prop="newPassword">
          <el-input
            v-model="backForm.newPassword"
            type="password"
            show-password
            placeholder="6-20 位"
          />
        </el-form-item>
      </el-form>
      <div slot="footer">
        <el-button @click="dialogVisible = false">取 消</el-button>
        <el-button type="primary" :loading="backLoading" @click="handleBack">确 定</el-button>
      </div>
    </el-dialog>
  </div>
</template>

<script>
import { login, findBackPassword } from '@/api/user'
import { roleHome } from '@/router'

export default {
  name: 'LoginPage',
  computed: {
    authBgStyle() {
      return { backgroundImage: `url(${process.env.BASE_URL}login-bg.jpg)` }
    }
  },
  data() {
    return {
      loading: false,
      dialogVisible: false,
      backLoading: false,
      form: { account: '', password: '' },
      rules: {
        account: [{ required: true, message: '请输入账号', trigger: 'blur' }],
        password: [{ required: true, message: '请输入密码', trigger: 'blur' }]
      },
      backForm: { account: '', idcard: '', newPassword: '' },
      backRules: {
        account: [{ required: true, message: '请输入账号', trigger: 'blur' }],
        idcard: [{ required: true, message: '请输入身份证号', trigger: 'blur' }],
        newPassword: [
          { required: true, message: '请输入新密码', trigger: 'blur' },
          { min: 6, max: 20, message: '密码长度需为 6-20 位', trigger: 'blur' }
        ]
      }
    }
  },
  methods: {
    handleLogin() {
      this.$refs.form.validate(valid => {
        if (!valid) return
        this.loading = true
        login(this.form)
          .then(res => {
            const data = res.data || {}
            this.$store.dispatch('setLogin', {
              token: data.token,
              role: data.role,
              userInfo: data
            })
            this.$message.success('登录成功')
            this.$router.push(roleHome(data.role))
          })
          .finally(() => {
            this.loading = false
          })
      })
    },
    handleBack() {
      this.$refs.backForm.validate(valid => {
        if (!valid) return
        this.backLoading = true
        findBackPassword(this.backForm)
          .then(() => {
            this.$message.success('密码重置成功，请使用新密码登录')
            this.dialogVisible = false
            this.backForm = { account: '', idcard: '', newPassword: '' }
          })
          .finally(() => {
            this.backLoading = false
          })
      })
    }
  }
}
</script>

<style scoped>
.login-box {
  width: 400px;
}

.login-form {
  margin-top: 4px;
}

.login-links {
  display: flex;
  justify-content: space-between;
  margin-top: -4px;
}

.login-tip {
  margin-top: 16px;
  padding-top: 12px;
  border-top: 1px dashed #ebeef5;
  font-size: 12px;
  color: #909399;
  line-height: 1.8;
}
</style>
