import { defineConfig } from 'vite'
  import react from '@vitejs/plugin-react'
  import tailwindcss from '@tailwindcss/vite'

  
  // https://vitejs.dev/config/
  export default defineConfig({
    plugins: [react(), tailwindcss()],
    
    // אופטימיזציות זיכרון
    optimizeDeps: {
      disabled: false,
      force: false,  // לא לכפות re-bundling
      include: [],   // רק מה שצריך
    },
    
    // הגדרות esbuild לזיכרון נמוך
    esbuild: {
      target: 'es2015',
      keepNames: false,
      minifyIdentifiers: false,
      minifySyntax: false,
      minifyWhitespace: false,
      legalComments: 'none'
    },
    
    // הגדרות build
    build: {
      target: 'es2015',
      minify: false,        // כבה minification לחיסכון בזיכרון
      sourcemap: false,     // כבה source maps
      
      rollupOptions: {
        maxParallelFileOps: 1,  // תהליך אחד בכל פעם
        output: {
          manualChunks: undefined  // בטל chunk splitting
        }
      },
      
      // הקטן chunk size
      chunkSizeWarningLimit: 1000
    },
    
    // הגדרות שרת פיתוח
    server: {
      host: '0.0.0.0',
      port: 3000,
      hmr: {
        overlay: false  // בטל error overlay
      },
      // הקטן watch overhead
      watch: {
        usePolling: false,
        ignored: ['**/node_modules/**', '**/.git/**']
      }
    },
    
    // הגדרות נוספות לחיסכון בזיכרון
    resolve: {
      dedupe: ['react', 'react-dom']  // מנע כפילויות
    },
    
    // כבה features לא חיוניות בפיתוח
    define: {
      __DEV__: JSON.stringify(true),
      'process.env.NODE_ENV': JSON.stringify('development')
    },
    
    // הגדרות CSS
    css: {
      devSourcemap: false  // כבה CSS source maps
    }
  })