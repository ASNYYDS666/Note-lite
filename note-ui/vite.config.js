import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'
import Components from 'unplugin-vue-components/vite'
import { ElementPlusResolver } from 'unplugin-vue-components/resolvers'
import path from 'path'

export default defineConfig({
    plugins: [
        vue(),
        Components({
            resolvers: [ElementPlusResolver()],
        }),
    ],
    resolve: {
        alias: {
            '@': path.resolve(__dirname, './src')
        }
    },
    server: {
        port: 3100,
        open: true,
        proxy: {
            '/api': {
                target: 'http://localhost:8080',
                changeOrigin: true
            }
        }
    },
    build: {
        rollupOptions: {
            output: {
                manualChunks: {
                    'vendor-vue': ['vue', 'vue-router', 'pinia'],
                    'vendor-element': ['element-plus'],
                    'vendor-editor': ['@tiptap/vue-3', '@tiptap/starter-kit', '@tiptap/extension-placeholder'],
                    'vendor-utils': ['axios', 'marked', 'dompurify', 'highlight.js'],
                }
            }
        }
    }
})