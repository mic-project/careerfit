import React, { useEffect, useState } from "react";
import { useNavigate } from "react-router-dom";
import "../components/Booking.css";
import JUNIOR from "../assets/JUNIOR.png";
import SENIOR from "../assets/SENIOR.png";
import EXECUTIVE from "../assets/EXECUTIVE.png";
import api from "@/client/axios";

export default function Booking() {
    const navigate = useNavigate();

    const [experts, setExperts] = useState([]);
    const [selectedExpert, setSelectedExpert] = useState(null);
    const [loading, setLoading] = useState(true);
    const [loadError, setLoadError] = useState("");

    // 경력(년) → 등급 & 뱃지
    const resolveTier = (careerYears) => {
        const years = Number.isFinite(+careerYears) ? +careerYears : null;
        if (years == null || years <= 5) return { label: "JUNIOR", img: JUNIOR };
        if (years <= 10) return { label: "SENIOR", img: SENIOR };
        return { label: "EXECUTIVE", img: EXECUTIVE };
    };

    useEffect(() => {
        let mounted = true;
        setLoading(true);
        setLoadError("");

        // 🔹 공개 리스트: consultantId, name, profileImage, careerYears, specialty, introduction
        api.get("/public/consultants")
            .then((res) => {
                if (!mounted) return;
                const raw = res.data;
                const list = Array.isArray(raw?.data) ? raw.data : Array.isArray(raw) ? raw : [];
                setExperts(list);
            })
            .catch((e) => {
                if (!mounted) return;
                setLoadError(e?.response?.data?.message || "컨설턴트 목록을 불러오지 못했습니다.");
                setExperts([]);
            })
            .finally(() => mounted && setLoading(false));

        return () => { mounted = false; };
    }, []);

    const handleBooking = () => {
        if (!selectedExpert) return alert("전문가를 선택해주세요!");
        const cid = selectedExpert.consultantId ?? selectedExpert.id;

        navigate("/BookingDate", {
            state: {
                consultantId: cid,
                expertInfo: {
                    name: selectedExpert.name,
                    career: selectedExpert.careerYears,   // ✅ 경력(년)
                    specialty: selectedExpert.specialty,  // ✅ 전문분야
                    specialtyCareer: "-",                 // ✅ 공개 DTO에 없으니 표시만 '-'
                },
            },
        });
    };

    const renderHeader = () => (
        <div className="doc-feedback-header">
            <h2>1:1 화상면접 ➡ 화상면접 일정 예약 / 결제</h2>
            <p>원하는 면접 전문가를 선택하고 일정을 예약하세요.</p>
        </div>
    );

    const renderTierInfo = () => (
        <div className="level-info">
            <p><b>컨설턴트 등급과 가격(부가세 포함)은 아래와 같습니다.</b></p>
            <p><b>JUNIOR</b> : 컨설턴트 경력 5년차 이하 | 1회 3만원</p>
            <p><b>SENIOR</b> : 컨설턴트 경력 6 ~ 10년차 이하 | 1회 5만원</p>
            <p><b>EXECUTIVE</b> : 컨설턴트 경력 10년차 초과 | 1회 8만원</p>
        </div>
    );

    const renderExperts = () => {
        if (loading) return <div className="loading">불러오는 중...</div>;
        if (loadError) return <div className="error-text">{loadError}</div>;
        if (!experts.length) return <div className="empty-text">등록된 컨설턴트가 없습니다.</div>;

        return (
            <div className="experts-list">
                {experts.map((ex) => {
                    const cid = ex.consultantId ?? ex.id;
                    const selected = selectedExpert && ((selectedExpert.consultantId ?? selectedExpert.id) === cid);
                    const { label: tierLabel, img: tierImg } = resolveTier(ex.careerYears);
                    const profileSrc = ex.profileImage || null; // 공개 DTO 기준

                    return (
                        <div
                            key={cid}
                            className={`expert-card ${selected ? "selected" : ""}`}
                            onClick={() => setSelectedExpert(ex)}
                            role="button"
                            tabIndex={0}
                            onKeyDown={(ev) => (ev.key === "Enter" || ev.key === " ") && setSelectedExpert(ex)}
                        >
                            <div className="tier-badge">
                                <img src={tierImg} alt={`${tierLabel} 뱃지`} loading="lazy" />
                            </div>

                            <div className="expert-thumb">
                                {profileSrc ? <img src={profileSrc} alt={`${ex.name} 프로필`} loading="lazy" /> : null}
                            </div>

                            <div className="expert-info">
                                <p><b>이름</b> : {ex.name}</p>
                                <p><b>컨설턴트 경력</b> : {ex.careerYears ?? "-"}년 ({tierLabel})</p>
                                <p><b>전문분야</b> : {ex.specialty ?? "-"}</p>
                                <p><b>전문분야 경력</b> : {/* 공개 DTO엔 없으니 */} -</p>
                                <p><b>컨설턴트 평점</b> : {/* 평균평점 없으니 */} 평가 없음</p>
                            </div>
                        </div>
                    );
                })}
            </div>
        );
    };

    return (
        <div className="home-container">
            <div className="doc-feedback-container">
                {renderHeader()}
                {renderTierInfo()}
                {renderExperts()}

                <div className="booking-button-container">
                    <button className="booking-btn" onClick={handleBooking} disabled={!selectedExpert}>
                        예약하기
                    </button>
                </div>
            </div>
        </div>
    );
}