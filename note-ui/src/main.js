import { createApp } from 'vue'
import { createPinia } from 'pinia'

// 本地自托管字体 (替代 Google Fonts，无需翻墙)
import '@fontsource/inter/400.css'
import '@fontsource/inter/500.css'
import '@fontsource/inter/600.css'
import '@fontsource/inter/700.css'
import '@fontsource/newsreader/400.css'
import '@fontsource/newsreader/600.css'
import '@fontsource/newsreader/700.css'
import 'material-symbols'

import './styles/tokens.css'
import './style.css'

import App from './App.vue'
import router from './router'

const app = createApp(App)

// 主题初始化
const savedTheme = localStorage.getItem('note-lite-theme')
if (savedTheme === 'dark') {
  document.documentElement.classList.add('dark')
}

app.use(createPinia())
app.use(router)

app.mount('#app')