<template>
  <!-- 就诊列表：分页展示当前医生名下的挂号单，可按关键字 / 就诊状态 / 缴费状态筛选 -->
  <div class="page-container">
    <el-card shadow="never">
      <el-form :inline="true" class="filter-bar" :model="query">
        <el-form-item label="关键字">
          <el-input
            v-model="query.keyword"
            placeholder="患者姓名"
            clearable
            style="width: 180px"
            @keyup.enter.native="handleSearch"
          />
        </el-form-item>
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
        </el-form-item>
      </el-form>

      <el-table :data="list" border stripe v-loading="loading">
        <el-table-column prop="registerId" label="单号" width="70" align="center" />
        <el-table-column prop="patientName" label="患者" width="100" />
        <el-table-column prop="patientGender" label="性别" width="60" align="center" />
        <el-table-column prop="scheduleDate" label="就诊日期" width="110" />
        <el-table-column label="时段" width="70" align="center">
          <template slot-scope="{ row }">{{ row.timePeriod === 'AM' ? '上午' : '下午' }}</template>
        </el-table-column>
        <el-table-column prop="appointTime" label="挂号时间" width="160" />
        <el-table-column prop="symptom" label="症状描述" min-width="160" show-overflow-tooltip />
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
        <!-- 已取消的单子不能进工作台；已完成的单子按钮文案变为「查看」 -->
        <el-table-column label="操作" width="150" fixed="right">
          <template slot-scope="{ row }">
            <el-button
              type="text"
              :disabled="row.registerStatus === '已取消'"
              @click="openDiagnosis(row)"
            >
              {{ row.registerStatus === '已完成' ? '查看' : '接诊' }}
            </el-button>
            <el-button type="text" @click="openHistory(row)">既往</el-button>
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
import { findMyOrders } from '@/api/doctor'

export default {
  name: 'DoctorOrder',
  data() {
    return {
      loading: false,
      list: [],
      total: 0,
      statusList: ['待就诊', '就诊中', '已完成', '已取消'],
      query: { page: 1, size: 10, keyword: '', registerStatus: '', payStatus: '' }
    }
  },
  created() {
    this.load()
  },
  methods: {
    /** 把就诊状态映射成 el-tag 的颜色类型：已完成绿、已取消灰、就诊中黄、待就诊蓝 */
    statusType(status) {
      if (status === '已完成') return 'success'
      if (status === '已取消') return 'info'
      if (status === '就诊中') return 'warning'
      return 'primary'
    },
    /** 分页查询：后端返回 MyBatis-Plus 的分页对象，records 是当前页数据，total 是总条数 */
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
      this.query = { page: 1, size: 10, keyword: '', registerStatus: '', payStatus: '' }
      this.load()
    },
    /** 带着挂号单 ID 跳到接诊工作台 */
    openDiagnosis(row) {
      this.$router.push('/doctor/diagnosis/' + row.registerId)
    },
    /** 跳到既往就诊页，把患者 ID 放在路由 query 里带过去 */
    openHistory(row) {
      this.$router.push({ path: '/doctor/history', query: { patientUserId: row.patientUserId } })
    }
  }
}
</script>
