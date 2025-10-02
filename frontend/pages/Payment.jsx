// src/pages/Payment.jsx
import { useEffect, useState } from "react";
import { useLocation, useNavigate } from "react-router-dom";
import api from "@/client/axios";

function loadPortOne() {
    return new Promise((res, rej) => {
        if (window.IMP) return res(window.IMP);
        const s = document.createElement("script");
        s.src = "https://cdn.iamport.kr/v1/iamport.js";
        s.onload = () => res(window.IMP);
        s.onerror = rej;
        document.head.appendChild(s);
    });
}

export default function Payment() {
    const { state } = useLocation(); // { consultantId, slots:[{startAt,endAt}] }
    const nav = useNavigate();
    const consultantId = state?.consultantId;
    const slots = Array.isArray(state?.slots) ? state.slots : [];

    const [price, setPrice] = useState(null);
    const [levelName, setLevelName] = useState("");

    // 가드
    useEffect(() => {
        if (!consultantId || slots.length === 0) nav(-1);
    }, [consultantId, slots, nav]);

    // ✅ 견적: 등급 기반 1회권 가격 (bundle 없이 호출)
    useEffect(() => {
        let alive = true;
        (async () => {
            try {
                // 서버에서 consultant.level을 읽어 1회권 가격을 리턴
                const r = await api.get("/booking/quote", { params: { consultantId } });
                if (!alive) return;
                const payload = r.data?.data || r.data;
                setPrice(Number(payload.totalPrice ?? payload.price));
                setLevelName(payload.levelName || payload.level || "");
            } catch (e) {
                console.error("quote error:", e?.response?.data || e.message);
            }
        })();
        return () => { alive = false; };
    }, [consultantId]);

    const pay = async () => {
        try {
            // 1) 체크아웃: bundle은 더 이상 의미 없지만, 호환 위해 "ONE" 또는 미전달
            const { data } = await api.post("/payments/checkout", {
                consultantId,
                slots,
                method: "card",
                // bundle: "ONE" // (서버가 필요 없으면 주석 유지)
            });
            const payload = data?.data || data; // { merchantUid, amount, name, buyerName }

            // 2) PortOne
            const IMP = await loadPortOne();
            const impCode = import.meta.env.VITE_IMP_CODE;
            const channelKey = import.meta.env.VITE_PORTONE_CHANNEL_KEY;
            if (!impCode || !channelKey) {
                alert("VITE_IMP_CODE / VITE_PORTONE_CHANNEL_KEY 환경변수를 확인하세요.");
                return;
            }
            IMP.init(impCode);
            IMP.request_pay(
                {
                    channelKey,
                    pay_method: "card",
                    merchant_uid: payload.merchantUid,
                    name: payload.name,                 // 예: "[Junior] 1회권 (컨설턴트: 김OO)"
                    amount: Number(payload.amount),     // 서버 계산금액
                    buyer_name: payload.buyerName ?? "사용자",
                    buyer_tel: "010-0000-0000",
                },
                async (rsp) => {
                    if (!rsp.success) return alert(`결제 실패: ${rsp.error_msg || "취소/오류"}`);
                    try {
                        await api.post("/payments/confirm", {
                            impUid: rsp.imp_uid,
                            merchantUid: rsp.merchant_uid,
                        });
                        alert("결제가 완료되었습니다. 예약이 확정되었습니다.");
                        nav("/MyPage");
                    } catch (e) {
                        const msg = e?.response?.data?.message || e.message || "확인 실패";
                        alert(`결제 승인 검증 실패: ${msg}`);
                    }
                }
            );
        } catch (e) {
            const msg = e?.response?.data?.message || e.message || "요청 실패";
            alert(`결제 요청 중 오류: ${msg}`);
        }
    };

    return (
        <div className="container">
            <h2>결제</h2>
            <p>{levelName ? `[${levelName}]` : ""} 1회권</p>
            <div className="card single">
                <div className="price">{price != null ? `${price.toLocaleString()} 원` : "..."}</div>
                <button onClick={pay} disabled={price == null}>결제하기</button>
            </div>
        </div>
    );
}
