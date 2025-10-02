import React, { useEffect, useState } from "react";
import { useNavigate } from "react-router-dom";
import api from "@/client/axios";
import "../components/MyPageSchedule.css";

// ApiResponse({ data }) 또는 순수 배열 모두 안전하게 받기
const pick = (res) => res?.data?.data ?? res?.data ?? res;

export default function MyPageSchedule() {
    const [schedules, setSchedules] = useState([]);
    const [writingMap, setWritingMap] = useState({});
    const navigate = useNavigate();

    useEffect(() => {
        let alive = true;
        (async () => {
            try {
                // 1) 내 주문(예약 포함) 조회
                const r = await api.get("/mypage/orders");
                const orders = pick(r) || [];

                // 2) orders -> appointments를 화면 아이템으로 변환 (중복 제거)
                const now = Date.now();
                const itemsMap = new Map(); // appointmentId를 key로 사용

                for (const o of orders) {
                    const consName = o.consultantName ?? o.consultant?.name ?? "-";
                    const consId = o.consultantId ?? o.consultant?.id;

                    for (const a of o.appointments || []) {
                        const id = a.appointmentId ?? a.id;

                        // 이미 추가된 예약이면 스킵 (중복 제거)
                        if (itemsMap.has(id)) continue;

                        const start = new Date(a.startAt);
                        const end = new Date(a.endAt);
                        const status = end.getTime() <= now ? "done" : "upcoming";

                        itemsMap.set(id, {
                            id,
                            consultant: consName,
                            consultantId: consId,
                            startAt: a.startAt,
                            endAt: a.endAt,
                            status,
                            year: start.getFullYear(),
                            month: start.getMonth() + 1,
                            day: start.getDate(),
                            time: start.toLocaleTimeString([], { hour: "2-digit", minute: "2-digit" }),
                        });
                    }
                }

                const items = Array.from(itemsMap.values());

                // 3) 정렬: 예정(미래) 먼저(시작시간 오름차순) → 과거(내림차순)
                items.sort((x, y) => {
                    const xs = new Date(x.startAt).getTime();
                    const ys = new Date(y.startAt).getTime();
                    const xa = xs >= now;
                    const ya = ys >= now;
                    if (xa !== ya) return xa ? -1 : 1; // 예정 먼저
                    return xa ? xs - ys : ys - xs;     // 예정 오름 / 과거 내림
                });

                if (alive) setSchedules(items);

                // 4) 완료된 예약의 컨설턴트들에 대해 "내 리뷰 존재?" 병렬 조회
                const uniqueCons = [...new Set(items.filter(i => i.status === "done").map(i => i.consultantId))]
                    .filter(Boolean);
                if (uniqueCons.length) {
                    const results = await Promise.all(
                        uniqueCons.map(id =>
                            api.get("/reviews/my/existence", { params: { consultantId: id } })
                                .then(pick)
                                .catch(() => false)
                        )
                    );
                    const map = {};
                    uniqueCons.forEach((id, idx) => (map[id] = !!results[idx]));
                    if (alive) setWritingMap(map);
                } else {
                    if (alive) setWritingMap({});
                }
            } catch (e) {
                console.error("load schedules error:", e?.response?.data || e.message);
                if (alive) {
                    setSchedules([]);
                    setWritingMap({});
                }
            }
        })();
        return () => {
            alive = false;
        };
    }, []);

    const onChangeClick = (sch) => {
        navigate("/BookingDate", {
            state: {
                schedule: {
                    id: sch.id,
                    consultantId: sch.consultantId,
                    startAt: sch.startAt,
                    endAt: sch.endAt,
                },
                consultantId: sch.consultantId,
            },
        });
    };

    return (
        <div className="schedule-container">
            <h2>📅 일정 관리</h2>
            <p className="subtitle">컨설턴트는 변경할 수 없습니다.</p>

            <ul className="schedule-list">
                {schedules.length > 0 ? (
                    schedules.map((sch) => (
                        <li key={sch.id} className={`schedule-item ${sch.status}`}>
                            <div>
                <span className="date">
                  {sch.year}-{sch.month}-{sch.day} {sch.time}
                </span>
                                <span className="consultant">컨설턴트: {sch.consultant}</span>
                            </div>

                            {sch.status === "upcoming" ? (
                                <button className="btn change" onClick={() => onChangeClick(sch)}>
                                    일정 변경
                                </button>
                            ) : writingMap[sch.consultantId] ? (
                                <span className="done-label">리뷰 작성 완료</span>
                            ) : (
                                <button
                                    className="btn evaluate"
                                    onClick={() => navigate("/MyPageEvaluate", { state: { schedule: sch } })}
                                >
                                    컨설턴트 평가
                                </button>
                            )}
                        </li>
                    ))
                ) : (
                    <p className="empty">일정이 없습니다.</p>
                )}
            </ul>
        </div>
    );
}