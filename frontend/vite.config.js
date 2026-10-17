import { defineConfig } from 'vite';
import react from '@vitejs/plugin-react';

// L'app React vive sotto /app/ dello stesso server Spring Boot (stessa origine → stessa sessione e cookie).
export default defineConfig({
  plugins: [react()],
  base: '/app/',
  build: {
    // `npm run build` (lanciato anche da ./mvnw package) scrive qui: Spring Boot serve i file statici da static/
    outDir: '../src/main/resources/static/app',
    emptyOutDir: true,
  },
  server: {
    port: 5173,
    // In sviluppo (`npm run dev`) le chiamate al backend vengono inoltrate a Spring Boot su 8080
    proxy: {
      '/api': 'http://localhost:8080',
      '/css': 'http://localhost:8080',
      '/login': 'http://localhost:8080',
      '/logout': 'http://localhost:8080',
    },
  },
});
