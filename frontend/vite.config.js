import { defineConfig } from "vite";
import react from "@vitejs/plugin-react";
import path from "path";
import { fileURLToPath } from "url";
const __dirname = path.dirname(fileURLToPath(import.meta.url));

export default defineConfig({
    plugins: [react()],
    resolve: {
        alias: {
            // ⬇️ frontend 루트를 @로 고정 (여기에 api, pages가 모두 존재)
            "@": path.resolve(__dirname, "./"),
        },
    },
    server: {
        proxy: {
            "/api": { target: "http://localhost:8080", changeOrigin: true, secure: false },
            "/oauth2/authorization": { target: "http://localhost:8080", changeOrigin: true, secure: false },
            "/oauth2/redirect": { bypass: () => "/index.html" },
        },
    },
});