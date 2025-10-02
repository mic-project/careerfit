import React, { useState } from "react";
import { useLocation, useNavigate } from "react-router-dom";
import "../components/MyPageEvaluate.css";

const MyPageEvaluate = () => {
    const location = useLocation();
    const navigate = useNavigate();
    const { schedule } = location.state || {};

    const [rating, setRating] = useState(0); // 1~5점

    if (!schedule) {
        return <p>잘못된 접근입니다.</p>;
    }

    const handleSubmit = () => {
        if (rating === 0) {
            alert("별점을 선택해주세요.");
            return;
        }

        // TODO: 백엔드 API 호출
        console.log("평가 제출:", {
            consultant: schedule.consultant,
            rating,
            scheduleId: schedule.id,
        });

        alert("평가가 제출되었습니다!");
        navigate("/MyPageSchedule"); // 제출 후 일정 페이지로 이동
    };

    return (
        <div className="schedule-container">
            <h2>컨설턴트 평가</h2>
            <p className="subtitle">컨설턴트: {schedule.consultant}</p>

            <div className="rating-stars">
                {[1, 2, 3, 4, 5].map((star) => (
                    <span
                        key={star}
                        className={`star ${star <= rating ? "filled" : ""}`}
                        onClick={() => setRating(star)}
                    >
            ★
          </span>
                ))}
            </div>

            <button className="btn evaluatee" onClick={handleSubmit}>
                제출
            </button>
        </div>
    );
};

export default MyPageEvaluate;
