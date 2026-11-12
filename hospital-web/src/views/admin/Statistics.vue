<template>
  <div class="page-container">
    <!-- 概览卡片 -->
    <el-row :gutter="16">
      <el-col :span="6">
        <el-card shadow="never">
          <div class="stat-card-value">{{ data.totalRegisters }}</div>
          <div class="stat-card-label">挂号总数</div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="never">
          <div class="stat-card-value">{{ data.totalPatients }}</div>
          <div class="stat-card-label">患者总数</div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="never">
          <div class="stat-card-value">{{ data.totalDoctors }}</div>
          <div class="stat-card-label">医生总数</div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="never">
          <div class="stat-card-value">{{ data.totalIncome }} 元</div>
          <div class="stat-card-label">已缴费总收入</div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 床位使用率 -->
    <el-card shadow="never" style="margin-top: 16px">
      <h3 class="card-title">床位使用情况</h3>
      <el-row :gutter="16" align="middle">
        <el-col :span="8">
          <el-progress
            type="dashboard"
            :percentage="data.bedUsageRate || 0"
            :color="progressColor"
          />
          <div style="text-align: center; color: #909399; margin-top: 6px">
            已用 {{ data.bedUsed }} / 共 {{ data.bedTotal }} 张
          </div>
        </el-col>
        <el-col :span="16">
          <div ref="bedChart" class="chart chart-sm"></div>
        </el-col>
      </el-row>
    </el-card>

    <el-row :gutter="16" style="margin-top: 16px">
      <el-col :span="12">
        <el-card shadow="never">
          <h3 class="card-title">各科室挂号量</h3>
          <div ref="deptRegisterChart" class="chart"></div>
        </el-card>
      </el-col>
      <el-col :span="12">
        <el-card shadow="never">
          <h3 class="card-title">患者性别分布</h3>
          <div ref="genderChart" class="chart"></div>
        </el-card>
      </el-col>
    </el-row>

    <el-row :gutter="16" style="margin-top: 16px">
      <el-col :span="24">
        <el-card shadow="never">
          <h3 class="card-title">近 7 天挂号趋势</h3>
          <div ref="trendChart" class="chart"></div>
        </el-card>
      </el-col>
    </el-row>

    <el-row :gutter="16" style="margin-top: 16px">
      <el-col :span="12">
        <el-card shadow="never">
          <h3 class="card-title">各科室收入</h3>
          <div ref="incomeChart" class="chart"></div>
        </el-card>
      </el-col>
      <el-col :span="12">
        <el-card shadow="never">
          <h3 class="card-title">医生工作量（接诊量）</h3>
          <div ref="doctorChart" class="chart"></div>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script>
import * as echarts from 'echarts'
import { getStatistics } from '@/api/admin'

export default {
  name: 'AdminStatistics',
  data() {
    return {
      data: {
        totalRegisters: 0,
        totalPatients: 0,
        totalDoctors: 0,
        totalIncome: 0,
        bedTotal: 0,
        bedUsed: 0,
        bedFree: 0,
        bedUsageRate: 0,
        departmentRegisters: [],
        genderDistribution: [],
        registerTrend: [],
        departmentIncome: [],
        doctorWorkload: [],
        bedStatus: []
      },
      charts: []
    }
  },
  computed: {
    progressColor() {
      const rate = this.data.bedUsageRate || 0
      if (rate >= 85) return '#f56c6c'
      if (rate >= 60) return '#e6a23c'
      return '#67c23a'
    }
  },
  mounted() {
    this.load()
  },
  beforeDestroy() {
    this.charts.forEach(c => c && c.dispose())
  },
  methods: {
    load() {
      getStatistics().then(res => {
        this.data = Object.assign(this.data, res.data || {})
        this.$nextTick(this.renderCharts)
      })
    },
    renderCharts() {
      this.charts.forEach(c => c && c.dispose())
      this.charts = []

      this.charts.push(
        this.drawPie(this.$refs.bedChart, this.data.bedStatus, ['#67c23a', '#f56c6c'])
      )
      this.charts.push(
        this.drawBar(
          this.$refs.deptRegisterChart,
          this.data.departmentRegisters,
          '挂号量',
          '#409eff'
        )
      )
      this.charts.push(
        this.drawPie(this.$refs.genderChart, this.data.genderDistribution, [
          '#409eff',
          '#f78989'
        ])
      )
      this.charts.push(this.drawLine(this.$refs.trendChart, this.data.registerTrend))
      this.charts.push(
        this.drawBar(this.$refs.incomeChart, this.data.departmentIncome, '收入（元）', '#e6a23c')
      )
      this.charts.push(
        this.drawBar(
          this.$refs.doctorChart,
          this.data.doctorWorkload,
          '接诊量',
          '#67c23a',
          true
        )
      )
    },
    drawBar(el, list, seriesName, color, horizontal) {
      if (!el) return null
      const chart = echarts.init(el)
      const items = list || []
      const names = items.map(i => i.name)
      const values = items.map(i => i.value)
      const option = horizontal
        ? {
            tooltip: { trigger: 'axis', axisPointer: { type: 'shadow' } },
            grid: { left: 90, right: 30, top: 20, bottom: 30 },
            xAxis: { type: 'value' },
            yAxis: { type: 'category', data: names },
            series: [{ type: 'bar', name: seriesName, data: values, itemStyle: { color } }]
          }
        : {
            tooltip: { trigger: 'axis' },
            grid: { left: 50, right: 20, top: 30, bottom: 40 },
            xAxis: { type: 'category', data: names, axisLabel: { interval: 0 } },
            yAxis: { type: 'value' },
            series: [{ type: 'bar', name: seriesName, data: values, itemStyle: { color } }]
          }
      chart.setOption(option)
      return chart
    },
    drawPie(el, list, colors) {
      if (!el) return null
      const chart = echarts.init(el)
      chart.setOption({
        tooltip: { trigger: 'item' },
        legend: { bottom: 0 },
        color: colors,
        series: [
          {
            type: 'pie',
            radius: ['40%', '65%'],
            center: ['50%', '45%'],
            data: (list || []).map(i => ({ name: i.name, value: i.value })),
            label: { formatter: '{b}: {c}' }
          }
        ]
      })
      return chart
    },
    drawLine(el, list) {
      if (!el) return null
      const chart = echarts.init(el)
      const items = list || []
      chart.setOption({
        tooltip: { trigger: 'axis' },
        grid: { left: 50, right: 30, top: 30, bottom: 40 },
        xAxis: { type: 'category', data: items.map(i => i.name), boundaryGap: false },
        yAxis: { type: 'value' },
        series: [
          {
            type: 'line',
            smooth: true,
            areaStyle: { opacity: 0.15 },
            itemStyle: { color: '#409eff' },
            data: items.map(i => i.value)
          }
        ]
      })
      return chart
    }
  }
}
</script>

<style scoped>
.chart {
  height: 300px;
  width: 100%;
}

.chart-sm {
  height: 220px;
}
</style>
