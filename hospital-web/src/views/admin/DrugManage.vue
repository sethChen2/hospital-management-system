<template>
  <div class="page-container">
    <el-card shadow="never">
      <el-form :inline="true" class="filter-bar" :model="query">
        <el-form-item label="关键字">
          <el-input
            v-model="query.keyword"
            placeholder="药品名称"
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
        <el-button type="primary" icon="el-icon-plus" @click="openAdd">新增药品</el-button>
      </div>

      <el-table :data="list" border stripe v-loading="loading">
        <el-table-column prop="drugId" label="编号" width="80" align="center" />
        <el-table-column prop="drugName" label="药品名称" min-width="180" />
        <el-table-column prop="price" label="单价（元）" width="130" align="right" />
        <el-table-column prop="unit" label="单位" width="100" align="center" />
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

    <el-dialog :title="form.drugId ? '编辑药品' : '新增药品'" :visible.sync="dialogVisible" width="440px">
      <el-form ref="form" :model="form" :rules="rules" label-width="90px">
        <el-form-item label="药品名称" prop="drugName">
          <el-input v-model="form.drugName" placeholder="如：阿莫西林胶囊" />
        </el-form-item>
        <el-form-item label="单价" prop="price">
          <el-input-number v-model="form.price" :min="0" :precision="2" :step="1" />
        </el-form-item>
        <el-form-item label="单位" prop="unit">
          <el-input v-model="form.unit" placeholder="如：盒 / 瓶 / 支" />
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
import { findAllDrugs, addDrug, modifyDrug, deleteDrug } from '@/api/admin'

const emptyForm = () => ({ drugId: null, drugName: '', price: 0, unit: '' })

export default {
  name: 'AdminDrug',
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
        drugName: [{ required: true, message: '请输入药品名称', trigger: 'blur' }],
        price: [{ required: true, message: '请输入单价', trigger: 'blur' }],
        unit: [{ required: true, message: '请输入单位', trigger: 'blur' }]
      }
    }
  },
  created() {
    this.load()
  },
  methods: {
    load() {
      this.loading = true
      findAllDrugs(this.query)
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
        const req = this.form.drugId ? modifyDrug(this.form) : addDrug(this.form)
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
      this.$confirm(`确定删除药品「${row.drugName}」吗？`, '提示', { type: 'warning' })
        .then(() => deleteDrug(row.drugId))
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
