<template>
  <div class="page-container">
    <el-card shadow="never">
      <el-form :inline="true" class="filter-bar" :model="query">
        <el-form-item label="关键字">
          <el-input
            v-model="query.keyword"
            placeholder="检查项目名称"
            clearable
            style="width: 200px"
            @keyup.enter.native="handleSearch"
          />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" icon="el-icon-search" @click="handleSearch">查询</el-button>
          <el-button icon="el-icon-refresh" @click="handleReset">重置</el-button>
        </el-form-item>
      </el-form>

      <div class="table-toolbar">
        <el-button type="primary" icon="el-icon-plus" @click="openAdd">新增检查项目</el-button>
      </div>

      <el-table :data="list" border stripe v-loading="loading">
        <el-table-column prop="checkId" label="编号" width="80" align="center" />
        <el-table-column prop="checkName" label="检查项目" min-width="220" />
        <el-table-column prop="price" label="费用（元）" width="130" align="right" />
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

    <el-dialog :title="form.checkId ? '编辑检查项目' : '新增检查项目'" :visible.sync="dialogVisible" width="440px">
      <el-form ref="form" :model="form" :rules="rules" label-width="90px">
        <el-form-item label="项目名称" prop="checkName">
          <el-input v-model="form.checkName" placeholder="如：血常规" />
        </el-form-item>
        <el-form-item label="费用" prop="price">
          <el-input-number v-model="form.price" :min="0" :precision="2" :step="10" />
        </el-form-item>
      </el-form>
      <div slot="footer">
        <el-button @click="dialogVisible = false">取 消</el-button>
        <el-button type="primary" :loading="saving" @click="handleSave">保 存</el-button>
      </div>
    </el-dialog>
  </div>
</template>

<script>
import { findAllChecks, addCheck, modifyCheck, deleteCheck } from '@/api/admin'

const emptyForm = () => ({ checkId: null, checkName: '', price: 0 })

export default {
  name: 'AdminCheck',
  data() {
    return {
      loading: false,
      saving: false,
      list: [],
      total: 0,
      query: { page: 1, size: 10, keyword: '' },
      dialogVisible: false,
      form: emptyForm(),
      rules: {
        checkName: [{ required: true, message: '请输入检查项目名称', trigger: 'blur' }],
        price: [{ required: true, message: '请输入费用', trigger: 'blur' }]
      }
    }
  },
  created() {
    this.load()
  },
  methods: {
    load() {
      this.loading = true
      findAllChecks(this.query)
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
      this.query = { page: 1, size: 10, keyword: '' }
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
        const req = this.form.checkId ? modifyCheck(this.form) : addCheck(this.form)
        req
          .then(res => {
            this.$message.success(res.msg || '保存成功')
            this.dialogVisible = false
            this.load()
          })
          .finally(() => {
            this.saving = false
          })
      })
    },
    handleDelete(row) {
      this.$confirm(`确定删除检查项目「${row.checkName}」吗？`, '提示', { type: 'warning' })
        .then(() => deleteCheck(row.checkId))
        .then(res => {
          this.$message.success(res.msg || '删除成功')
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
