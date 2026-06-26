import axios from 'axios'
import { useUserStore } from '@/store/user'
import { ElMessage } from 'element-plus'
import router from '@/router'

const request = axios.create({
    baseURL: '/api/v1', //改成了相对地址避免跨域问题
    timeout: 10000,
    headers: {
        'Content-Type': 'application/json'
    }
})

// 请求拦截器：自动添加 Token
request.interceptors.request.use(
    (config) => {
        const userStore = useUserStore()
        if (userStore.token) {
            config.headers.Authorization = `Bearer ${userStore.token}`
        }
        return config
    },
    (error) => {
        return Promise.reject(error)
    }
)

// 错误码映射表：根据业务错误码决定处理策略
const ERROR_HANDLERS = {
    10004: () => {  // TOKEN_EXPIRED
        const userStore = useUserStore()
        userStore.logout()
        router.push('/login')
        ElMessage.error('登录已过期，请重新登录')
    },
    10005: () => {  // TOKEN_INVALID
        const userStore = useUserStore()
        userStore.logout()
        router.push('/login')
        ElMessage.error('登录已失效，请重新登录')
    }
}

// 响应拦截器：统一错误处理
request.interceptors.response.use(
    (response) => {
        const res = response.data
        // 你的 Result 包装格式：{ code, message, data }
        // 成功响应（200）
        if (res.code === 200) {
            return res.data//只返回data部分
        }
        // 业务错误
        const handler = ERROR_HANDLERS[res.code]
        if (handler) {
            handler(res)
        } else if (!response.config.silent) {
            ElMessage.error(res.message || '请求失败')
        }

        return Promise.reject({
            code:res.code,
            message:res.message,
            data:res.data
        })
    },
    (error) => {
        const { response } = error
        if (!response) {
            // 网络完全断开，只弹一次
            if (!error.config?.silent) {
                ElMessage.error({ message: '网络连接失败，请检查后端服务', grouping: true })
            }
            return Promise.reject(error)
        }

        if (response?.status === 401 || response?.status=== 403) {
            if (!error.config?.silent) {
                ElMessage.error('登录已过期，请重新登录')
            }
            const userStore = useUserStore()
            userStore.logout()
            router.push('/login')
        } else if (!error.config?.silent) {
            ElMessage.error({ message: response?.data?.message || '请求失败', grouping: true })
        }

        return Promise.reject(error)
    }
)

export default request