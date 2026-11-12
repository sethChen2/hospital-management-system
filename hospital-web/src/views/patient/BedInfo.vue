<template>
  <div class="page-container">
    <el-card shadow="never">
      <div slot="header">我的住院信息</div>

      <el-table :data="list" border stripe v-loading="loading">
        <el-table-column prop="bedId" label="床位号" width="100" align="center" />
        <el-table-column label="状态" width="100" align="center">
          <template slot-scope="{ row }">
            <el-tag :type="row.bedStatus === '占用' ? 'danger' : 'success'" size="mini">
              {{ row.bedStatus }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="doctorName" label="主治医生" width="120">
          <template slot-scope="{ row }">{{ row.doctorName || '-' }}</template>
        </el-table-column>
        <el-table-column prop="department" label="科室" width="120">
          <template slot-scope="{ row }">{{ row.department || '-' }}</template>
        </el-table-column>
        <el-table-column prop="hospitalTime" label="入住时间" width="180">
          <template slot-scope="{ row }">{{ row.hospitalTime || '-' }}</template>
        </el-table-column>
        <el-table-column prop="hospitalReason" label="住院原因" min-width="200" show-overflow-tooltip>
          <template slot-scope="{ row }">{{ row.hospitalReason || '-' }}</template>
        </el-table-column>
      </el-table>

      <el-empty v-if="!loading && list.length === 0" description="暂无住院记录" />
    </el-card>
  </div>
</template>

<script>
import { findBed } from '@/api/patient'

export default {
  name: 'PatientBed',
  data() {
    return {
      loading: false,
      list: []
    }
  },
  created() {
    this.load()
  },
  methods: {
    load() {
      this.loading = true
      findBed()
        .then(res => {
          this.list = res.data || []
        })
        .finally(() => {
          this.loading = false
        })
    }
  }
}
</script>
