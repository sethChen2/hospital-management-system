<template>
  <div class="register-page auth-page" :style="authBgStyle">
    <div class="register-box auth-card">
      <div class="auth-brand">
        <div class="auth-logo">
          <i class="el-icon-user-solid"></i>
        </div>
        <h1>患者注册</h1>
        <p>PATIENT REGISTRATION</p>
      </div>
      <el-form ref="form" :model="form" :rules="rules" label-width="80px">
        <el-form-item label="账号" prop="account">
          <el-input v-model="form.account" placeholder="4-20 位，登录使用" />
        </el-form-item>
        <el-form-item label="密码" prop="password">
          <el-input v-model="form.password" type="password" show-password placeholder="6-20 位" />
        </el-form-item>
        <el-form-item label="姓名" prop="name">
          <el-input v-model="form.name" placeholder="请输入真实姓名" />
        </el-form-item>
        <el-form-item label="性别" prop="gender">
          <el-radio-group v-model="form.gender">
            <el-radio label="男">男</el-radio>
            <el-radio label="女">女</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="手机号" prop="phone">
          <el-input v-model="form.phone" placeholder="11 位手机号" />
        </el-form-item>
        <el-form-item label="身份证" prop="idcard">
          <el-input v-model="form.idcard" placeholder="选填，用于找回密码" />
        </el-form-item>
        <el-form-item label="出生日期" prop="birth">
          <el-date-picker
            v-model="form.birth"
            type="date"
            value-format="yyyy-MM-dd"
            placeholder="选择日期"
            style="width: 100%"
          />
        </el-form-item>
        <el-form-item label="邮箱" prop="email">
          <el-input v-model="form.email" placeholder="选填" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :loading="loading" class="auth-submit" @click="handleSubmit">
            注 册
          </el-button>
        </el-form-item>
      </el-form>
      <div class="register-footer">
        <el-button type="text" @click="$router.push('/login')">已有账号？返回登录</el-button>
      </div>
    </div>
  </div>
</template>

<script>
import { addPatient } from '@/api/user'

export default {
  name: 'RegisterPage',
  computed: {
    authBgStyle() {
      return { backgroundImage: `url(${process.env.BASE_URL}login-bg.jpg)` }
    }
  },
  data() {
    return {
      loading: false,
      form: {
        account: '',
        password: '',
        name: '',
        gender: '男',
        phone: '',
        idcard: '',
        birth: '',
        email: ''
      },
      rules: {
        account: [
          { required: true, message: '请输入账号', trigger: 'blur' },
          { min: 4, max: 20, message: '账号长度需为 4-20 位', trigger: 'blur' }
        ],
        password: [
          { required: true, message: '请输入密码', trigger: 'blur' },
          { min: 6, max: 20, message: '密码长度需为 6-20 位', trigger: 'blur' }
        ],
        name: [{ required: true, message: '请输入姓名', trigger: 'blur' }],
        gender: [{ required: true, message: '请选择性别', trigger: 'change' }],
        phone: [
          { required: true, message: '请输入手机号', trigger: 'blur' },
          { pattern: /^1[3-9]\d{9}$/, message: '手机号格式不正确', trigger: 'blur' }
        ],
        idcard: [
          { pattern: /^$|^\d{17}[0-9Xx]$/, message: '身份证号格式不正确', trigger: 'blur' }
        ]
      }
    }
  },
  methods: {
    handleSubmit() {
      this.$refs.form.validate(valid => {
        if (!valid) return
        this.loading = true
        addPatient(this.form)
          .then(() => {
            this.$message.success('注册成功，请登录')
            this.$router.push('/login')
          })
          .finally(() => {
            this.loading = false
          })
      })
    }
  }
}
</script>

<style scoped>
.register-box {
  width: 460px;
  padding: 32px 36px 20px;
}

.register-footer {
  text-align: center;
}
</style>
