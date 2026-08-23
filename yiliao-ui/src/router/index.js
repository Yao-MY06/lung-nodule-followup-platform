import { createRouter, createWebHistory } from 'vue-router'
import { useAuthStore } from '../stores/auth'

// 路由总表：视图文件由各子代理填充，此处只做导航与守卫（specs/modules 各模块 §3 对应页面）
const routes = [
  { path: '/login', name: 'login', component: () => import('../views/login/index.vue'), meta: { public: true } },
  {
    path: '/',
    component: () => import('../layout/AdminLayout.vue'),
    redirect: '/home',
    children: [
      { path: 'home', name: 'home', component: () => import('../views/home/index.vue'), meta: { title: '首页' } },
      { path: 'patient/list', name: 'patient-list', component: () => import('../views/patient/list.vue'), meta: { title: '患者列表' } },
      { path: 'patient/create', name: 'patient-create', component: () => import('../views/patient/create.vue'), meta: { title: '建档' } },
      { path: 'patient/detail/:id', name: 'patient-detail', component: () => import('../views/patient/detail.vue'), meta: { title: '患者详情' } },
      { path: 'nodule/report', name: 'nodule-report', component: () => import('../views/nodule/report.vue'), meta: { title: '报告录入与AI抽取' } },
      { path: 'nodule/trend/:patientId', name: 'nodule-trend', component: () => import('../views/nodule/trend.vue'), meta: { title: '结节趋势' } },
      { path: 'followup/workbench', name: 'followup-workbench', component: () => import('../views/followup/workbench.vue'), meta: { title: '随访工作台' } },
      { path: 'followup/timeline/:patientId', name: 'followup-timeline', component: () => import('../views/followup/timeline.vue'), meta: { title: '随访计划时间轴' } },
      { path: 'stats/dashboard', name: 'stats-dashboard', component: () => import('../views/stats/dashboard.vue'), meta: { title: '统计驾驶舱' } }
    ]
  },
  {
    path: '/portal',
    component: () => import('../layout/PatientLayout.vue'),
    redirect: '/portal/plan',
    children: [
      { path: 'plan', name: 'portal-plan', component: () => import('../views/portal/plan.vue'), meta: { title: '我的随访计划' } },
      { path: 'chat', name: 'portal-chat', component: () => import('../views/ai/chat.vue'), meta: { title: 'AI 助手' } },
      { path: 'interpret', name: 'portal-interpret', component: () => import('../views/ai/interpret.vue'), meta: { title: '报告解读' } }
    ]
  },
  { path: '/:pathMatch(.*)*', redirect: '/home' }
]

const router = createRouter({ history: createWebHistory(), routes })

// 守卫：未登录→登录页；患者角色强制走 /portal，其他角色走管理端（specs/modules/gateway.md §4.3）
router.beforeEach((to) => {
  const auth = useAuthStore()
  if (to.meta.public) {
    return true
  }
  if (!auth.isLoggedIn) {
    return { path: '/login', query: { redirect: to.fullPath } }
  }
  if (auth.isPatient && !to.path.startsWith('/portal')) {
    return '/portal/plan'
  }
  if (!auth.isPatient && to.path.startsWith('/portal')) {
    return '/home'
  }
  return true
})

export default router
