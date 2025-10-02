// OAuth2Redirect.jsx
import { useEffect } from "react";
import { useNavigate } from "react-router-dom";
import api from "@/client/axios"

export default function OAuth2Redirect() {
    const navigate = useNavigate();

    useEffect(() => {
        const go = async () => {
            try {
                // 아주 짧게 쿠키 적용 기다리기
                await new Promise(r => setTimeout(r, 150));
                // 바로 /me 검증
                await api.get("/auth/me");
                navigate("/MainPage", { replace: true });
            } catch {
                // 혹시 401이면 한 번 리프레시 후 재시도
                try {
                    await api.post("/auth/refresh");
                    await new Promise(r => setTimeout(r, 120));
                    await api.get("/auth/me");
                    navigate("/MainPage", { replace: true });
                } catch {
                    navigate("/login", { replace: true });
                }
            }
        };
        go();
    }, [navigate]);

    return null;
}