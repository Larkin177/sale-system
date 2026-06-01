import { createRouter, createWebHistory } from 'vue-router'

const routes = [
  // 客户端
  {
    path: '/',
    name: 'Home',
    component: () => import('../views/customer/Home.vue')
  },
  {
    path: '/pay',
    name: 'Pay',
    component: () => import('../views/customer/Pay.vue')
  },
  {
    path: '/download',
    name: 'Download',
    component: () => import('../views/customer/Download.vue')
  },
  // 销售端
  {
    path: '/s/login',
    name: 'SalesLogin',
    component: () => import('../views/sales/Login.vue')
  },
  {
    path: '/s/register',
    name: 'SalesRegister',
    component: () => import('../views/sales/Register.vue')
  },
  {
    path: '/s/dashboard',
    name: 'SalesDashboard',
    component: () => import('../views/sales/Dashboard.vue'),
    meta: { requiresAuth: true, role: 'sales' }
  },
  {
    path: '/s/orders',
    name: 'SalesOrders',
    component: () => import('../views/sales/Orders.vue'),
    meta: { requiresAuth: true, role: 'sales' }
  },
  {
    path: '/s/link',
    name: 'SalesLink',
    component: () => import('../views/sales/Link.vue'),
    meta: { requiresAuth: true, role: 'sales' }
  },
  {
    path: '/s/claim',
    name: 'SalesClaim',
    component: () => import('../views/sales/Claim.vue'),
    meta: { requiresAuth: true, role: 'sales' }
  },
  {
    path: '/s/leaderboard',
    name: 'SalesLeaderboard',
    component: () => import('../views/sales/Leaderboard.vue'),
    meta: { requiresAuth: true, role: 'sales' }
  },
  // 管理端
  {
    path: '/admin/login',
    name: 'AdminLogin',
    component: () => import('../views/admin/Login.vue')
  },
  {
    path: '/admin/dashboard',
    name: 'AdminDashboard',
    component: () => import('../views/admin/Dashboard.vue'),
    meta: { requiresAuth: true, role: 'admin' }
  },
  {
    path: '/admin/sales',
    name: 'AdminSales',
    component: () => import('../views/admin/Sales.vue'),
    meta: { requiresAuth: true, role: 'admin' }
  },
  {
    path: '/admin/orders',
    name: 'AdminOrders',
    component: () => import('../views/admin/Orders.vue'),
    meta: { requiresAuth: true, role: 'admin' }
  },
  {
    path: '/admin/settle',
    name: 'AdminSettle',
    component: () => import('../views/admin/Settle.vue'),
    meta: { requiresAuth: true, role: 'admin' }
  },
  {
    path: '/admin/config',
    name: 'AdminConfig',
    component: () => import('../views/admin/Config.vue'),
    meta: { requiresAuth: true, role: 'admin' }
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

// 路由守卫
router.beforeEach((to, from, next) => {
  const token = localStorage.getItem('token')
  const role = localStorage.getItem('role')

  if (to.meta.requiresAuth) {
    if (!token) {
      next(`/login?redirect=${to.fullPath}`)
      return
    }
    if (to.meta.role && to.meta.role !== role) {
      next('/')
      return
    }
  }
  next()
})

export default router
