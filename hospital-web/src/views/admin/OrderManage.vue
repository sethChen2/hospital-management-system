<template>
  <div class="page-container">
    <el-card shadow="never">
      <el-form :inline="true" class="filter-bar" :model="query">
        <el-form-item label="关键字">
          <el-input
            v-model="query.keyword"
            placeholder="患者 / 医生姓名"
            clearable
            style="width: 180px"
            @keyup.enter.native="handleSearch"
          />
        </el-form-item>
        <el-form-item label="就诊状态">
          <el-select v-model="query.registerStatus" placeholder="全部" clearable style="width: 120px">
            <el-option v-for="s in registerStatusList" :key="s" :label="s" :value="s" />
          </el-select>
        </el-form-item>
        <el-form-item label="缴费状态">
          <el-select v-model="query.payStatus" placeholder="全部" clearable style="width: 120px">
            <el-option label="未缴费" value="未缴费" />
            <el-option label="已缴费" value="已缴费" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" icon="el-icon-search" @click="handleSearch">查询</el-button>
          <el-button icon="el-icon-refresh" @click="handleReset">重置</el-button>
        </el-form-item>
      </el-form>

      <el-table :data="list" border stripe v-loading="loading">
        <el-table-column prop="registerId" label="单号" width="70" align="center" />
        <el-table-column prop="patientName" label="患者" width="90" />
        <el-table-column prop="doctorName" label="医生" width="90" />
        <el-table-column prop="department" label="科室" width="90" />
        <el-table-column prop="scheduleDate" label="就诊日期" width="110" />
        <el-table-column prop="timePeriod" label="时段" width="70" align="center" />
        <el-table-column prop="appointTime" label="挂号时间" width="160" />
        <el-table-column label="状态" width="90" align="center">
          <template slot-scope="{ row }">
            <el-tag :type="statusType(row.registerStatus)" size="mini">
              {{ row.registerStatus }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="缴费" width="80" align="center">
          <template slot-scope="{ row }">
            <el-tag :type="row.payStatus === '已缴费' ? 'success' : 'warning'" size="mini">
              {{ row.payStatus }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="totalCost" label="费用" width="90" align="right" />
        <el-table-column label="操作" width="130" fixed="right">
          <template slot-scope="{ row }">
            <el-button type="text" @click="openDetail(row)">详情</el-button>
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

    <!-- 详情 -->
    <el-dialog title="挂号单详情" :visible.sync="detailVisible" width="760px">
      <el-descriptions :column="2" border size="medium">
        <el-descriptions-item label="单号">{{ detail.registerId }}</el-descriptions-item>
        <el-descriptions-item label="患者">{{ detail.patientName }}</el-descriptions-item>
        <el-descriptions-item label="医生">{{ detail.doctorName }}</el-descriptions-item>
        <el-descriptions-item label="科室">{{ detail.department }}</el-descriptions-item>
        <el-descriptions-item label="就诊日期">{{ detail.scheduleDate }}</el-descriptions-item>
        <el-descriptions-item label="时段">{{ detail.timePeriod }}</el-descriptions-item>
        <el-descriptions-item label="挂号时间">{{ detail.appointTime }}</el-descriptions-item>
        <el-descriptions-item label="结束时间">{{ detail.endTime || '-' }}</el-descriptions-item>
        <el-descriptions-item label="就诊状态">{{ detail.registerStatus }}</el-descriptions-item>
        <el-descriptions-item label="缴费状态">{{ detail.payStatus }}</el-descriptions-item>
        <el-descriptions-item label="症状" :span="2">{{ detail.symptom || '-' }}</el-descriptions-item>
        <el-descriptions-item label="诊断" :span="2">{{ detail.diagnosis || '-' }}</el-descriptions-item>
        <el-descriptions-item label="合计费用" :span="2">{{ detail.totalCost }} 元</el-descriptions-item>
      </el-descriptions>

      <h4 style="margin: 16px 0 8px">处方明细</h4>
      <el-table :data="detail.prescriptionList || []" border size="mini">
        <el-table-column prop="drugName" label="药品" min-width="140" />
        <el-table-column prop="price" label="单价" width="90" align="right" />
        <el-table-column prop="drugNum" label="数量" width="80" align="center" />
        <el-table-column prop="subtotal" label="小计" width="100" align="right" />
      </el-table>

      <h4 style="margin: 16px 0 8px">检查项目</h4>
      <el-table :data="detail.checkList || []" border size="mini">
        <el-table-column prop="checkName" label="检查项目" min-width="180" />
        <el-table-column prop="price" label="费用" width="100" align="right" />
      </el-table>
    </el-dialog>
  </div>
</template>

<script>
import { findAllOrders, findOrder, deleteOrder } from '@/api/admin'

export default {
  name: 'AdminOrder',
  data() {
    return {
      loading: false,
      list: [],
      total: 0,
      registerStatusList: ['待就诊', '就诊中', '已完成', '已取消'],
      query: { page: 1, size: 10, keyword: '', registerStatus: '', payStatus: '' },
      detailVisible: false,
      detail: {}
    }
  },
  created() {
    this.load()
  },
  methods: {
    statusType(status) {
      if (status === '已完成') return 'success'
      if (status === '已取消') return 'info'
      if (status === '就诊中') return 'warning'
      return 'primary'
    },
    load() {
      this.loading = true
      findAllOrders(this.query)
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
      this.query = { page: 1, size: 10, keyword: '', registerStatus: '', payStatus: '' }
      this.load()
    },
    openDetail(row) {
      findOrder(row.registerId).then(res => {
        this.detail = res.data || {}
        this.detailVisible = true
      })
    },
    handleDelete(row) {
      this.$confirm(`确定删除挂号单 #${row.registerId} 吗？`, '提示', { type: 'warning' })
        .then(() => deleteOrder(row.registerId))
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
