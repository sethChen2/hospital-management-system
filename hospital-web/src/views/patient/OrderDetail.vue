<template>
  <div class="page-container">
    <el-card shadow="never" v-loading="loading">
      <div slot="header">
        <el-button size="small" icon="el-icon-back" @click="$router.push('/patient/orders')">
          返回列表
        </el-button>
        <span class="title">挂号单详情 #{{ registerId }}</span>
      </div>

      <el-descriptions :column="3" border size="medium">
        <el-descriptions-item label="患者">{{ detail.patientName }}</el-descriptions-item>
        <el-descriptions-item label="医生">{{ detail.doctorName }}</el-descriptions-item>
        <el-descriptions-item label="科室">{{ detail.department }}</el-descriptions-item>
        <el-descriptions-item label="就诊日期">{{ detail.scheduleDate }}</el-descriptions-item>
        <el-descriptions-item label="时段">
          {{ detail.timePeriod === 'AM' ? '上午' : '下午' }}
        </el-descriptions-item>
        <el-descriptions-item label="挂号时间">{{ detail.appointTime }}</el-descriptions-item>
        <el-descriptions-item label="就诊状态">
          <el-tag :type="statusType(detail.registerStatus)" size="mini">
            {{ detail.registerStatus }}
          </el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="缴费状态">
          <el-tag :type="detail.payStatus === '已缴费' ? 'success' : 'warning'" size="mini">
            {{ detail.payStatus }}
          </el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="合计费用">{{ detail.totalCost }} 元</el-descriptions-item>
        <el-descriptions-item label="症状" :span="3">{{ detail.symptom || '-' }}</el-descriptions-item>
        <el-descriptions-item label="诊断" :span="3">{{ detail.diagnosis || '-' }}</el-descriptions-item>
      </el-descriptions>

      <h4 class="sec">处方明细</h4>
      <el-table :data="prescriptions" border size="mini">
        <el-table-column type="index" label="#" width="50" align="center" />
        <el-table-column prop="drugName" label="药品" min-width="160" />
        <el-table-column prop="price" label="单价" width="90" align="right" />
        <el-table-column prop="drugNum" label="数量" width="80" align="center" />
        <el-table-column prop="subtotal" label="小计" width="100" align="right" />
      </el-table>
      <el-empty v-if="prescriptions.length === 0" description="暂无处方" :image-size="60" />

      <h4 class="sec">检查项目</h4>
      <el-table :data="checks" border size="mini">
        <el-table-column type="index" label="#" width="50" align="center" />
        <el-table-column prop="checkName" label="检查项目" min-width="200" />
        <el-table-column prop="price" label="费用" width="100" align="right" />
      </el-table>
      <el-empty v-if="checks.length === 0" description="暂无检查项目" :image-size="60" />

      <div class="actions">
        <el-button
          v-if="detail.payStatus === '未缴费' && detail.totalCost > 0"
          type="primary"
          @click="handlePay"
        >
          缴纳费用（{{ detail.totalCost }} 元）
        </el-button>
        <el-button v-if="detail.registerStatus === '待就诊'" type="danger" plain @click="handleCancel">
          取消挂号
        </el-button>
        <el-button
          type="success"
          icon="el-icon-download"
          :disabled="detail.registerStatus !== '已完成'"
          @click="downloadReport"
        >
          下载就诊报告单
        </el-button>
      </div>
    </el-card>
  </div>
</template>

<script>
import {
  findOrderDetail,
  findPrescriptions,
  findChecks,
  cancelOrder,
  payOrder,
  reportPdfUrl
} from '@/api/patient'

export default {
  name: 'PatientOrderDetail',
  data() {
    return {
      loading: false,
      registerId: null,
      detail: {},
      prescriptions: [],
      checks: []
    }
  },
  created() {
    this.registerId = Number(this.$route.params.registerId)
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
      Promise.all([
        findOrderDetail(this.registerId),
        findPrescriptions(this.registerId),
        findChecks(this.registerId)
      ])
        .then(([orderRes, presRes, checkRes]) => {
          this.detail = orderRes.data || {}
          this.prescriptions = presRes.data || []
          this.checks = checkRes.data || []
        })
        .finally(() => {
          this.loading = false
        })
    },
    handlePay() {
      this.$confirm(`确认支付 ${this.detail.totalCost} 元？`, '缴费确认', { type: 'warning' })
        .then(() => payOrder(this.registerId))
        .then(res => {
          this.$message.success(res.msg || '缴费成功')
          this.load()
        })
        .catch(() => {})
    },
    handleCancel() {
      this.$confirm('确定取消该挂号吗？取消后号源将退回。', '提示', { type: 'warning' })
        .then(() => cancelOrder(this.registerId))
        .then(res => {
          this.$message.success(res.msg || '已取消挂号')
          this.load()
        })
        .catch(() => {})
    },
    downloadReport() {
      const token = this.$store.getters.token
      window.open(reportPdfUrl(this.registerId, token), '_blank')
    }
  }
}
</script>

<style scoped>
.title {
  margin-left: 12px;
  font-size: 16px;
  font-weight: 600;
}
.sec {
  margin: 18px 0 8px;
}
.actions {
  margin-top: 20px;
  text-align: right;
}
</style>
