<template>
  <div class="page-container">
    <el-row :gutter="16">
      <el-col :span="10">
        <el-card shadow="never">
          <h3 class="card-title">个人信息</h3>
          <el-descriptions :column="1" border size="medium">
            <el-descriptions-item label="账号">{{ info.account }}</el-descriptions-item>
            <el-descriptions-item label="姓名">{{ info.name }}</el-descriptions-item>
            <el-descriptions-item label="角色">{{ roleName }}</el-descriptions-item>
            <el-descriptions-item label="性别">{{ info.gender || '-' }}</el-descriptions-item>
            <el-descriptions-item label="手机号">{{ info.phone || '-' }}</el-descriptions-item>
            <el-descriptions-item label="身份证">{{ info.idcard || '-' }}</el-descriptions-item>
            <el-descriptions-item label="出生日期">{{ info.birth || '-' }}</el-descriptions-item>
            <el-descriptions-item label="邮箱">{{ info.email || '-' }}</el-descriptions-item>
            <template v-if="info.role === 'DOCTOR'">
              <el-descriptions-item label="科室">{{ info.department || '-' }}</el-descriptions-item>
              <el-descriptions-item label="职称">{{ info.position || '-' }}</el-descriptions-item>
              <el-descriptions-item label="挂号费">{{ info.registerFee }} 元</el-descriptions-item>
            </template>
            <el-descriptions-item label="账号状态">
              <el-tag :type="info.accountStatus === 1 ? 'success' : 'danger'" size="mini">
                {{ info.accountStatus === 1 ? '正常' : '禁用' }}
              </el-tag>
            </el-descriptions-item>
          </el-descriptions>
        </el-card>
      </el-col>

      <el-col :span="14">
        <el-card shadow="never">
          <h3 class="card-title">修改资料</h3>
          <el-form ref="infoForm" :model="infoForm" label-width="80px">
            <el-form-item label="姓名">
              <el-input v-model="infoForm.name" />
            </el-form-item>
            <el-form-item label="性别">
              <el-radio-group v-model="infoForm.gender">
                <el-radio label="男">男</el-radio>
                <el-radio label="女">女</el-radio>
              </el-radio-group>
            </el-form-item>
            <el-form-item label="手机号">
              <el-input v-model="infoForm.phone" />
            </el-form-item>
            <el-form-item label="身份证">
              <el-input v-model="infoForm.idcard" />
            </el-form-item>
            <el-form-item label="出生日期">
              <el-date-picker
                v-model="infoForm.birth"
                type="date"
                value-format="yyyy-MM-dd"
                style="width: 100%"
              />
            </el-form-item>
            <el-form-item label="邮箱">
              <el-input v-model="infoForm.email" />
            </el-form-item>
            <el-form-item>
              <el-button type="primary" :loading="saving" @click="saveInfo">保存资料</el-button>
            </el-form-item>
          </el-form>
        </el-card>

        <el-card shadow="never" style="margin-top: 16px">
          <h3 class="card-title">修改密码</h3>
          <el-form ref="pwdForm" :model="pwdForm" :rules="pwdRules" label-width="80px">
            <el-form-item label="原密码" prop="oldPassword">
              <el-input v-model="pwdForm.oldPassword" type="password" show-password />
            </el-form-item>
            <el-form-item label="新密码" prop="newPassword">
              <el-input v-model="pwdForm.newPassword" type="password" show-password />
            </el-form-item>
            <el-form-item>
              <el-button type="primary" :loading="pwdSaving" @click="savePassword">
                修改密码
              </el-button>
            </el-form-item>
          </el-form>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script>
import { getInfo, modifyInfo, modifyPassword } from '@/api/user'

const ROLE_NAME = { ADMIN: '管理员', DOCTOR: '医生', PATIENT: '患者' }

export default {
  name: 'ProfilePage',
  data() {
    return {
      info: {},
      infoForm: {},
      pwdForm: { oldPassword: '', newPassword: '' },
      pwdRules: {
        oldPassword: [{ required: true, message: '请输入原密码', trigger: 'blur' }],
        newPassword: [
          { required: true, message: '请输入新密码', trigger: 'blur' },
          { min: 6, max: 20, message: '密码长度需为 6-20 位', trigger: 'blur' }
        ]
      },
      saving: false,
      pwdSaving: false
    }
  },
  computed: {
    roleName() {
      return ROLE_NAME[this.info.role] || ''
    }
  },
  created() {
    this.load()
  },
  methods: {
    load() {
      getInfo().then(res => {
        this.info = res.data || {}
        this.infoForm = {
          name: this.info.name,
          gender: this.info.gender,
          phone: this.info.phone,
          idcard: this.info.idcard,
          birth: this.info.birth,
          email: this.info.email
        }
        // 同步 store，保证顶栏姓名一致
        this.$store.dispatch('setUserInfo', Object.assign({}, this.$store.getters.userInfo, this.info))
      })
    },
    saveInfo() {
      this.saving = true
      modifyInfo(this.infoForm)
        .then(() => {
          this.$message.success('资料已更新')
          this.load()
        })
        .finally(() => {
          this.saving = false
        })
    },
    savePassword() {
      this.$refs.pwdForm.validate(valid => {
        if (!valid) return
        this.pwdSaving = true
        modifyPassword(this.pwdForm)
          .then(() => {
            this.$message.success('密码修改成功，请重新登录')
            this.$store.dispatch('clearLogin')
            this.$router.push('/login')
          })
          .finally(() => {
            this.pwdSaving = false
          })
      })
    }
  }
}
</script>
