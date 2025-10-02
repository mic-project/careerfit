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
                // 1) 내 정보 조회 → role 확인
                const { data: meRaw } = await api.get("/auth/me", { withCredentials: true });
                const meInfo = meRaw?.data ?? meRaw;

                // 2) 컨설턴트면 컨설턴트 마이페이지로 이동
                if (meInfo?.role === "CONSULTANT") {
                    navigate("/ConsultantPage", { replace: true });
                    return;
                }

                if (alive) setMe(meInfo);
            } catch (e) {
                const s = e?.response?.status;
                if (alive && (s === 401 || s === 403)) {
                    navigate("/", { replace: true }); // 로그인 화면
                } else {
                    console.error("mypage load error:", e?.response?.data || e.message);
                }
            } finally {
                if (alive) setLoading(false);
            }
        })();
        return () => { alive = false; };
    }, [navigate]);

    if (loading) return null;

    const avatarSrc =
        me?.profileImage ||
        me?.socialProfileImage ||
        "https://via.placeholder.com/80x80.png?text=USER";

    return (
        <div className="mypage-container">
            <h2>마이페이지</h2>

            <div className="user-info">
                <img className="avatar" src={avatarSrc} alt="프로필" />
                <div className="info-text">
                    <div className="user-name">{me?.name ?? "회원"}님</div>
                    <div className="email">{me?.email ?? "-"}</div>
                </div>

                <button
                    className="edit-btn"
                    onClick={() => navigate("/MyPageEditProfile")}
                >
                    프로필 수정
                </button>
            </div>

            <div className="mypage-menu">
                <Link to="/MyPageSchedule" className="menu-item">
                    예약 일정<span className="arrow">›</span>
                </Link>
                <Link to="/MyPageMeeting" className="menu-item">
                    화상면접 기록<span className="arrow">›</span>
                </Link>
                <Link to="/MyPagePractice" className="menu-item">
                    연습 기록<span className="arrow">›</span>
                </Link>
                <Link to="/MyPagePayment" className="menu-item">
                    구독/결제 내역<span className="arrow">›</span>
                </Link>
            </div>
        </div>
    );
};

export default MyPage;