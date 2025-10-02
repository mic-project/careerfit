// src/api/axios.js
import axios from "axios";

// ✅ baseURL: 환경에 따라 자동 설정
const baseURL = import.meta.env.VITE_API_BASE_URL ||
    (window.location.hostname === 'app.careerfit.net'
        ? 'https://api.careerfit.net/api'
        : '/api');

// ✅ 일반 API 인스턴스 (인터셉터 포함)
const api = axios.create({
    baseURL,
    withCredentials: true, // 쿠키 기반이면 필수
});

// ✅ 리프레시 호출용 별도 인스턴스 (인터셉터 X)
const refreshClient = axios.create({
    baseURL,
    withCredentials: true,
});

let isRefreshing = false;
let waiters = [];

api.interceptors.response.use(
    (res) => res,
    async (error) => {
        const original = error.config;
        const status = error.response?.status;

        // ❗ HttpOnly 쿠키는 JS로 보이지 않음 → document.cookie 검사 제거
        // 리프레시 엔드포인트 자기 자신은 제외
        const isRefreshCall = original?.url?.includes("/auth/refresh");

        // 403 Access Denied도 로그인 페이지로 리다이렉트
        if (status === 403) {
            window.location.replace("/login");
            return Promise.reject(error);
        }

        if (status === 401 && !original._retry && !isRefreshCall) {
            original._retry = true;

            try {
                if (!isRefreshing) {
                    isRefreshing = true;
                    await refreshClient.post("/auth/refresh"); // 서버가 Set-Cookie로 새 토큰 내려줌
                    isRefreshing = false;

                    // 대기중인 요청들 재개
                    waiters.forEach((resume) => resume());
                    waiters = [];
                } else {
                    // 다른 탭/요청이 리프레시 중이면 완료까지 대기
                    await new Promise((resolve) => waiters.push(resolve));
                }

                // 원래 요청 재시도
                return api(original);
            } catch (e) {
                isRefreshing = false;
                waiters = [];
                // 세션 만료 → 로그인으로
                window.location.replace("/login");
                return Promise.reject(e);
            }
        }

        return Promise.reject(error);
    }
);

export default api;
