<template>
  <div class="page-container">
    <el-card shadow="never">
      <el-form :inline="true" class="filter-bar" :model="query">
        <el-form-item label="医生">
          <el-select
            v-model="query.doctorId"
            placeholder="全部"
            clearable
            filterable
            style="width: 170px"
          >
            <el-option
              v-for="d in doctors"
              :key="d.doctorId"
              :label="d.name + '（' + d.department + '）'"
              :value="d.doctorId"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="科室">
          <el-select v-model="query.department" placeholder="全部" clearable style="width: 130px">
            <el-option v-for="d in departments" :key="d" :label="d" :value="d" />
          </el-select>
        </el-form-item>
        <el-form-item label="日期">
          <el-date-picker
            v-model="dateRange"
            type="daterange"
            value-format="yyyy-MM-dd"
            range-separator="至"
            start-placeholder="开始"
            end-placeholder="结束"
            style="width: 240px"
          />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" icon="el-icon-search" @click="handleSearch">查询</el-button>
          <el-button icon="el-icon-refresh" @click="handleReset">重置</el-button>
        </el-form-item>
      </el-form>

      <div class="table-toolbar">
        <el-button type="primary" icon="el-icon-plus" @click="openAdd">新增排班</el-button>
      </div>

      <el-table :data="list" border stripe v-loading="loading">
        <el-table-column prop="scheduleId" label="编号" width="70" align="center" />
        <el-table-column prop="doctorName" label="医生" width="100" />
        <el-table-column prop="department" label="科室" width="100" />
        <el-table-column prop="position" label="职称" width="110" />
        <el-table-column prop="scheduleDate" label="出诊日期" width="120" />
        <el-table-column label="时段" width="80" align="center">
          <template slot-scope="{ row }">
            <el-tag size="mini" :type="row.timePeriod === 'AM' ? '' : 'warning'">
              {{ row.timePeriod === 'AM' ? '上午' : '下午' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="maxNum" label="总号源" width="90" align="center" />
        <el-table-column label="剩余号源" width="100" align="center">
          <template slot-scope="{ row }">
            <span :class="{ 'danger-text': row.remainNum === 0 }">{{ row.remainNum }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="registerFee" label="挂号费" width="90" align="right" />
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

    <el-dialog :title="form.scheduleId ? '编辑排班' : '新增排班'" :visible.sync="dialogVisible" width="480px">
      <el-form ref="form" :model="form" :rules="rules" label-width="90px">
        <el-form-item label="医生" prop="doctorId">
          <el-select
            v-model="form.doctorId"
            placeholder="请选择医生"
            filterable
            style="width: 100%"
          >
            <el-option
              v-for="d in doctors"
              :key="d.doctorId"
              :label="d.name + '（' + d.department + ' · ' + (d.position || '医师') + '）'"
              :value="d.doctorId"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="出诊日期" prop="scheduleDate">
          <el-date-picker
            v-model="form.scheduleDate"
            type="date"
            value-format="yyyy-MM-dd"
            style="width: 100%"
          />
        </el-form-item>
        <el-form-item label="时段" prop="timePeriod">
          <el-radio-group v-model="form.timePeriod">
            <el-radio label="AM">上午</el-radio>
            <el-radio label="PM">下午</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="总号源" prop="maxNum">
          <el-input-number v-model="form.maxNum" :min="1" :step="1" />
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
import {
  findAllSchedules,
  addSchedule,
  modifySchedule,
  deleteSchedule,
  findAllDoctors,
  findDepartments
} from '@/api/admin'

const emptyForm = () => ({
  scheduleId: null,
  doctorId: null,
  scheduleDate: '',
  timePeriod: 'AM',
  maxNum: 20
})

export default {
  name: 'AdminSchedule',
  data() {
    return {
      loading: false,
      saving: false,
      list: [],
      total: 0,
      doctors: [],
      departments: [],
      dateRange: [],
      query: { page: 1, size: 10, doctorId: null, department: '', startDate: '', endDate: '' },
      dialogVisible: false,
      form: emptyForm(),
      rules: {
        doctorId: [{ required: true, message: '请选择医生', trigger: 'change' }],
        scheduleDate: [{ required: true, message: '请选择出诊日期', trigger: 'change' }],
        timePeriod: [{ required: true, message: '请选择时段', trigger: 'change' }],
        maxNum: [{ required: true, message: '请输入总号源', trigger: 'blur' }]
      }
    }
  },
  created() {
    this.load()
    this.loadDoctors()
    this.loadDepartments()
  },
  methods: {
    load() {
      this.loading = true
      const params = Object.assign({}, this.query)
      params.startDate = this.dateRange && this.dateRange[0] ? this.dateRange[0] : ''
      params.endDate = this.dateRange && this.dateRange[1] ? this.dateRange[1] : ''
      findAllSchedules(params)
        .then(res => {
          const page = res.data || {}
          this.list = page.records || []
          this.total = page.total || 0
        })
        .finally(() => {
          this.loading = false
        })
    },
    loadDoctors() {
      findAllDoctors({ page: 1, size: 500 }).then(res => {
        this.doctors = (res.data && res.data.records) || []
      })
    },
    loadDepartments() {
      findDepartments().then(res => {
        this.departments = res.data || []
      })
    },
    handleSearch() {
      this.query.page = 1
      this.load()
    },
    handleReset() {
      this.query = { page: 1, size: 10, doctorId: null, department: '', startDate: '', endDate: '' }
      this.dateRange = []
      this.load()
    },
    openAdd() {
      this.form = emptyForm()
      this.dialogVisible = true
      this.$nextTick(() => this.$refs.form && this.$refs.form.clearValidate())
    },
    openEdit(row) {
      this.form = {
        scheduleId: row.scheduleId,
        doctorId: row.doctorId,
        scheduleDate: row.scheduleDate,
        timePeriod: row.timePeriod,
        maxNum: row.maxNum
      }
      this.dialogVisible = true
      this.$nextTick(() => this.$refs.form && this.$refs.form.clearValidate())
    },
    handleSave() {
      this.$refs.form.validate(valid => {
        if (!valid) return
        this.saving = true
        const req = this.form.scheduleId ? modifySchedule(this.form) : addSchedule(this.form)
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
      this.$confirm(`确定删除该排班吗？（${row.scheduleDate} ${row.timePeriod === 'AM' ? '上午' : '下午'}）`, '提示', {
        type: 'warning'
      })
        .then(() => deleteSchedule(row.scheduleId))
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
