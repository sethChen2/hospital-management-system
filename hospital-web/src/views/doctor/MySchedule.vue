<template>
  <!-- 我的排班：不选日期时后端默认返回今天起 7 天，选了日期只查当天 -->
  <div class="page-container">
    <el-card shadow="never">
      <el-form :inline="true" class="filter-bar">
        <el-form-item label="出诊日期">
          <el-date-picker
            v-model="scheduleDate"
            type="date"
            value-format="yyyy-MM-dd"
            placeholder="默认未来 7 天"
            clearable
            @change="load"
          />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" icon="el-icon-search" @click="load">查询</el-button>
          <el-button icon="el-icon-refresh" @click="handleReset">重置</el-button>
        </el-form-item>
      </el-form>

      <el-table :data="list" border stripe v-loading="loading">
        <el-table-column prop="scheduleId" label="编号" width="80" align="center" />
        <el-table-column prop="scheduleDate" label="出诊日期" width="130" />
        <el-table-column label="时段" width="90" align="center">
          <template slot-scope="{ row }">
            <el-tag size="mini" :type="row.timePeriod === 'AM' ? '' : 'warning'">
              {{ row.timePeriod === 'AM' ? '上午' : '下午' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="maxNum" label="总号源" width="100" align="center" />
        <el-table-column label="已挂号" width="100" align="center">
          <template slot-scope="{ row }">{{ row.maxNum - row.remainNum }}</template>
        </el-table-column>
        <el-table-column label="剩余号源" width="100" align="center">
          <template slot-scope="{ row }">
            <span :class="{ 'danger-text': row.remainNum === 0 }">{{ row.remainNum }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="registerFee" label="挂号费" width="100" align="right" />
        <el-table-column label="号源使用" min-width="200">
          <template slot-scope="{ row }">
            <el-progress
              :percentage="row.maxNum ? Math.round(((row.maxNum - row.remainNum) / row.maxNum) * 100) : 0"
            />
          </template>
        </el-table-column>
      </el-table>

      <el-empty v-if="!loading && list.length === 0" description="暂无排班" />
    </el-card>
  </div>
</template>

<script>
import { findMySchedules } from '@/api/doctor'

export default {
  name: 'DoctorSchedule',
  data() {
    return {
      loading: false,
      scheduleDate: '',
      list: []
    }
  },
  created() {
    this.load()
  },
  methods: {
    /** 日期为空就不传参数，交给后端按默认 7 天查询 */
    load() {
      this.loading = true
      const params = {}
      if (this.scheduleDate) params.scheduleDate = this.scheduleDate
      findMySchedules(params)
        .then(res => {
          this.list = res.data || []
        })
        .finally(() => {
          this.loading = false
        })
    },
    /** 清空日期后重新走默认查询 */
    handleReset() {
      this.scheduleDate = ''
      this.load()
    }
  }
}
</script>

<style scoped>
.danger-text {
  color: #f56c6c;
  font-weight: bold;
}
</style>
