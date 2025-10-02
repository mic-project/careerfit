import React, { useEffect, useMemo, useRef, useState } from "react";
import { useNavigate } from "react-router-dom";
import api from "@/client/axios";
import "../components/AdminPage.css";

// ✅ 한국어 라벨 ↔ 코드 변환
const toKLabel = (code) => (code === "CONSULTANT" ? "컨설턴트" : "회원");
const toCode = (labelOrCode) =>
    labelOrCode === "컨설턴트" || labelOrCode === "CONSULTANT" ? "CONSULTANT" : "USER";

const MyPage = () => {
    const navigate = useNavigate();
    const [me, setMe] = useState(null);
    const [loading, setLoading] = useState(true);

    // ✅ 더미 제거: 실제 검색 결과를 여기에 담음(최대 1명)
    const [users, setUsers] = useState([]);
    const [search, setSearch] = useState("");
    const typingTimer = useRef(null);

    // ===== 1) 관리자 가드 =====
    useEffect(() => {
        let alive = true;
        (async () => {
            try {
                const { data } = await api.get("/auth/me", { withCredentials: true });
                const meData = data?.data ?? data; // 래핑 대비
                if (!alive) return;
                setMe(meData);
                if (meData?.role !== "ADMIN") {
                    navigate("/", { replace: true });
                    return;
                }
            } catch {
                if (alive) navigate("/", { replace: true });
            } finally {
                if (alive) setLoading(false);
            }
        })();
        return () => {
            alive = false;
            if (typingTimer.current) clearTimeout(typingTimer.current);
        };
    }, [navigate]);

    // ===== 2) 이메일로 백엔드 검색 =====
    const fetchByEmail = async (email) => {
        if (!email) {
            setUsers([]);
            return;
        }
        try {
            // ⚠️ baseURL에 /api가 이미 붙어있으므로 여기서는 /admin/... 만 사용
            const { data } = await api.get("/admin/users/search", {
                params: { email },
                withCredentials: true,
            });
            if (data?.success && data?.data) {
                const u = data.data;
                setUsers([
                    {
                        id: u.id,
                        name: u.name,
                        email: u.email,
                        roleCode: u.role, // USER | CONSULTANT
                        role: toKLabel(u.role),
                    },
                ]);
            } else {
                setUsers([]);
            }
        } catch (e) {
            console.error(e);
            setUsers([]);
        }
    };

    // 입력 변화 -> 300ms 디바운스 후 검색
    const handleSearch = (e) => {
        const v = e.target.value.trim();
        setSearch(v);
        if (typingTimer.current) clearTimeout(typingTimer.current);
        typingTimer.current = setTimeout(() => fetchByEmail(v), 300);
    };

    // 기존 UI의 클라이언트 필터 로직 유지(추가적인 부분검색용)
    const filteredUsers = useMemo(() => {
        if (!search) return users;
        const q = search.toLowerCase();
        return users.filter(
            (u) =>
                (u.name || "").toLowerCase().includes(q) ||
                (u.email || "").toLowerCase().includes(q)
        );
    }, [users, search]);

    // ===== 3) 권한 토글 (서버 반영) =====
    const toggleRole = async (user) => {
        const nextRoleCode = user.roleCode === "CONSULTANT" ? "USER" : "CONSULTANT";
        try {
            const { data } = await api.put(
                `/admin/users/${user.id}/role`,
                null,
                { params: { role: nextRoleCode }, withCredentials: true }
            );
            if (data?.success && data?.data) {
                const updated = data.data;
                setUsers((prev) =>
                    prev.map((u) =>
                        u.id === user.id
                            ? {
                                ...u,
                                roleCode: updated.role,
                                role: toKLabel(updated.role),
                            }
                            : u
                    )
                );
            } else {
                alert(data?.message ?? "권한 변경에 실패했습니다.");
            }
        } catch (e) {
            console.error(e);
            alert("권한 변경 중 오류가 발생했습니다.");
        }
    };

    if (loading) return null;

    return (
        <div className="mypage-container">
            <h2>관리자페이지</h2>

            <div className="user-info">
                <img
                    className="avatar"
                    src={me?.profileImage || "https://via.placeholder.com/80x80.png?text=USER"}
                    alt="프로필"
                />
                <div className="info-text">
                    <div className="user-name">{me?.name ?? "회원"} 관리자님</div>
                    <div className="subscription"></div>
                    <div className="emaill">{me?.email ?? "-"}</div>
                </div>

                <button
                    className="edit-btn"
                    onClick={() => navigate("/MyPageEditProfile")}
                >
                    프로필 수정
                </button>
            </div>

            <div className="user-search-container">
                <input
                    type="text"
                    placeholder="회원 검색 (이메일)"
                    value={search}
                    onChange={handleSearch}
                    className="user-search-input"
                    onKeyDown={(e) => e.key === "Enter" && fetchByEmail(search)}
                />

                <div className="user-list-container">
                    <ul className="user-list">
                        {filteredUsers.length > 0 ? (
                            filteredUsers.map((user) => (
                                <li key={user.id} className="user-item">
                  <span>
                    {user.name} ({user.email}) - {user.role}
                  </span>
                                    <button
                                        className={
                                            user.roleCode === "CONSULTANT"
                                                ? "toggle-consultant-btn revoke"
                                                : "toggle-consultant-btn grant"
                                        }
                                        onClick={() => toggleRole(user)}
                                    >
                                        {user.roleCode === "CONSULTANT" ? "권한 해제" : "컨설턴트 권한 부여"}
                                    </button>
                                </li>
                            ))
                        ) : (
                            <li className="muted">검색 결과가 없습니다.</li>
                        )}
                    </ul>
                </div>
            </div>
        </div>
    );
};

export default MyPage;
