import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'

export default defineConfig({
    plugins: [vue()],
    server: {
        host: true,
        port: 5173,
        proxy: {
            '/api': {
                target: 'http://localhost:8080',
                changeOrigin: true,
                secure: false,
                configure: (proxy) => {
                    proxy.on('error', (err, req, res) => {
                        console.log('[VITE-PROXY][error]', req?.url, err?.message)
                    })
                    proxy.on('proxyReq', (proxyReq, req) => {
                        console.log('[VITE-PROXY][req]', req.method, req.url)
                    })
                    proxy.on('proxyRes', (proxyRes, req) => {
                        console.log('[VITE-PROXY][res]', req.method, req.url, proxyRes.statusCode)
                    })
                }
            },
            '/login': {
                target: 'http://localhost:8080',
                changeOrigin: true,
                secure: false
            }
        }
    },
})
