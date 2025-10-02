// src/pages/VerifyComplete.jsx
import { useEffect } from "react";

export default function VerifyComplete() {
    useEffect(() => {
        const params = new URLSearchParams(window.location.search);
        const email = params.get("email") || "";

        localStorage.setItem("emailVerified", "1");
        if (email) localStorage.setItem("verifiedEmail", email);

        // 1~1.5초 후 가입 페이지로 자동 이동
        const t = setTimeout(() => {
            window.location.replace(
                "/FeatureAuthJoin?verified=1" + (email ? "&email=" + encodeURIComponent(email) : "")
            );
            // window.close(); // 새 탭을 자동으로 닫고 싶다면 시도(대부분 브라우저에서 막힘)
        }, 1200);

        return () => clearTimeout(t);
    }, []);

    return (
        <div style={{padding: 24}}>
            <h2>이메일 인증이 완료되었습니다 ✅</h2>
            <p>가입 페이지로 돌아가는 중입니다...</p>
            <p><a href="/FeatureAuthJoin">바로 이동</a></p>
        </div>
    );
}
