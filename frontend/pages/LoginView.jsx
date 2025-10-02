// LoginView.jsx

import React, { useState } from "react";
import { useNavigate } from "react-router-dom";
import api from "@/client/axios";
import "../components/LoginView.css";
import LoginViewImg from "../assets/LoginViewImg.png";
import MainLogo from "../assets/MainLogo.png";

// 소셜 아이콘
import GoogleIcon from "../assets/google.png";
import KakaoIcon from "../assets/kakao.png";
import NaverIcon from "../assets/naver.png";

const API_BASE =
    import.meta.env.VITE_API_BASE_URL ?? "http://localhost:8080/api";
const API_ORIGIN = API_BASE.replace(/\/api$/, ""); // http://localhost:8080

const toOAuthHref = (provider) =>
    `${API_ORIGIN}/oauth2/authorization/${provider}`;

const SocialLogin = () => {
    const providers = [
        {
            id: "google",
            name: "Google",
            icon: GoogleIcon,
            href: toOAuthHref("google"),
        },
        { id: "kakao", name: "Kakao", icon: KakaoIcon, href: toOAuthHref("kakao") },
        { id: "naver", name: "Naver", icon: NaverIcon, href: toOAuthHref("naver") },
    ];
    return (
        <div className="social-login">
            <div className="social-divider">
                <span className="line" />
                <span className="label">간편로그인으로 시작하기</span>
                <span className="line" />
            </div>
            <div className="social-buttons" role="group" aria-label="소셜 로그인">
                {providers.map((p) => (
                    <a key={p.id} className={`social-btn ${p.id}`} href={p.href}>
                        <img src={p.icon} alt={`${p.name} 아이콘`} />
                        <span>{p.name}</span>
                    </a>
                ))}
            </div>
        </div>
    );
};

const LoginView = () => {
    const navigate = useNavigate();

    const LoginForm = () => {
        const [formData, setFormData] = useState({ username: "", password: "" });

        const handleChange = (e) => {
            const { name, value } = e.target;
            setFormData({ ...formData, [name]: value });
        };

        const handleSubmit = async (e) => {
            e.preventDefault();
            try {
                // 서버가 쿠키 Set-Cookie로 내려줌 (withCredentials 전역 or 여기서 true)
                await api.post(
                    "/auth/login",
                    { email: formData.username, password: formData.password },
                    { withCredentials: true }
                );

                // 로그인 후 역할 확인
                const { data } = await api.get("/auth/me", { withCredentials: true });
                const me = data?.data ?? data;
                if (me?.role === "ADMIN") navigate("/admin", { replace: true });
                else navigate("/MainPage", { replace: true });
            } catch (err) {
                alert("로그인 실패");
            }
        };

        const handleSignUpClick = () => navigate("/FeatureAuthJoin");

        return (
            <>
                <form className="login-form" onSubmit={handleSubmit}>
                    <h2>로그인 및 회원가입</h2>

                    <label htmlFor="username">아이디</label>
                    <input
                        type="text"
                        id="username"
                        name="username"
                        placeholder="아이디를 입력하세요"
                        value={formData.username}
                        onChange={handleChange}
                        required
                    />

                    <label htmlFor="password">비밀번호</label>
                    <div className="input-wrapper">
                        <input
                            type="password"
                            id="password"
                            name="password"
                            placeholder="비밀번호를 입력하세요"
                            value={formData.password}
                            onChange={handleChange}
                            required
                        />
                    </div>

                    <div className="button-groupp">
                        <button type="submit">로그인</button>
                        <button
                            type="button"
                            className="signup-btn"
                            onClick={handleSignUpClick}
                        >
                            회원가입
                        </button>
                    </div>

                    <SocialLogin />
                </form>
            </>
        );
    };

    return (
        <div className="login-container">
            <div className="LoginView-header">
                <img src={MainLogo} alt="CareerFit 로고" />
            </div>

            <div className="login-left">
                <img src={LoginViewImg} alt="로그인뷰 이미지" />
                <p>오직 나만을 위한 AI 면접 피드백</p>
                <p>커리어핏과 함께 당신의 커리어를 완성하세요</p>
            </div>

            <div className="login-right">
                <LoginForm />
            </div>
        </div>
    );
};

export default LoginView;