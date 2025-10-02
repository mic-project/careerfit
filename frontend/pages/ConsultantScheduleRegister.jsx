import React, { useEffect, useMemo, useState } from "react";
import "../components/ConsultantScheduleRegister.css";
import api from "@/client/axios";

export default function ConsultantScheduleRegister() {
    const today = new Date();
    const [selectedYear, setSelectedYear] = useState(today.getFullYear());
    const [selectedMonth, setSelectedMonth] = useState(today.getMonth());
    const [selectedDate, setSelectedDate] = useState(null);

    const [slots, setSlots] = useState([]);
    const [startTime, setStartTime] = useState("");
    const [endTime, setEndTime] = useState("");

    const formatTime12 = (hour24, minute) => {
        const hour = Number(hour24);
        const period = hour < 12 ? "오전" : "오후";
        const hour12 = hour % 12 === 0 ? 12 : hour % 12;
        return `${period} ${hour12}:${minute}`;
    };

    const toLocalYmd = (d) =>
        `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, "0")}-${String(d.getDate()).padStart(2, "0")}`;

    const monthStart = new Date(selectedYear, selectedMonth, 1);
    const monthEnd = new Date(selectedYear, selectedMonth + 1, 0);
    const fromStr = toLocalYmd(monthStart);
    const toStr = toLocalYmd(monthEnd);

    useEffect(() => {
        api
            .get(`/availability/me/slots`, { params: { from: fromStr, to: toStr } })
            .then((res) => setSlots(res.data?.data ?? res.data ?? []))
            .catch((e) => console.error("load my slots error:", e?.response?.data || e.message));
    }, [fromStr, toStr]);

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
        const sel = `${selectedYear}-${String(selectedMonth + 1).padStart(2, "0")}-${String(selectedDate).padStart(2, "0")}`;
        return slots.filter((s) => (s.startAt ?? s.start)?.startsWith(sel));
    }, [slots, selectedYear, selectedMonth, selectedDate]);

    const fmtTime = (iso) => {
        const d = new Date(iso);
        return `${String(d.getHours()).padStart(2, "0")}:${String(d.getMinutes()).padStart(2, "0")}`;
    };

    const handleSave = async () => {
        if (!selectedDate || !startTime || !endTime) {
            alert("날짜와 시간을 모두 선택해주세요.");
            return;
        }
        const dateStr = `${selectedYear}-${String(selectedMonth + 1).padStart(2, "0")}-${String(selectedDate).padStart(2, "0")}`;
        const startAt = `${dateStr}T${startTime}:00`;
        const endAt = `${dateStr}T${endTime}:00`;

        try {
            await api.post("/availability/me/slots", [{ startAt, endAt }], { withCredentials: true });
            alert("가용 일정이 저장되었습니다.");
            setStartTime("");
            setEndTime("");
            const res = await api.get(`/availability/me/slots`, { params: { from: fromStr, to: toStr } });
            setSlots(res.data?.data ?? res.data ?? []);
        } catch (e) {
            console.error("save slot error:", e?.response?.data || e.message);
            alert("일정 저장 중 오류가 발생했습니다: " + (e?.response?.data?.message || e.message));
        }
    };

    const handleDelete = async (slotId) => {
        if (!window.confirm("이 시간을 삭제하시겠습니까?")) return;
        try {
            await api.delete(`/availability/me/slots/${slotId}`, { withCredentials: true });
            alert("삭제되었습니다.");
            setSlots((prev) => prev.filter((s) => s.id !== slotId));
        } catch (e) {
            console.error("delete slot error:", e);
            alert("삭제 실패");
        }
    };

    const months = ["1월","2월","3월","4월","5월","6월","7월","8월","9월","10월","11월","12월"];
    const years = [today.getFullYear(), today.getFullYear() + 1];
    const weekDays = ["일", "월", "화", "수", "목", "금", "토"];

    return (
        <div className="home-container">
            <div className="doc-feedback-container">
                <div className="doc-feedback-header">
                    <h2>가용 일정 등록/삭제</h2>
                    <p>예약 가능한 날짜와 시간을 등록/삭제할 수 있습니다.</p>
                </div>

                <div className="booking-containerr">
                    <div className="calendarr">
                        <h3>날짜 선택</h3>
                        <div className="year-month-selectt">
                            <select value={selectedYear} onChange={(e) => setSelectedYear(Number(e.target.value))}>
                                {years.map((y) => (<option key={y} value={y}>{y}년</option>))}
                            </select>
                            <select value={selectedMonth} onChange={(e) => setSelectedMonth(Number(e.target.value))}>
                                {months.map((m, idx) => (<option key={idx} value={idx}>{m}</option>))}
                            </select>
                        </div>

                        <div className="weekdays-gridd">
                            {weekDays.map((wd) => (<div key={wd} className="weekdayy">{wd}</div>))}
                        </div>

                        <div className="dates-gridd">
                            {calendarDays.map((day, idx) => {
                                if (!day) return <div key={idx} className="date-itemm empty"></div>;
                                const dateObj = new Date(selectedYear, selectedMonth, day);
                                const isPast = dateObj <= new Date(today.getFullYear(), today.getMonth(), today.getDate());
                                const isSelected = selectedDate === day;
                                return (
                                    <div
                                        key={idx}
                                        className={`date-itemm ${isPast ? "disabled" : "available"} ${isSelected ? "selected" : ""}`}
                                        onClick={() => !isPast && setSelectedDate(day)}
                                    >
                                        {day}
                                    </div>
                                );
                            })}
                        </div>
                    </div>

                    <div className="time-selectionn">
                        <h3>시간 등록</h3>
                        {selectedDate ? (
                            <>
                                <p>{selectedYear}년 {selectedMonth + 1}월 {selectedDate}일</p>
                                <div className="time-select-wrapperr">
                                    <select value={startTime} onChange={(e) => setStartTime(e.target.value)}>
                                        <option value="">시작 시간</option>
                                        {Array.from({ length: 48 }).map((_, i) => {
                                            const hour = String(Math.floor(i / 2)).padStart(2, "0");
                                            const minute = i % 2 === 0 ? "00" : "30";
                                            const val = `${hour}:${minute}`;
                                            return (<option key={val} value={val}>{formatTime12(hour, minute)}</option>);
                                        })}
                                    </select>
                                    <p>~</p>
                                    <select value={endTime} onChange={(e) => setEndTime(e.target.value)}>
                                        <option value="">종료 시간</option>
                                        {Array.from({ length: 48 }).map((_, i) => {
                                            const hour = String(Math.floor(i / 2)).padStart(2, "0");
                                            const minute = i % 2 === 0 ? "00" : "30";
                                            const val = `${hour}:${minute}`;
                                            return (<option key={val} value={val}>{formatTime12(hour, minute)}</option>);
                                        })}
                                    </select>
                                </div>

                                <button className="confirm-btnn" onClick={handleSave}>저장</button>
                                <h3>등록된 시간</h3>
                                <ul>
                                    {slotsOfSelectedDate.map((s) => (
                                        <li key={s.id}>
                                            {fmtTime(s.startAt ?? s.start)} ~ {fmtTime(s.endAt ?? s.end)}
                                            <button className="delete-btnn" onClick={() => handleDelete(s.id)}>삭제</button>
                                        </li>
                                    ))}
                                </ul>
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
