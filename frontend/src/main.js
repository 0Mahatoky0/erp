import './assets/base.css'
import './assets/main.css'
import './assets/tailwind.css'          // ← AJOUT (après tes CSS existants)
import 'primeicons/primeicons.css'

import { createApp } from 'vue'
import { createPinia } from 'pinia'
import PrimeVue from 'primevue/config'
import ToastService from 'primevue/toastservice'
import Aura from '@primevue/themes/aura'

import App from './App.vue'
import router from './router'

const app = createApp(App)

app.use(createPinia())
app.use(PrimeVue, {
  theme: {
    preset: Aura,
    options: {
      prefix: 'p',
      darkModeSelector: '.app-dark',
      cssLayer: {                        // ← AJOUT
        name: 'primevue',
        order: 'theme, base, primevue',
      },
    },
  },
})
app.use(ToastService)
app.use(router)

app.mount('#app')