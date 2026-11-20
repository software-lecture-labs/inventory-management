import { defineConfig } from "vite";
import react from "@vitejs/plugin-react";

export default defineConfig({
  plugins: [react()],
  server: {
    port: 5173,
    // Geliştirme sırasında /api isteklerini Spring Boot'a yönlendir
    proxy: {
      "/api": "http://localhost:8080",
    },
  },
  build: {
    rollupOptions: {
      output: {
        // Büyük kütüphaneleri ayrı dosyalara böl (tarayıcı önbelleği için)
        manualChunks: {
          react: ["react", "react-dom", "react-router-dom"],
          ui: ["react-bootstrap", "react-icons"],
          charts: ["recharts"],
          calendar: [
            "@fullcalendar/core",
            "@fullcalendar/react",
            "@fullcalendar/daygrid",
            "@fullcalendar/timegrid",
            "@fullcalendar/interaction",
          ],
        },
      },
    },
  },
});
