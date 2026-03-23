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
        component: () => import('@/views/Layout.vue'),
        meta: { requiresAuth: true },
        children: [
            {
                path: '',
                name: 'NoteList',
                component: () => import('@/views/NoteList.vue')
            },
            {
                //day05更新路由配置：添加回收站到Layout的子路由中
                path:'recycle',
                name:'RecycleBin',
                component: ()=> import('@/views/RecycleBin.vue')
            },
            {
                path: 'note/new',
                name: 'NoteCreate',
                component: () => import('@/views/NoteEdit.vue')
            },
            {
                path: 'note/:id',
                name: 'NoteEdit',
                component: () => import('@/views/NoteEdit.vue')
            }
        ]
    }
]

const router = createRouter({
    history: createWebHistory(),
    routes
})

// 全局路由守卫
router.beforeEach((to, from, next) => {
    const userStore = useUserStore()

    if (to.meta.requiresAuth && !userStore.token) {
        next('/login')
    } else if (to.meta.public && userStore.token) {
        // 已登录用户访问登录页，跳首页
        next('/')
    } else {
        next()
    }
})

export default router
