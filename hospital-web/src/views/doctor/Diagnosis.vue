<template>
  <!--
    接诊工作台：一个页面按顺序完成一次就诊
    患者信息 → 病历 → 处方 → 检查单 → 住院申请 → 结束就诊
    页面本身不做业务判断，状态流转和费用计算全部由后端负责
  -->
  <div class="page-container">
    <el-card shadow="never">
      <div class="page-head">
        <div>
          <el-button size="small" icon="el-icon-back" @click="$router.push('/doctor/order')">
            返回就诊列表
          </el-button>
          <span class="title">接诊工作台 · 挂号单 #{{ registerId }}</span>
        </div>
      </div>

      <!-- 患者基本信息：由挂号单上的 patientUserId 再查一次用户表 -->
      <el-descriptions :column="3" border size="medium" style="margin-top: 12px">
        <el-descriptions-item label="姓名">{{ patient.name || '-' }}</el-descriptions-item>
        <el-descriptions-item label="性别">{{ patient.gender || '-' }}</el-descriptions-item>
        <el-descriptions-item label="手机号">{{ patient.phone || '-' }}</el-descriptions-item>
        <el-descriptions-item label="身份证">{{ patient.idcard || '-' }}</el-descriptions-item>
        <el-descriptions-item label="出生日期">{{ patient.birth || '-' }}</el-descriptions-item>
        <el-descriptions-item label="邮箱">{{ patient.email || '-' }}</el-descriptions-item>
      </el-descriptions>
    </el-card>

    <!-- 病历 -->
    <el-card shadow="never" class="mt">
      <div slot="header">病历书写</div>
      <el-form label-width="90px">
        <el-form-item label="症状描述">
          <el-input v-model="symptom" type="textarea" :rows="2" disabled placeholder="由患者挂号时填写" />
        </el-form-item>
        <el-form-item label="诊断结论">
          <el-input v-model="diagnosis" type="textarea" :rows="3" placeholder="请填写诊断结论" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :loading="savingDiagnosis" @click="handleSaveDiagnosis">
            保存病历
          </el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <!-- 处方：上半部分是已开明细，下半部分是"新增行"输入区，提交后统一写库 -->
    <el-card shadow="never" class="mt">
      <div slot="header">处方明细</div>
      <el-table :data="prescriptions" border size="mini">
        <el-table-column type="index" label="#" width="50" align="center" />
        <el-table-column prop="drugName" label="药品" min-width="160" />
        <el-table-column prop="price" label="单价" width="90" align="right" />
        <el-table-column prop="drugNum" label="数量" width="80" align="center" />
        <el-table-column prop="subtotal" label="小计" width="100" align="right" />
        <el-table-column label="操作" width="80" align="center">
          <template slot-scope="{ row }">
            <el-button type="text" class="danger-text" @click="handleDeletePrescription(row)">
              删除
            </el-button>
          </template>
        </el-table-column>
      </el-table>

      <div class="add-bar" v-if="drugRows.length">
        <div v-for="(row, idx) in drugRows" :key="idx" class="drug-row">
          <el-select v-model="row.drugId" placeholder="选择药品" filterable style="width: 260px">
            <el-option
              v-for="d in drugList"
              :key="d.drugId"
              :label="d.drugName + '（' + d.price + ' 元/' + d.unit + '）'"
              :value="d.drugId"
            />
          </el-select>
          <el-input-number v-model="row.drugNum" :min="1" :step="1" />
          <el-button type="text" class="danger-text" @click="drugRows.splice(idx, 1)">移除</el-button>
        </div>
      </div>

      <div class="btns">
        <el-button size="small" icon="el-icon-plus" @click="addDrugRow">添加一行</el-button>
        <el-button
          type="primary"
          size="small"
          :loading="savingPrescription"
          @click="handleSubmitPrescriptions"
        >
          提交处方
        </el-button>
      </div>
    </el-card>

    <!-- 检查单：检查项目没有数量概念，多选后一次性提交 -->
    <el-card shadow="never" class="mt">
      <div slot="header">检查项目</div>
      <el-table :data="checks" border size="mini">
        <el-table-column type="index" label="#" width="50" align="center" />
        <el-table-column prop="checkName" label="检查项目" min-width="200" />
        <el-table-column prop="price" label="费用" width="100" align="right" />
        <el-table-column label="操作" width="80" align="center">
          <template slot-scope="{ row }">
            <span class="muted">—</span>
          </template>
        </el-table-column>
      </el-table>

      <div class="btns">
        <el-select
          v-model="selectedCheckIds"
          multiple
          filterable
          placeholder="选择要开具的检查项目"
          style="width: 420px"
        >
          <el-option
            v-for="c in checkList"
            :key="c.checkId"
            :label="c.checkName + '（' + c.price + ' 元）'"
            :value="c.checkId"
          />
        </el-select>
        <el-button
          type="primary"
          size="small"
          :loading="savingCheck"
          @click="handleSubmitChecks"
        >
          开具检查单
        </el-button>
      </div>
    </el-card>

    <!-- 住院申请 + 底部结束就诊：结束是终态操作，后端会锁定这张单子 -->
    <el-card shadow="never" class="mt">
      <div slot="header">住院申请</div>
      <el-form label-width="90px">
        <el-form-item label="住院原因">
          <el-input v-model="hospitalReason" type="textarea" :rows="2" placeholder="填写病情说明 / 住院原因" />
        </el-form-item>
        <el-form-item>
          <el-button type="warning" :loading="savingBed" @click="handleApplyBed">
            申请住院（自动分配空闲病床）
          </el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <div class="footer-actions">
      <el-button type="success" size="medium" :loading="finishing" @click="handleFinish">
        结束就诊
      </el-button>
    </div>
  </div>
</template>

<script>
import {
  findPatient,
  findOrder,
  submitDiagnosis,
  addPrescriptions,
  findPrescriptions,
  deletePrescription,
  addRegisterChecks,
  findRegisterChecks,
  applyBed,
  finishRegister,
  findAllDrugList,
  findAllCheckList
} from '@/api/doctor'

export default {
  name: 'DoctorDiagnosis',
  data() {
    return {
      registerId: null,
      patientUserId: null,
      order: {},
      patient: {},
      symptom: '',
      diagnosis: '',
      savingDiagnosis: false,

      prescriptions: [],
      drugList: [],
      drugRows: [],
      savingPrescription: false,

      checks: [],
      checkList: [],
      selectedCheckIds: [],
      savingCheck: false,

      hospitalReason: '',
      savingBed: false,

      finishing: false
    }
  },
  created() {
    // 路由参数 /doctor/diagnosis/:registerId 是整页的数据入口
    this.registerId = Number(this.$route.params.registerId)
    // 进页面就并发拉取四份数据：单子详情、已开处方、已开检查、下拉字典
    this.loadOrder()
    this.loadPrescriptions()
    this.loadChecks()
    this.loadDicts()
  },
  methods: {
    /** 单条挂号单详情：回填症状、已保存的诊断，并用 patientUserId 去查患者信息 */
    loadOrder() {
      findOrder(this.registerId).then(res => {
        const order = res.data || {}
        this.order = order
        this.patientUserId = order.patientUserId
        this.symptom = order.symptom || ''
        this.diagnosis = order.diagnosis || ''
        if (this.patientUserId) {
          this.loadPatient()
        }
      })
    },
    /** 患者基本信息，只用于页面顶部展示 */
    loadPatient() {
      findPatient(this.patientUserId).then(res => {
        this.patient = res.data || {}
      })
    },
    /** 药品 / 检查项目下拉字典，开单时选择用 */
    loadDicts() {
      findAllDrugList().then(res => {
        this.drugList = res.data || []
      })
      findAllCheckList().then(res => {
        this.checkList = res.data || []
      })
    },
    /** 回显这张单子已开过的处方 */
    loadPrescriptions() {
      findPrescriptions(this.registerId).then(res => {
        this.prescriptions = res.data || []
      })
    },
    /** 回显这张单子已开过的检查项目 */
    loadChecks() {
      findRegisterChecks(this.registerId).then(res => {
        this.checks = res.data || []
      })
    },
    /** 保存病历：前端只拦住空内容，状态是否允许修改由后端 requireTreatable 判断 */
    handleSaveDiagnosis() {
      if (!this.diagnosis.trim()) {
        this.$message.warning('请填写诊断结论')
        return
      }
      this.savingDiagnosis = true
      submitDiagnosis({ registerId: this.registerId, diagnosis: this.diagnosis })
        .then(res => {
          this.$message.success(res.msg || '病历已保存')
        })
        .finally(() => {
          this.savingDiagnosis = false
        })
    },
    /** 在"新增行"区域加一条空的药品行 */
    addDrugRow() {
      this.drugRows.push({ drugId: null, drugNum: 1 })
    },
    /** 提交处方：逐行校验是否选了药品，成功后清空输入区并重新拉取明细 */
    handleSubmitPrescriptions() {
      if (!this.drugRows.length) {
        this.$message.warning('请至少添加一行处方')
        return
      }
      const invalid = this.drugRows.some(r => !r.drugId)
      if (invalid) {
        this.$message.warning('请为每一行选择药品')
        return
      }
      this.savingPrescription = true
      addPrescriptions({
        registerId: this.registerId,
        prescriptions: this.drugRows.map(r => ({ drugId: r.drugId, drugNum: r.drugNum }))
      })
        .then(res => {
          this.$message.success(res.msg || '处方已提交')
          this.drugRows = []
          this.loadPrescriptions()
        })
        .finally(() => {
          this.savingPrescription = false
        })
    },
    /** 删除一行处方：二次确认后提交，总费用由后端重算，前端只需刷新明细 */
    handleDeletePrescription(row) {
      this.$confirm(`确定删除「${row.drugName}」这行处方吗？`, '提示', { type: 'warning' })
        .then(() => deletePrescription(row.prescriptionId))
        .then(res => {
          this.$message.success(res.msg || '已删除')
          this.loadPrescriptions()
        })
        .catch(() => {})
    },
    /** 开具检查单：多选项一次性提交，成功后清空选择并刷新列表 */
    handleSubmitChecks() {
      if (!this.selectedCheckIds.length) {
        this.$message.warning('请选择检查项目')
        return
      }
      this.savingCheck = true
      addRegisterChecks({ registerId: this.registerId, checkIds: this.selectedCheckIds })
        .then(res => {
          this.$message.success(res.msg || '检查单已提交')
          this.selectedCheckIds = []
          this.loadChecks()
        })
        .finally(() => {
          this.savingCheck = false
        })
    },
    /** 申请住院：只需填写住院原因，具体分配哪张床由后端自动完成 */
    handleApplyBed() {
      if (!this.hospitalReason.trim()) {
        this.$message.warning('请填写住院原因')
        return
      }
      this.savingBed = true
      applyBed({ registerId: this.registerId, hospitalReason: this.hospitalReason })
        .then(res => {
          this.$message.success(res.msg || '住院申请已提交')
        })
        .finally(() => {
          this.savingBed = false
        })
    },
    /** 结束就诊：二次确认后调用结束接口，成功即回到列表页（此后单子不可再改） */
    handleFinish() {
      this.$confirm('确定结束本次就诊吗？结束后将生成费用合计。', '提示', { type: 'warning' })
        .then(() => {
          this.finishing = true
          return finishRegister(this.registerId)
        })
        .then(res => {
          this.$message.success(res.msg || '就诊已结束')
          this.$router.push('/doctor/order')
        })
        .catch(() => {})
        .finally(() => {
          this.finishing = false
        })
    }
  }
}
</script>

<style scoped>
.page-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
}
.title {
  margin-left: 12px;
  font-size: 16px;
  font-weight: 600;
}
.mt {
  margin-top: 12px;
}
.add-bar {
  margin-top: 12px;
}
.drug-row {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 8px;
}
.btns {
  margin-top: 12px;
  display: flex;
  align-items: center;
  gap: 10px;
}
.footer-actions {
  margin-top: 16px;
  text-align: right;
}
.danger-text {
  color: #f56c6c;
}
.muted {
  color: #c0c4cc;
}
</style>
