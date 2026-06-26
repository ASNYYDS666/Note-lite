import { createRouter, createWebHistory } from 'vue-router'
import { useUserStore } from '@/store/user'

const routes = [
    {
        path: '/login',
        name: 'Login',
        component: () => import('@/views/Login.vue'),
        meta: { public: true }
    },
    {
        path: '/register',
        name: 'Register',
        component: () => import('@/views/Register.vue'),
        meta: { public: true }
    },
    {
        path: '/',
        name: 'Workspace',
        component: () => import('@/views/Workspace.vue'),
        meta: { requiresAuth: true }
    },
    {
        path: '/share/:code',
        name: 'ShareView',
        component: () => import('@/views/ShareView.vue'),
        meta: { public: true }
    }
]

const router = createRouter({
    history: createWebHistory(),
    routes
})

// 全局路由守卫 (开发预览阶段关闭鉴权)
router.beforeEach((to, from, next) => {
    // TODO: 预览完成后恢复鉴权
    next()
})

export default router
