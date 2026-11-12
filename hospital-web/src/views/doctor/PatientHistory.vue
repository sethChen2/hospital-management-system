<template>
  <!-- 既往就诊：按患者 ID 查询"当前医生本人"接诊过的历史单据，点「详情」看处方与检查明细 -->
  <div class="page-container">
    <el-card shadow="never">
      <el-form :inline="true" class="filter-bar">
        <el-form-item label="患者ID">
          <el-input-number v-model="patientUserId" :min="1" placeholder="请输入患者ID" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" icon="el-icon-search" @click="load">查询</el-button>
          <el-button icon="el-icon-refresh" @click="handleReset">重置</el-button>
        </el-form-item>
      </el-form>

      <el-alert
        v-if="!patientUserId"
        title="请输入患者ID进行查询；在「就诊管理」中点击某条记录的「既往」可直接带入。"
        type="info"
        :closable="false"
        show-icon
        style="margin-bottom: 12px"
      />

      <el-table :data="list" border stripe v-loading="loading">
        <el-table-column prop="registerId" label="单号" width="70" align="center" />
        <el-table-column prop="doctorName" label="接诊医生" width="110" />
        <el-table-column prop="department" label="科室" width="100" />
        <el-table-column prop="scheduleDate" label="就诊日期" width="120" />
        <el-table-column label="状态" width="90" align="center">
          <template slot-scope="{ row }">
            <el-tag :type="row.registerStatus === '已完成' ? 'success' : 'info'" size="mini">
              {{ row.registerStatus }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="diagnosis" label="诊断结论" min-width="160" show-overflow-tooltip>
          <template slot-scope="{ row }">{{ row.diagnosis || '-' }}</template>
        </el-table-column>
        <el-table-column prop="totalCost" label="费用" width="90" align="right" />
        <el-table-column label="操作" width="90" fixed="right">
          <template slot-scope="{ row }">
            <el-button type="text" @click="openDetail(row)">详情</el-button>
          </template>
        </el-table-column>
      </el-table>

      <!-- 后端该接口一次性返回全部历史，前端分页器仅用于展示总条数 -->
      <div class="pagination-wrap">
        <el-pagination
          background
          layout="total, prev, pager, next"
          :total="total"
          :current-page="1"
          :page-size="size"
        />
      </div>

      <el-empty v-if="!loading && list.length === 0" description="暂无就诊记录" />
    </el-card>

    <!-- 详情弹窗：列表行数据里已带 prescriptionList / checkList，打开时不用再发请求 -->
    <el-dialog title="就诊记录详情" :visible.sync="detailVisible" width="680px">
      <el-descriptions :column="2" border size="medium">
        <el-descriptions-item label="单号">{{ detail.registerId }}</el-descriptions-item>
        <el-descriptions-item label="就诊日期">{{ detail.scheduleDate }}</el-descriptions-item>
        <el-descriptions-item label="医生">{{ detail.doctorName }}</el-descriptions-item>
        <el-descriptions-item label="科室">{{ detail.department }}</el-descriptions-item>
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
import { findHistoryRegisters } from '@/api/doctor'

export default {
  name: 'DoctorHistory',
  data() {
    return {
      loading: false,
      patientUserId: null,
      size: 10,
      list: [],
      total: 0,
      detailVisible: false,
      detail: {}
    }
  },
  created() {
    // 从就诊列表点「既往」进入时，路由 query 里会带 patientUserId，有值就直接查
    const q = this.$route.query.patientUserId
    if (q) {
      this.patientUserId = Number(q)
      this.load()
    }
  },
  methods: {
    /** 查询历史记录：接口一次返回全量，所以 total 直接取数组长度 */
    load() {
      if (!this.patientUserId) {
        this.$message.warning('请输入患者ID')
        return
      }
      this.loading = true
      findHistoryRegisters(this.patientUserId)
        .then(res => {
          this.list = res.data || []
          this.total = this.list.length
        })
        .finally(() => {
          this.loading = false
        })
    },
    handleReset() {
      this.patientUserId = null
      this.list = []
      this.total = 0
    },
    /** 打开详情弹窗：整行数据即详情，包含处方与检查明细 */
    openDetail(row) {
      this.detail = row
      this.detailVisible = true
    }
  }
}
</script>
