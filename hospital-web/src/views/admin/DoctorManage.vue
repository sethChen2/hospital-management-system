<template>
  <div class="page-container">
    <el-card shadow="never">
      <!-- 查询条件 -->
      <el-form :inline="true" class="filter-bar" :model="query">
        <el-form-item label="关键字">
          <el-input
            v-model="query.keyword"
            placeholder="账号 / 姓名 / 手机号"
            clearable
            style="width: 200px"
            @keyup.enter.native="handleSearch"
          />
        </el-form-item>
        <el-form-item label="科室">
          <el-select v-model="query.department" placeholder="全部" clearable style="width: 140px">
            <el-option v-for="d in departments" :key="d" :label="d" :value="d" />
          </el-select>
        </el-form-item>
        <el-form-item label="在职">
          <el-select v-model="query.isOnJob" placeholder="全部" clearable style="width: 110px">
            <el-option label="在职" :value="1" />
            <el-option label="离职" :value="0" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" icon="el-icon-search" @click="handleSearch">查询</el-button>
          <el-button icon="el-icon-refresh" @click="handleReset">重置</el-button>
        </el-form-item>
      </el-form>

      <div class="table-toolbar">
        <el-button type="primary" icon="el-icon-plus" @click="openAdd">新增医生</el-button>
        <el-button icon="el-icon-download" @click="handleExport">导出 Excel</el-button>
        <el-upload
          action=""
          :show-file-list="false"
          :http-request="handleImport"
          accept=".xls,.xlsx"
          style="display: inline-block; margin-left: 10px"
        >
          <el-button icon="el-icon-upload2">导入 Excel</el-button>
        </el-upload>
      </div>

      <el-table :data="list" border stripe v-loading="loading">
        <el-table-column type="index" label="#" width="50" align="center" />
        <el-table-column prop="account" label="账号" width="110" />
        <el-table-column prop="name" label="姓名" width="100" />
        <el-table-column prop="gender" label="性别" width="60" align="center" />
        <el-table-column prop="department" label="科室" width="90" />
        <el-table-column prop="position" label="职称" width="100" />
        <el-table-column prop="registerFee" label="挂号费" width="80" align="right" />
        <el-table-column prop="phone" label="手机号" width="120" />
        <el-table-column prop="email" label="邮箱" min-width="150" show-overflow-tooltip />
        <el-table-column label="在职" width="70" align="center">
          <template slot-scope="{ row }">
            <el-tag :type="row.isOnJob === 1 ? 'success' : 'info'" size="mini">
              {{ row.isOnJob === 1 ? '在职' : '离职' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="70" align="center">
          <template slot-scope="{ row }">
            <el-tag :type="row.accountStatus === 1 ? 'success' : 'danger'" size="mini">
              {{ row.accountStatus === 1 ? '正常' : '禁用' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="150" fixed="right">
          <template slot-scope="{ row }">
            <el-button type="text" @click="openEdit(row)">编辑</el-button>
            <el-button type="text" class="danger-text" @click="handleDelete(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>

      <div class="pagination-wrap">
        <el-pagination
          background
          layout="total, prev, pager, next, sizes"
          :total="total"
          :current-page.sync="query.page"
          :page-size.sync="query.size"
          :page-sizes="[10, 20, 50]"
          @current-change="load"
          @size-change="handleSearch"
        />
      </div>
    </el-card>

    <!-- 新增 / 编辑 -->
    <el-dialog :title="form.doctorId ? '编辑医生' : '新增医生'" :visible.sync="dialogVisible" width="620px">
      <el-form ref="form" :model="form" :rules="rules" label-width="90px">
        <el-row :gutter="12">
          <el-col :span="12">
            <el-form-item label="账号" prop="account">
              <el-input v-model="form.account" :disabled="!!form.doctorId" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item v-if="!form.doctorId" label="密码" prop="password">
              <el-input v-model="form.password" type="password" show-password placeholder="6-20 位" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="姓名" prop="name">
              <el-input v-model="form.name" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="性别" prop="gender">
              <el-radio-group v-model="form.gender">
                <el-radio label="男">男</el-radio>
                <el-radio label="女">女</el-radio>
              </el-radio-group>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="科室" prop="department">
              <el-input v-model="form.department" placeholder="如：内科" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="职称" prop="position">
              <el-input v-model="form.position" placeholder="如：主任医师" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="挂号费" prop="registerFee">
              <el-input-number v-model="form.registerFee" :min="0" :precision="2" :step="1" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="手机号" prop="phone">
              <el-input v-model="form.phone" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="身份证" prop="idcard">
              <el-input v-model="form.idcard" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="出生日期">
              <el-date-picker
                v-model="form.birth"
                type="date"
                value-format="yyyy-MM-dd"
                style="width: 100%"
              />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="邮箱">
              <el-input v-model="form.email" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="在职状态">
              <el-select v-model="form.isOnJob" style="width: 100%">
                <el-option label="在职" :value="1" />
                <el-option label="离职" :value="0" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="账号状态">
              <el-select v-model="form.accountStatus" style="width: 100%">
                <el-option label="正常" :value="1" />
                <el-option label="禁用" :value="0" />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>
      </el-form>
      <div slot="footer">
        <el-button @click="dialogVisible = false">取 消</el-button>
        <el-button type="primary" :loading="saving" @click="handleSave">保 存</el-button>
      </div>
    </el-dialog>
  </div>
</template>

<script>
import {
  findAllDoctors,
  addDoctor,
  modifyDoctor,
  deleteDoctor,
  exportDoctor,
  importDoctor,
  findDepartments
} from '@/api/admin'

const emptyForm = () => ({
  doctorId: null,
  account: '',
  password: '',
  name: '',
  gender: '男',
  phone: '',
  email: '',
  idcard: '',
  birth: '',
  position: '',
  department: '',
  registerFee: 10,
  isOnJob: 1,
  accountStatus: 1
})

export default {
  name: 'AdminDoctor',
  data() {
    return {
      loading: false,
      saving: false,
      list: [],
      total: 0,
      departments: [],
      query: { page: 1, size: 10, keyword: '', department: '', isOnJob: null },
      dialogVisible: false,
      form: emptyForm(),
      rules: {
        account: [{ required: true, message: '请输入账号', trigger: 'blur' }],
        password: [
          { required: true, message: '请输入密码', trigger: 'blur' },
          { min: 6, max: 20, message: '密码长度需为 6-20 位', trigger: 'blur' }
        ],
        name: [{ required: true, message: '请输入姓名', trigger: 'blur' }],
        department: [{ required: true, message: '请输入科室', trigger: 'blur' }]
      }
    }
  },
  created() {
    this.loadDepartments()
    this.load()
  },
  methods: {
    loadDepartments() {
      findDepartments().then(res => {
        this.departments = res.data || []
      })
    },
    load() {
      this.loading = true
      findAllDoctors(this.query)
        .then(res => {
          const page = res.data || {}
          this.list = page.records || []
          this.total = page.total || 0
        })
        .finally(() => {
          this.loading = false
        })
    },
    handleSearch() {
      this.query.page = 1
      this.load()
    },
    handleReset() {
      this.query = { page: 1, size: 10, keyword: '', department: '', isOnJob: null }
      this.load()
    },
    openAdd() {
      this.form = emptyForm()
      this.dialogVisible = true
      this.$nextTick(() => this.$refs.form && this.$refs.form.clearValidate())
    },
    openEdit(row) {
      this.form = Object.assign(emptyForm(), row)
      this.dialogVisible = true
      this.$nextTick(() => this.$refs.form && this.$refs.form.clearValidate())
    },
    handleSave() {
      this.$refs.form.validate(valid => {
        if (!valid) return
        this.saving = true
        const req = this.form.doctorId ? modifyDoctor(this.form) : addDoctor(this.form)
        req
          .then(res => {
            this.$message.success(res.msg || '保存成功')
            this.dialogVisible = false
            this.loadDepartments()
            this.load()
          })
          .finally(() => {
            this.saving = false
          })
      })
    },
    handleDelete(row) {
      this.$confirm(`确定删除医生「${row.name}」吗？`, '提示', { type: 'warning' })
        .then(() => deleteDoctor(row.doctorId))
        .then(res => {
          this.$message.success(res.msg || '删除成功')
          this.load()
        })
        .catch(() => {})
    },
    handleExport() {
      exportDoctor({
        keyword: this.query.keyword,
        department: this.query.department,
        isOnJob: this.query.isOnJob
      }).then(response => {
        const blob = new Blob([response.data], {
          type: 'application/vnd.ms-excel'
        })
        const url = window.URL.createObjectURL(blob)
        const a = document.createElement('a')
        a.href = url
        a.download = '医生信息表.xlsx'
        a.click()
        window.URL.revokeObjectURL(url)
      })
    },
    handleImport(option) {
      const formData = new FormData()
      formData.append('file', option.file)
      importDoctor(formData)
        .then(res => {
          this.$message.success(res.msg || '导入完成')
          this.loadDepartments()
          this.load()
        })
        .catch(() => {})
    }
  }
}
</script>

<style scoped>
.danger-text {
  color: #f56c6c;
}
</style>
