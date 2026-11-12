<template>
  <div class="page-container">
    <el-card shadow="never">
      <el-form :inline="true" class="filter-bar" :model="query">
        <el-form-item label="病床状态">
          <el-select v-model="query.bedStatus" placeholder="全部" clearable style="width: 120px">
            <el-option label="空闲" value="空闲" />
            <el-option label="占用" value="占用" />
          </el-select>
        </el-form-item>
        <el-form-item label="关键字">
          <el-input
            v-model="query.keyword"
            placeholder="患者 / 医生姓名"
            clearable
            style="width: 180px"
            @keyup.enter.native="handleSearch"
          />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" icon="el-icon-search" @click="handleSearch">查询</el-button>
          <el-button icon="el-icon-refresh" @click="handleReset">重置</el-button>
        </el-form-item>
      </el-form>

      <div class="table-toolbar">
        <el-button type="primary" icon="el-icon-plus" @click="openAdd">新增病床</el-button>
        <el-tag type="info" style="margin-left: 10px">
          空闲 {{ freeCount }} 张 / 共 {{ total }} 张
        </el-tag>
      </div>

      <el-table :data="list" border stripe v-loading="loading">
        <el-table-column prop="bedId" label="床位号" width="90" align="center" />
        <el-table-column label="状态" width="90" align="center">
          <template slot-scope="{ row }">
            <el-tag :type="row.bedStatus === '占用' ? 'danger' : 'success'" size="mini">
              {{ row.bedStatus }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="patientName" label="患者" width="100">
          <template slot-scope="{ row }">{{ row.patientName || '-' }}</template>
        </el-table-column>
        <el-table-column prop="doctorName" label="主治医生" width="100">
          <template slot-scope="{ row }">{{ row.doctorName || '-' }}</template>
        </el-table-column>
        <el-table-column prop="department" label="科室" width="100">
          <template slot-scope="{ row }">{{ row.department || '-' }}</template>
        </el-table-column>
        <el-table-column prop="hospitalTime" label="入住时间" width="170">
          <template slot-scope="{ row }">{{ row.hospitalTime || '-' }}</template>
        </el-table-column>
        <el-table-column prop="hospitalReason" label="住院原因" min-width="180" show-overflow-tooltip>
          <template slot-scope="{ row }">{{ row.hospitalReason || '-' }}</template>
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

    <el-dialog :title="form.bedId ? '编辑病床' : '新增病床'" :visible.sync="dialogVisible" width="480px">
      <el-form ref="form" :model="form" :rules="rules" label-width="90px">
        <el-form-item label="床位号" prop="bedId">
          <el-input-number v-model="form.bedId" :min="1" :disabled="!!form._edit" />
        </el-form-item>
        <el-form-item label="状态" prop="bedStatus">
          <el-select v-model="form.bedStatus" style="width: 100%">
            <el-option label="空闲" value="空闲" />
            <el-option label="占用" value="占用" />
          </el-select>
        </el-form-item>
        <el-form-item label="住院原因">
          <el-input v-model="form.hospitalReason" type="textarea" :rows="2" />
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
import { findAllBeds, findFreeBeds, addBed, modifyBed, deleteBed } from '@/api/admin'

const emptyForm = () => ({
  bedId: null,
  bedStatus: '空闲',
  hospitalReason: '',
  patientUserId: null,
  doctorId: null,
  hospitalTime: null,
  _edit: false
})

export default {
  name: 'AdminBed',
  data() {
    return {
      loading: false,
      saving: false,
      list: [],
      total: 0,
      freeCount: 0,
      query: { page: 1, size: 10, bedStatus: '', keyword: '' },
      dialogVisible: false,
      form: emptyForm(),
      rules: {
        bedId: [{ required: true, message: '请输入床位号', trigger: 'blur' }],
        bedStatus: [{ required: true, message: '请选择状态', trigger: 'change' }]
      }
    }
  },
  created() {
    this.load()
    this.loadFree()
  },
  methods: {
    load() {
      this.loading = true
      findAllBeds(this.query)
        .then(res => {
          const page = res.data || {}
          this.list = page.records || []
          this.total = page.total || 0
        })
        .finally(() => {
          this.loading = false
        })
    },
    loadFree() {
      findFreeBeds().then(res => {
        this.freeCount = (res.data || []).length
      })
    },
    handleSearch() {
      this.query.page = 1
      this.load()
    },
    handleReset() {
      this.query = { page: 1, size: 10, bedStatus: '', keyword: '' }
      this.load()
    },
    openAdd() {
      this.form = emptyForm()
      this.dialogVisible = true
      this.$nextTick(() => this.$refs.form && this.$refs.form.clearValidate())
    },
    openEdit(row) {
      this.form = Object.assign(emptyForm(), row, { _edit: true })
      this.dialogVisible = true
      this.$nextTick(() => this.$refs.form && this.$refs.form.clearValidate())
    },
    handleSave() {
      this.$refs.form.validate(valid => {
        if (!valid) return
        this.saving = true
        const payload = Object.assign({}, this.form)
        delete payload._edit
        delete payload.patientName
        delete payload.doctorName
        delete payload.department
        const req = this.form._edit ? modifyBed(payload) : addBed(payload)
        req
          .then(res => {
            this.$message.success(res.msg || '保存成功')
            this.dialogVisible = false
            this.load()
            this.loadFree()
          })
          .finally(() => {
            this.saving = false
          })
      })
    },
    handleDelete(row) {
      this.$confirm(`确定删除病床 #${row.bedId} 吗？`, '提示', { type: 'warning' })
        .then(() => deleteBed(row.bedId))
        .then(res => {
          this.$message.success(res.msg || '删除成功')
          this.load()
          this.loadFree()
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
