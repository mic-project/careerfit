import React, { useEffect, useMemo, useState } from "react";
import { useLocation, useNavigate } from "react-router-dom";
import "../components/BookingDate.css";
import api from "@/client/axios";
import DefaultProfile from "../assets/default-profile.png";

// (상단) 동적 import 유틸 - 함수 '참조'만 반환
const loadPortOne = async () => {
    const { ensurePortOne } = await import("@/client/portone");
    return ensurePortOne;     // <- 여기! 호출하지 말고 함수 자체를 반환
};

// ★ 로컬 시각 + 로컬 오프셋이 포함된 ISO(OffsetDateTime) 생성
const toLocalOffsetIso = (isoLike) => {
    const d = new Date(isoLike);
    const pad = (n) => String(n).padStart(2, "0");
    const yyyy = d.getFullYear();
    const mm = pad(d.getMonth() + 1);
    const dd = pad(d.getDate());
    const hh = pad(d.getHours());
    const mi = pad(d.getMinutes());
    const ss = pad(d.getSeconds());
    const tzMin = -d.getTimezoneOffset();
    const sign = tzMin >= 0 ? "+" : "-";
    const abs = Math.abs(tzMin);
    const tzh = pad(Math.floor(abs / 60));
    const tzm = pad(abs % 60);
    return `${yyyy}-${mm}-${dd}T${hh}:${mi}:${ss}${sign}${tzh}:${tzm}`;
};

export default function BookingDate() {
    const { state } = useLocation();
    const nav = useNavigate();

    // ── 진입 파라미터 ─────────────────────────────────────────────
    const schedule = state?.schedule || null;         // MyPageSchedule에서 넘긴 기존 예약
    const consultantId = state?.consultantId || schedule?.consultantId;
    const selectedExpertInfo = state?.expertInfo;

    // “리스케줄 모드” 여부(= 기존 예약에서 들어온 경우)
    const isReschedule = !!schedule?.id;

    // 가드
    useEffect(() => {
        if (!consultantId) nav("/Booking");
    }, [consultantId, nav]);

    // ── 컨설턴트 정보 ─────────────────────────────────────────────
    const [consultant, setConsultant] = useState(null);
    const [consultantLoading, setConsultantLoading] = useState(true);
    const [consultantError, setConsultantError] = useState("");

    useEffect(() => {
        if (!consultantId) return;
        setConsultantLoading(true);
        setConsultantError("");
        api
            .get(`/public/consultants/${consultantId}`)
            .then((res) => setConsultant(res.data?.data ?? res.data))
            .catch((e) => {
                console.error("load consultant error:", e?.response?.data || e.message);
                setConsultantError(
                    e?.response?.data?.message || "컨설턴트 정보를 불러오지 못했습니다."
                );
            })
            .finally(() => setConsultantLoading(false));
    }, [consultantId]);

    // ── 날짜/캘린더 상태 ─────────────────────────────────────────
    const today = new Date();
    const [selectedYear, setSelectedYear] = useState(today.getFullYear());
    const [selectedMonth, setSelectedMonth] = useState(today.getMonth());
    const [selectedDate, setSelectedDate] = useState(null);

    // ── 가용 슬롯 ─────────────────────────────────────────────────
    const [slots, setSlots] = useState([]);
    const [selectedSlot, setSelectedSlot] = useState(null);
    const [slotsLoading, setSlotsLoading] = useState(true);
    const [slotsError, setSlotsError] = useState("");

    const toLocalYmd = (d) =>
        `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, "0")}-${String(
            d.getDate()
        ).padStart(2, "0")}`;

    const monthStart = new Date(selectedYear, selectedMonth, 1);
    const monthEnd = new Date(selectedYear, selectedMonth + 1, 0);
    const fromStr = toLocalYmd(monthStart);
    const toStr = toLocalYmd(monthEnd);

    useEffect(() => {
        if (!consultantId) return;
        setSlotsLoading(true);
        setSlotsError("");
        api
            .get(`/availability/${consultantId}/slots`, { params: { from: fromStr, to: toStr } })
            .then((res) => {
                const list = res.data?.data ?? res.data ?? [];
                setSlots(list);
                setSelectedDate(null);
                setSelectedSlot(null);
            })
            .catch((e) => {
                console.error("load slots error:", e?.response?.data || e.message);
                setSlots([]);
                setSlotsError(e?.response?.data?.message || "가용 일정을 불러오지 못했습니다.");
            })
            .finally(() => setSlotsLoading(false));
    }, [consultantId, fromStr, toStr]);

    // ── 달력/시간 계산 ───────────────────────────────────────────
    const firstDayOfMonth = new Date(selectedYear, selectedMonth, 1).getDay();
    const daysInMonth = new Date(selectedYear, selectedMonth + 1, 0).getDate();
    const calendarDays = useMemo(() => {
        const arr = [];
        for (let i = 0; i < firstDayOfMonth; i++) arr.push(null);
        for (let i = 1; i <= daysInMonth; i++) arr.push(i);
        return arr;
    }, [firstDayOfMonth, daysInMonth]);

    const slotsOfSelectedDate = useMemo(() => {
        if (!selectedDate) return [];
        const sel = `${selectedYear}-${String(selectedMonth + 1).padStart(2, "0")}-${String(
            selectedDate
        ).padStart(2, "0")}`;
        return slots.filter((s) => (s.startAt ?? s.start)?.startsWith(sel));
    }, [slots, selectedYear, selectedMonth, selectedDate]);

    const fmtTime = (iso) => {
        const d = new Date(iso);
        return `${String(d.getHours()).padStart(2, "0")}:${String(d.getMinutes()).padStart(2, "0")}`;
    };
    const isPastDate = (y, m, d) => {
        const dateObj = new Date(y, m, d);
        const todayOnly = new Date(today.getFullYear(), today.getMonth(), today.getDate());
        return dateObj < todayOnly;
    };
    const isPastTime = (iso) => new Date(iso).getTime() <= Date.now();

    // ── 액션(확정) ────────────────────────────────────────────────
    const [submitting, setSubmitting] = useState(false);

    const handleConfirm = async () => {
        try {
            if (!consultantId || !selectedSlot) {
                alert("날짜와 시간을 선택해주세요.");
                return;
            }

            const startIso = selectedSlot.startAt ?? selectedSlot.start;
            const endIso   = selectedSlot.endAt   ?? selectedSlot.end;
            if (isPastTime(startIso)) {
                alert("과거 시간은 선택할 수 없습니다.");
                return;
            }

            const startLocal = toLocalOffsetIso(startIso);
            const endLocal   = toLocalOffsetIso(endIso);

            if (submitting) return;
            setSubmitting(true);

            // 1) 리스케줄 모드
            if (isReschedule) {
                try {
                    await api.patch(`/appointments/${schedule.id}/reschedule`, {
                        startAt: startLocal,
                        endAt: endLocal,
                    });
                    alert("일정이 변경되었습니다.");
                    nav("/MyPageSchedule");
                    return;              // <- ★ 결제 코드로 내려가지 않도록 즉시 반환
                } catch (e) {
                    const status = e?.response?.status;
                    const msg = e?.response?.data?.message || e.message;
                    if (status === 409) {
                        alert("이미 예약된 시간입니다. 다른 시간대를 선택해주세요.");
                    } else {
                        alert(`변경 실패: ${msg}`);
                    }
                    setSubmitting(false);
                    return;              // <- ★ 실패해도 결제 코드로 내려가지 않도록
                }
            }

            // 2) 신규 예약(결제) 모드
            const impCode = import.meta.env.VITE_IMP_CODE;
            const channelKey = import.meta.env.VITE_PORTONE_CHANNEL_KEY;
            const pgFallback = import.meta.env.VITE_PORTONE_PG || "html5_inicis";
            if (!impCode) {
                alert("결제 환경변수(VITE_IMP_CODE)가 설정되어 있지 않습니다.");
                setSubmitting(false);
                return;
            }

            const checkoutBody = {
                consultantId,
                bundle: "ONE",
                slots: [{ startAt: startLocal, endAt: endLocal }],
                method: "card",
            };
            const { data } = await api.post("/payments/checkout", checkoutBody);
            const payload = data?.data || data;
            if (!payload?.merchantUid) {
                throw new Error("결제 요청 정보가 올바르지 않습니다.(merchantUid 없음)");
            }

            // PortOne SDK
            const ensurePortOne = await loadPortOne();  // <- 함수 참조를 받고
            const IMP = await ensurePortOne();          // <- 여기서 한 번만 호출
            IMP.init(impCode);

            const baseParams = {
                pay_method: "card",
                merchant_uid: payload.merchantUid,
                name: payload.name,
                amount: Number(payload.amount),
                buyer_name: payload.buyerName,
                buyer_tel: payload.buyerTel || "010-0000-0000",
                buyer_email: payload.buyerEmail || "",
            };
            const isMobile = /Android|webOS|iPhone|iPad|iPod|BlackBerry|IEMobile|Opera Mini/i.test(navigator.userAgent);
            const params = {
                ...(channelKey ? { channelKey } : { pg: pgFallback }),
                ...(isMobile ? { m_redirect_url: `${window.location.origin}/payment/complete` } : {}),
                ...baseParams,
            };

            IMP.request_pay(params, async (rsp) => {
                try {
                    if (!rsp?.success) {
                        alert(`결제 실패: ${rsp?.error_msg || "취소/오류"}`);
                        return;
                    }
                    await api.post("/payments/confirm", {
                        impUid: rsp.imp_uid,
                        merchantUid: rsp.merchant_uid,
                    });
                    alert("결제가 완료되었습니다.");
                    nav("/MyPage");
                } catch (e) {
                    const msg = e?.response?.data?.message || e?.message || "결제 확인 실패";
                    console.error("confirm error:", e);
                    alert(`결제 승인 검증 실패: ${msg}`);
                } finally {
                    setSubmitting(false);
                }
            });
        } catch (e) {
            const msg = e?.response?.data?.message || e?.message || "요청 실패";
            console.error("submit error:", e);
            alert(msg);
            setSubmitting(false);
        }
    };

    // ── 렌더 ─────────────────────────────────────────────────────
    const months = ["1월","2월","3월","4월","5월","6월","7월","8월","9월","10월","11월","12월"];
    const years = [today.getFullYear(), today.getFullYear() + 1];
    const weekDays = ["일", "월", "화", "수", "목", "금", "토"];

    const display = {
        photo:
            selectedExpertInfo?.photo ||
            selectedExpertInfo?.profileImage ||
            consultant?.profileImage ||
            consultant?.avatar ||
            DefaultProfile,
        name: selectedExpertInfo?.name || consultant?.name || "-",
        careerYears:
            selectedExpertInfo?.careerYears ??
            selectedExpertInfo?.career ??
            consultant?.careerYears ??
            consultant?.totalCareerYears ??
            "-",
        organization:
            selectedExpertInfo?.organization ||
            selectedExpertInfo?.affiliation ||
            consultant?.organization ||
            consultant?.affiliation ||
            consultant?.company ||
            consultant?.orgName ||
            "-",
        specialty:
            selectedExpertInfo?.specialty ||
            consultant?.specialty ||
            consultant?.expertise ||
            "-",
        specialtyYears:
            selectedExpertInfo?.specialtyCareerYears ??
            selectedExpertInfo?.specialtyCareer ??
            consultant?.specialtyCareerYears ??
            consultant?.expertiseYears ??
            "-",
        introduction:
            selectedExpertInfo?.introduction ||
            consultant?.introduction ||
            consultant?.bio ||
            "-",
    };

    return (
        <div className="home-container">
            <div className="doc-feedback-container">
                <div className="doc-feedback-header">
                    <h2>
                        1:1 화상면접 ➡ {isReschedule ? "일정 변경" : "일정 예약"}
                    </h2>
                    <p>컨설턴트의 가용 슬롯을 선택하세요.</p>
                </div>

                <div className="booking-container">
                    {/* 왼쪽: 컨설턴트 정보 */}
                    <div className="consultant-info-box">
                        <h3>선택한 전문가 정보</h3>

                        {consultantLoading ? (
                            <p>컨설턴트 정보를 불러오는 중...</p>
                        ) : consultantError ? (
                            <p className="error-text">{consultantError}</p>
                        ) : (
                            <div className="consultant-card vertical">
                                <img className="consultant-avatar" src={display.photo} alt={`${display.name} 프로필`} />
                                <ul className="consultant-meta">
                                    <li><strong>이름:</strong> {display.name}</li>
                                    {/* ▼ 수정: 값이 '-'이면 '년'을 붙이지 않음 */}
                                    <li><strong>총 경력:</strong> {display.careerYears === "-" ? "-" : `${display.careerYears}년`}</li>
                                    <li><strong>소속:</strong> {display.organization}</li>
                                    <li><strong>전문분야:</strong> {display.specialty}</li>
                                    {/* ▼ 수정: 값이 '-'이면 '년'을 붙이지 않음 */}
                                    <li><strong>전문분야 경력:</strong> {display.specialtyYears === "-" ? "-" : `${display.specialtyYears}년`}</li>
                                    <li className="intro"><strong>소개글:</strong> {display.introduction}</li>
                                </ul>
                            </div>
                        )}
                    </div>

                    {/* 달력 */}
                    <div className="calendar">
                        <h3>{isReschedule ? "일정 변경" : "일정 예약"}</h3>
                        <p>예약하고자 하는 일정을 선택하세요.</p>

                        <div className="year-month-select">
                            <select value={selectedYear} onChange={(e) => setSelectedYear(Number(e.target.value))}>
                                {years.map((y) => (<option key={y} value={y}>{y}년</option>))}
                            </select>
                            <select value={selectedMonth} onChange={(e) => setSelectedMonth(Number(e.target.value))}>
                                {months.map((m, idx) => (<option key={idx} value={idx}>{m}</option>))}
                            </select>
                        </div>

                        <div className="weekdays-grid">
                            {weekDays.map((wd) => (<div key={wd} className="weekday">{wd}</div>))}
                        </div>

                        <div className="dates-grid">
                            {calendarDays.map((day, idx) => {
                                if (!day) return <div key={idx} className="date-item empty" />;
                                const isPast = isPastDate(selectedYear, selectedMonth, day);
                                const dateStr = `${selectedYear}-${String(selectedMonth + 1).padStart(2,"0")}-${String(day).padStart(2,"0")}`;

                                const hasFutureSlots = !isPast
                                    ? slots.some((s) => {
                                        const startIso = s.startAt ?? s.start;
                                        if (!startIso?.startsWith(dateStr)) return false;
                                        const sameDay =
                                            new Date(startIso).toDateString() ===
                                            new Date(selectedYear, selectedMonth, day).toDateString();
                                        return sameDay ? !isPastTime(startIso) : true;
                                    })
                                    : false;

                                const isSelected = selectedDate === day;

                                return (
                                    <div
                                        key={idx}
                                        className={`date-item ${hasFutureSlots ? "available" : "disabled"} ${isSelected ? "selected" : ""}`}
                                        onClick={() => hasFutureSlots && setSelectedDate(day)}
                                    >
                                        {day}
                                    </div>
                                );
                            })}
                        </div>

                        {slotsLoading && <div className="loading">일정을 불러오는 중...</div>}
                        {slotsError && <div className="error-text">{slotsError}</div>}
                        {!slotsLoading && !slotsError && slots.length === 0 && (
                            <div className="empty-text">예약 가능한 일정이 없습니다.</div>
                        )}
                    </div>

                    {/* 시간 선택 */}
                    <div className="time-selection">
                        <h3>시간 {isReschedule ? "변경" : "예약"}</h3>
                        {selectedDate ? (
                            <>
                                <p>{selectedYear}년 {selectedMonth + 1}월 {selectedDate}일을 선택하셨습니다.</p>
                                <select
                                    value={selectedSlot ? (selectedSlot.startAt ?? selectedSlot.start) : ""}
                                    onChange={(e) => {
                                        const val = e.target.value;
                                        const slot = slotsOfSelectedDate.find((s) => (s.startAt ?? s.start) === val);
                                        setSelectedSlot(slot || null);
                                    }}
                                >
                                    <option value="">{isReschedule ? "변경할 시간을 선택하세요." : "예약 시간을 선택하세요."}</option>
                                    {slotsOfSelectedDate
                                        .filter((s) => !isPastTime(s.startAt ?? s.start))
                                        .map((s) => {
                                            const startIso = s.startAt ?? s.start;
                                            const endIso   = s.endAt   ?? s.end;
                                            return (
                                                <option key={startIso} value={startIso}>
                                                    {fmtTime(startIso)} ~ {fmtTime(endIso)}
                                                </option>
                                            );
                                        })}
                                </select>

                                {selectedSlot && (
                                    <button className="confirm-btn" onClick={handleConfirm} disabled={submitting}>
                                        {submitting ? (isReschedule ? "변경 중..." : "결제 진행 중...") : (isReschedule ? "일정 변경" : "예약 요청")}
                                    </button>
                                )}
                            </>
                        ) : (
                            <p>날짜를 선택해주세요.</p>
                        )}
                    </div>
                </div>
            </div>
        </div>
    );
}
