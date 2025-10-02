// src/pages/MyPage.jsx
import React, { useEffect, useState } from "react";
import { useNavigate, Link } from "react-router-dom";
import api from "@/client/axios";
import "../components/MyPage.css";

const MyPage = () => {
    const navigate = useNavigate();
    const [me, setMe] = useState(null);
    const [loading, setLoading] = useState(true);

    useEffect(() => {
        let alive = true;
        (async () => {
            try {
                const { data } = await api.get("/auth/me", { withCredentials: true });
                if (alive) setMe(data);
            } catch {
                if (alive) navigate("/", { replace: true }); // 로그인 화면
            } finally {
                if (alive) setLoading(false);
            }
        })();
        return () => {
            alive = false;
        };
    }, [navigate]);

    if (loading) return null;

    return (
        <div className="mypage-container">
            <h2>컨설턴트페이지</h2>

            <div className="user-info">
                <img
                    className="avatar"
                    src={
                        me?.profileImage ||
                        "https://via.placeholder.com/80x80.png?text=USER"
                    }
                    alt="프로필"
                />
                <div className="info-text">
                    <div className="user-name">{me?.name ?? "회원"}님</div>
                    <div className="emaill">{me?.email ?? "-"}</div>
                </div>

                <button
                    className="edit-btn"
                    onClick={() => navigate("/ConsultantEditProfile")}
                >
                    프로필 수정
                </button>
            </div>

            <div className="mypage-menu">
                <Link to="/ConsultantSchedule" className="menu-item">
                    예약 내역<span className="arrow">›</span>
                </Link>
                <Link to="/ConsultantScheduleRegister" className="menu-item">
                    가용 일정 등록 및 삭제<span className="arrow">›</span>
                </Link>
            </div>
        </div>
    );
};

export default MyPage;
