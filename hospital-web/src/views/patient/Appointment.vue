<template>
  <div class="page-container">
    <el-card shadow="never">
      <el-steps :active="activeStep" finish-status="success" simple style="margin-bottom: 20px">
        <el-step title="选择科室" icon="el-icon-office-building" />
        <el-step title="选择医生" icon="el-icon-user" />
        <el-step title="选择号源并提交" icon="el-icon-date" />
      </el-steps>

      <!-- 第一步：科室 -->
      <div v-show="activeStep === 0" v-loading="loading">
        <div class="step-title">请选择就诊科室</div>
        <div class="dept-grid">
          <div
            v-for="d in departments"
            :key="d"
            class="dept-card"
            :class="{ active: form.department === d }"
            @click="chooseDepartment(d)"
          >
            <i class="el-icon-office-building"></i>
            <span>{{ d }}</span>
          </div>
        </div>
        <el-empty v-if="!loading && departments.length === 0" description="暂无可预约科室" />
      </div>

      <!-- 第二步：医生 -->
      <div v-show="activeStep === 1" v-loading="loading">
        <div class="step-title">
          {{ form.department }} · 请选择医生
          <el-button type="text" icon="el-icon-back" @click="activeStep = 0">返回科室</el-button>
        </div>
        <el-table :data="doctors" border stripe>
          <el-table-column prop="name" label="姓名" width="110" />
          <el-table-column prop="gender" label="性别" width="70" align="center" />
          <el-table-column prop="position" label="职称" width="130" />
          <el-table-column prop="department" label="科室" width="120" />
          <el-table-column prop="registerFee" label="挂号费" width="100" align="right" />
          <el-table-column prop="phone" label="联系电话" min-width="140" />
          <el-table-column label="操作" width="110" fixed="right">
            <template slot-scope="{ row }">
              <el-button type="text" @click="chooseDoctor(row)">选择医生</el-button>
            </template>
          </el-table-column>
        </el-table>
        <el-empty v-if="!loading && doctors.length === 0" description="该科室暂无出诊医生" />
      </div>

      <!-- 第三步：排班 -->
      <div v-show="activeStep === 2" v-loading="loading">
        <div class="step-title">
          {{ form.doctorName }} · 请选择号源
          <el-button type="text" icon="el-icon-back" @click="activeStep = 1">返回医生</el-button>
        </div>

        <el-table :data="schedules" border stripe>
          <el-table-column prop="scheduleDate" label="出诊日期" width="130" />
          <el-table-column label="时段" width="90" align="center">
            <template slot-scope="{ row }">
              <el-tag size="mini" :type="row.timePeriod === 'AM' ? '' : 'warning'">
                {{ row.timePeriod === 'AM' ? '上午' : '下午' }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="registerFee" label="挂号费" width="100" align="right" />
          <el-table-column prop="remainNum" label="剩余号源" width="100" align="center">
            <template slot-scope="{ row }">
              <el-tag :type="row.remainNum > 0 ? 'success' : 'danger'" size="mini">
                {{ row.remainNum }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column label="操作" width="110" fixed="right">
            <template slot-scope="{ row }">
              <el-button
                type="primary"
                size="mini"
                :disabled="row.remainNum <= 0"
                @click="chooseSchedule(row)"
              >
                {{ form.scheduleId === row.scheduleId ? '已选' : '选号' }}
              </el-button>
            </template>
          </el-table-column>
        </el-table>
        <el-empty v-if="!loading && schedules.length === 0" description="该医生未来 7 天暂无排班" />

        <el-form label-width="90px" style="margin-top: 16px; max-width: 620px">
          <el-form-item label="症状描述">
            <el-input
              v-model="form.symptom"
              type="textarea"
              :rows="3"
              placeholder="请简要描述病情，便于医生接诊"
            />
          </el-form-item>
          <el-form-item>
            <el-button
              type="primary"
              :loading="submitting"
              :disabled="!form.scheduleId"
              @click="handleSubmit"
            >
              确认挂号
            </el-button>
            <span v-if="form.scheduleId" class="picked">
              已选：{{ form.scheduleDate }} {{ form.timePeriod === 'AM' ? '上午' : '下午' }}
              · {{ form.registerFee }} 元
            </span>
          </el-form-item>
        </el-form>
      </div>
    </el-card>
  </div>
</template>

<script>
import {
  findDepartments,
  findDoctorsByDepartment,
  findSchedulesByDoctor,
  addOrder
} from '@/api/patient'

export default {
  name: 'PatientAppointment',
  data() {
    return {
      activeStep: 0,
      loading: false,
      submitting: false,
      departments: [],
      doctors: [],
      schedules: [],
      form: {
        department: '',
        doctorId: null,
        doctorName: '',
        scheduleId: null,
        scheduleDate: '',
        timePeriod: '',
        registerFee: '',
        symptom: ''
      }
    }
  },
  created() {
    this.loadDepartments()
  },
  methods: {
    loadDepartments() {
      this.loading = true
      findDepartments()
        .then(res => {
          this.departments = res.data || []
        })
        .finally(() => {
          this.loading = false
        })
    },
    chooseDepartment(d) {
      this.form.department = d
      this.loadDoctors(d)
      this.activeStep = 1
    },
    loadDoctors(department) {
      this.loading = true
      findDoctorsByDepartment({ department })
        .then(res => {
          this.doctors = res.data || []
        })
        .finally(() => {
          this.loading = false
        })
    },
    chooseDoctor(row) {
      this.form.doctorId = row.doctorId
      this.form.doctorName = row.name
      this.form.registerFee = row.registerFee
      this.form.scheduleId = null
      this.loadSchedules(row.doctorId)
      this.activeStep = 2
    },
    loadSchedules(doctorId) {
      this.loading = true
      findSchedulesByDoctor({ doctorId })
        .then(res => {
          this.schedules = res.data || []
        })
        .finally(() => {
          this.loading = false
        })
    },
    chooseSchedule(row) {
      this.form.scheduleId = row.scheduleId
      this.form.scheduleDate = row.scheduleDate
      this.form.timePeriod = row.timePeriod
      this.form.registerFee = row.registerFee
    },
    handleSubmit() {
      if (!this.form.scheduleId) {
        this.$message.warning('请先选择号源')
        return
      }
      this.submitting = true
      addOrder({ scheduleId: this.form.scheduleId, symptom: this.form.symptom })
        .then(res => {
          this.$message.success(res.msg || '挂号成功')
          this.$router.push('/patient/orders')
        })
        .finally(() => {
          this.submitting = false
        })
    }
  }
}
</script>

<style scoped>
.step-title {
  font-size: 15px;
  font-weight: 600;
  margin-bottom: 14px;
  display: flex;
  align-items: center;
  gap: 10px;
}
.dept-grid {
  display: flex;
  flex-wrap: wrap;
  gap: 14px;
}
.dept-card {
  width: 150px;
  height: 84px;
  border: 1px solid #dcdfe6;
  border-radius: 6px;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  transition: all 0.2s;
  color: #606266;
}
.dept-card i {
  font-size: 22px;
  margin-bottom: 6px;
}
.dept-card:hover,
.dept-card.active {
  border-color: #409eff;
  color: #409eff;
  box-shadow: 0 0 8px rgba(64, 158, 255, 0.25);
}
.picked {
  margin-left: 14px;
  color: #67c23a;
  font-size: 13px;
}
</style>
