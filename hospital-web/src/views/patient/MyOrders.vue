<template>
  <div class="page-container">
    <el-card shadow="never">
      <el-form :inline="true" class="filter-bar" :model="query">
        <el-form-item label="就诊状态">
          <el-select v-model="query.registerStatus" placeholder="全部" clearable style="width: 120px">
            <el-option v-for="s in statusList" :key="s" :label="s" :value="s" />
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
          <el-button type="success" icon="el-icon-plus" @click="$router.push('/patient/appointment')">
            去挂号
          </el-button>
        </el-form-item>
      </el-form>

      <el-table :data="list" border stripe v-loading="loading">
        <el-table-column prop="registerId" label="单号" width="70" align="center" />
        <el-table-column prop="doctorName" label="医生" width="100" />
        <el-table-column prop="department" label="科室" width="100" />
        <el-table-column prop="scheduleDate" label="就诊日期" width="120" />
        <el-table-column label="时段" width="80" align="center">
          <template slot-scope="{ row }">{{ row.timePeriod === 'AM' ? '上午' : '下午' }}</template>
        </el-table-column>
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
        <el-table-column label="操作" width="180" fixed="right">
          <template slot-scope="{ row }">
            <el-button type="text" @click="goDetail(row)">详情</el-button>
            <el-button
              v-if="row.registerStatus === '待就诊'"
              type="text"
              class="danger-text"
              @click="handleCancel(row)"
            >
              取消
            </el-button>
            <el-button
              v-if="row.payStatus === '未缴费' && row.totalCost > 0"
              type="text"
              @click="handlePay(row)"
            >
              缴费
            </el-button>
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
  </div>
</template>

<script>
import { findMyOrders, cancelOrder, payOrder } from '@/api/patient'

export default {
  name: 'PatientOrders',
  data() {
    return {
      loading: false,
      list: [],
      total: 0,
      statusList: ['待就诊', '就诊中', '已完成', '已取消'],
      query: { page: 1, size: 10, registerStatus: '', payStatus: '' }
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
      findMyOrders(this.query)
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
      this.query = { page: 1, size: 10, registerStatus: '', payStatus: '' }
      this.load()
    },
    goDetail(row) {
      this.$router.push('/patient/order/' + row.registerId)
    },
    handleCancel(row) {
      this.$confirm(`确定取消挂号单 #${row.registerId} 吗？取消后号源将退回。`, '提示', {
        type: 'warning'
      })
        .then(() => cancelOrder(row.registerId))
        .then(res => {
          this.$message.success(res.msg || '已取消挂号')
          this.load()
        })
        .catch(() => {})
    },
    handlePay(row) {
      this.$confirm(`确认支付 ${row.totalCost} 元？`, '缴费确认', { type: 'warning' })
        .then(() => payOrder(row.registerId))
        .then(res => {
          this.$message.success(res.msg || '缴费成功')
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
