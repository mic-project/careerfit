// src/pages/Layout.jsx
import React from "react";
import { Outlet, NavLink, useNavigate, useLocation } from "react-router-dom";
import "../components/Layout.css";
import MainLogo from "../assets/MainLogo.png";
import api from "@/client/axios";

const Layout = () => {
    const navigate = useNavigate();
    const location = useLocation();

    const onLogout = async (e) => {
        e?.preventDefault?.();
        try {
            await api.post("/auth/logout", {}, { withCredentials: true });
        } catch {}
        if (window?.localStorage) localStorage.clear();
        if (window?.sessionStorage) sessionStorage.clear();
        navigate("/", { replace: true }); // 로그인 화면
    };

    return (
        <div className="home-container">
            <header className="home-header">
                <div className="logo" onClick={() => navigate("/MainPage")}>
                    <img src={MainLogo} alt="CareerFit 로고" />
                </div>

                <nav className="nav-menu">
                    <NavLink to="/UseGuide" className={({ isActive }) => (isActive ? "active" : "")}>
                        이용가이드
                    </NavLink>
                    <NavLink to="/MyPage" className={({ isActive }) => (isActive ? "active" : "")}>
                        마이페이지
                    </NavLink>
                    <a href="#" onClick={onLogout} role="button">
                        로그아웃
                    </a>
                </nav>
            </header>

            <main>
                <Outlet />
            </main>
        </div>
    );
};

export default Layout;
