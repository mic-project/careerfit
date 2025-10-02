import React, { useEffect, useMemo, useState } from "react";
import api from "@/client/axios";
import "../components/MyPagePayment.css";

const pick = (res) => res?.data?.data ?? res?.data ?? res;

/** 안전한 날짜 포맷 */
const fmtDT = (iso) => {
    if (!iso) return "-";
    const d = new Date(iso);
    return `${d.toLocaleDateString()} ${d.toLocaleTimeString([], {
        hour: "2-digit",
        minute: "2-digit",
    })}`;
};

/** 문자열에 숫자만 남기고 마지막 4자리 뽑기 */
const last4 = (v) => {
    if (!v) return "";
    const digits = String(v).replace(/\D/g, "");
    return digits.slice(-4);
};

export default function MyPagePayment() {
    const [items, setItems] = useState([]);
    const [selectedId, setSelectedId] = useState(null);
    const [loading, setLoading] = useState(true);
    const [err, setErr] = useState("");
    const [workingId, setWorkingId] = useState(null); // 취소 중인 orderId

    const load = async () => {
        setLoading(true);
        setErr("");
        try {
            // 서버가 "주문 + (결제 + 예약)"를 같이 주는 엔드포인트
            const r = await api.get("/mypage/orders");
            const orders = pick(r) || [];

            // 응답이 다양한 모양일 수 있으니 결제 정보 우선순위 정리
            // 1) order.cardBrand/cardLast4/paidAt
            // 2) order.payment / order.payments[0]
            const mapped = orders.map((o) => {
                const pay =
                    o.payment ||
                    (Array.isArray(o.payments) && o.payments.length > 0 ? o.payments[0] : {}) ||
                    {};

                const orderId = Number(o.orderId ?? o.id); // 어떤 필드를 쓰든 숫자로 고정
                const bundleCount = Number(o.bundleCount ?? 1);
                const title = bundleCount > 1 ? `${bundleCount}회권` : "1회권";
                const price = Number(o.totalPrice ?? pay.amount ?? 0);

                const used =
                    Array.isArray(o.appointments)
                        ? o.appointments.filter(
                            (a) => a.status === "APPROVED" || a.status === "COMPLETED"
                        ).length
                        : Number(o.usedCount ?? 0);

                const paidAt = o.paidAt || pay.paidAt || o.createdAt || pay.createdAt;
                const brand = o.cardBrand || pay.cardBrand || o.cardIssuer || pay.cardIssuer || "";
                const lastFour =
                    o.cardLast4 || pay.cardLast4 || last4(o.cardNumber || pay.cardNumber || "");

                // 취소 가능 여부는 서버 판단값을 그대로 사용하고, 없으면 기본 false
                const cancellable = !!(o.cancellable ?? pay.cancellable ?? false);

                return {
                    id: orderId,                // 🔴 취소/키/선택 모두 이 값 사용
                    title,
                    price,
                    validity: paidAt ? `결제일 : ${new Date(paidAt).toLocaleDateString()}` : "결제 완료",
                    used,
                    total: bundleCount,
                    cardBrand: brand,
                    cardLast4: lastFour,
                    method: o.paymentMethod || o.pgProvider || pay.pgProvider || "CARD",
                    paidAt,
                    refundStatus: o.refundStatus || pay.refundStatus || "NONE",
                    cancellable,
                };
            });

            setItems(mapped);
        } catch (e) {
            console.error("load payments error:", e?.response?.data || e.message);
            setErr(e?.response?.data?.message || e?.message || "불러오기 실패");
            setItems([]);
        } finally {
            setLoading(false);
        }
    };

    useEffect(() => {
        load();
    }, []);

    const sorted = useMemo(() => [...items].sort((a, b) => b.id - a.id), [items]);

    const cancelPayment = async (orderId) => {
        if (!orderId && orderId !== 0) {
            alert("주문 ID가 없습니다. 새로고침 후 다시 시도해주세요.");
            return;
        }
        if (workingId) return;
        if (!window.confirm("결제를 취소하시겠습니까?")) return;

        try {
            setWorkingId(orderId);
            await api.post(`/payments/${orderId}/cancel`, { reason: "사용자 취소" });
            alert("결제가 취소(환불) 처리되었습니다.");
            await load();
            setSelectedId(null);
        } catch (e) {
            const msg = e?.response?.data?.message || e?.message || "취소 실패";
            alert(msg);
        } finally {
            setWorkingId(null);
        }
    };

    if (loading) {
        return (
            <div className="payments-container">
                <h2>💳 결제 내역</h2>
                <p>불러오는 중...</p>
            </div>
        );
    }
    if (err) {
        return (
            <div className="payments-container">
                <h2>💳 결제 내역</h2>
                <p className="error">{err}</p>
            </div>
        );
    }

    return (
        <div className="payments-container">
            <h2>💳 결제 내역</h2>
            {sorted.length === 0 ? (
                <p className="empty">결제 내역이 없습니다.</p>
            ) : (
                <ul className="payments-list">
                    {sorted.map((p) => {
                        const selected = selectedId === p.id;
                        const cardLabel =
                            p.cardBrand || p.cardLast4
                                ? `${p.cardBrand || ""}${p.cardLast4 ? `(${p.cardLast4})` : ""}`
                                : p.method;

                        return (
                            <li
                                key={p.id}
                                className={`payment-item ${selected ? "selected" : ""}`}
                                onClick={() => setSelectedId(selected ? null : p.id)} // 🔵 클릭한 것만 열림
                                role="button"
                                tabIndex={0}
                                onKeyDown={(e) => {
                                    if (e.key === "Enter" || e.key === " ") setSelectedId(selected ? null : p.id);
                                }}
                            >
                                <div className="payment-info">
                                    <span className="title">{p.title}</span>
                                    <span className="price">{p.price.toLocaleString()}원</span>
                                </div>

                                <div className="payment-detail">
                                    <span className="validity">{p.validity}</span>
                                    <span className="usage">
                    사용 횟수 : {p.used} / {p.total}
                  </span>
                                </div>

                                {selected && (
                                    <div className="payment-history">
                                        <div className="history-header">
                                            <h4>결제 상세</h4>
                                            {p.cancellable ? (
                                                <button
                                                    className="btn cancell"
                                                    disabled={!!workingId}
                                                    onClick={(e) => {
                                                        e.stopPropagation();           // 🔴 상위 li 클릭 토글 방지
                                                        cancelPayment(p.id);           // 🔴 항상 orderId로 보냄
                                                    }}
                                                >
                                                    {workingId === p.id ? "취소 처리 중..." : "결제 취소"}
                                                </button>
                                            ) : (
                                                <span className={`refund-badge status-${String(p.refundStatus).toLowerCase()}`}>
                          {p.refundStatus === "NONE"
                              ? "취소 불가"
                              : p.refundStatus === "REQUESTED"
                                  ? "환불 요청"
                                  : p.refundStatus === "PARTIAL"
                                      ? "부분 환불"
                                      : p.refundStatus === "FULL"
                                          ? "전액 환불"
                                          : p.refundStatus}
                        </span>
                                            )}
                                        </div>

                                        <p>
                                            결제일시 : {fmtDT(p.paidAt)} <br />
                                            결제수단 : {cardLabel}
                                        </p>
                                    </div>
                                )}
                            </li>
                        );
                    })}
                </ul>
            )}
        </div>
    );
}