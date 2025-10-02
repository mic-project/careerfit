import React, { useEffect, useState } from "react";
import { useNavigate } from "react-router-dom";
import api from "@/client/axios";
import "../components/MyPageSchedule.css";

const ConsultantSchedule = () => {
    const [schedules, setSchedules] = useState([]);
    const navigate = useNavigate();

    useEffect(() => {
        (async () => {
            try {
                // baseURL=/api → 접두사 없이
                const res = await api.get("/consultants/me/schedules", { withCredentials: true });
                setSchedules(res.data?.data ?? res.data ?? []);
            } catch (e) {
                console.error("load schedules error:", e?.response?.data || e.message);
            }
        })();
    }, []);

    const fmt = (iso) => {
        const d = new Date(iso);
        const y = d.getFullYear();
        const m = String(d.getMonth() + 1).padStart(2, "0");
        const day = String(d.getDate()).padStart(2, "0");
        const hh = String(d.getHours()).padStart(2, "0");
        const mm = String(d.getMinutes()).padStart(2, "0");
        return `${y}-${m}-${day} ${hh}:${mm}`;
    };

    return (
        <div className="schedule-container">
            <h2>📅 예약 내역</h2>
            <p className="subtitle">예약 일정 및 종료된 일정</p>

            <ul className="schedule-list">
                {schedules.length > 0 ? (
                    schedules.map((sch) => (
                        <li key={sch.id} className={`schedule-item ${sch.status?.toLowerCase?.() || ""}`}>
                            <div>
                <span className="date">
                  {fmt(sch.startAt)} ~ {fmt(sch.endAt)}
                </span>
                                {sch.userName && <span className="consultant">회원명: {sch.userName}</span>}
                            </div>

                            {sch.status === "UPCOMING" ? (
                                <button className="btn change" onClick={() => navigate("/Meeting")}>면접 입장</button>
                            ) : (
                                <span className="done-label">종료됨</span>
                            )}
                        </li>
                    ))
                ) : (
                    <p className="empty">일정이 없습니다.</p>
                )}
            </ul>
        </div>
    );
};

export default ConsultantSchedule;
