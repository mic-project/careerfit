import React, { useEffect, useMemo, useState } from "react";
import { Link, useNavigate } from "react-router-dom";
import "../components/MyPageEditProfile.css";
import DefaultProfile from "../assets/default-profile.png";
// import api from "../client/axios"; // 별칭(@) 없으면 이 줄 사용
import api from "@/client/axios";

const PW_REGEX = /^(?=.*[a-z])(?=.*[A-Z])(?=.*\d).{8,}$/;

const MyPageEditProfile = () => {
    const navigate = useNavigate();

    // 서버에서 가져온 내 정보
    const [me, setMe] = useState(null);
    const [loading, setLoading] = useState(true);

    // 프로필 파일/프리뷰
    const [profileFile, setProfileFile] = useState(null);
    const [profilePreview, setProfilePreview] = useState(null);

    // 입력값
    const [name, setName] = useState("");
    const [phone, setPhone] = useState("");

    // 비밀번호(선택)
    const [password, setPassword] = useState("");
    const [confirmPassword, setConfirmPassword] = useState("");
    const [passwordMessage, setPasswordMessage] = useState("");
    const [showPassword, setShowPassword] = useState(false);

    // 공통 에러/상태
    const [formError, setFormError] = useState("");
    const [saving, setSaving] = useState(false);

    // 초기 로딩
    useEffect(() => {
        let alive = true;
        (async () => {
            try {
                const { data } = await api.get("/auth/me", { withCredentials: true });
                const meData = data?.data ?? data;
                if (!alive) return;
                setMe(meData);
                setName(meData?.name ?? "");
                setPhone(meData?.phone ?? "");
                // 기존 이미지 프리뷰
                const src = meData?.profileImage || meData?.socialProfileImage || null;
                setProfilePreview(src);
            } catch (e) {
                const status = e?.response?.status;
                if (alive && (status === 401 || status === 403)) {
                    navigate("/", { replace: true });
                }
            } finally {
                if (alive) setLoading(false);
            }
        })();
        return () => {
            alive = false;
            if (profilePreview?.startsWith("blob:")) URL.revokeObjectURL(profilePreview);
        };
        // eslint-disable-next-line react-hooks/exhaustive-deps
    }, []);

    // 비밀번호 입력 체크
    const handlePasswordChange = (e) => {
        const value = e.target.value;
        setPassword(value);
        if (!value) {
            setPasswordMessage("");
            return;
        }
        setPasswordMessage(
            PW_REGEX.test(value)
                ? "사용 가능한 비밀번호입니다."
                : "영문 대/소문자, 숫자를 포함하여 8자 이상 입력하세요."
        );
    };

    // 전화번호 숫자만
    const handlePhone = (v) => {
        const onlyDigits = v.replace(/\D/g, "").slice(0, 11);
        setPhone(onlyDigits);
    };

    // 파일 선택
    const onPickFile = (file) => {
        if (!file) return;
        setProfileFile(file);
        const url = URL.createObjectURL(file);
        if (profilePreview?.startsWith("blob:")) URL.revokeObjectURL(profilePreview);
        setProfilePreview(url);
    };

    // 저장
    const handleSubmit = async (e) => {
        e.preventDefault();
        setFormError("");

        // 필수값
        if (!name || !phone) {
            setFormError("이름과 전화번호는 필수 입력 사항입니다.");
            return;
        }
        // 전화번호 길이
        if (phone.length < 10 || phone.length > 11) {
            setFormError("전화번호는 숫자만 10~11자리로 입력하세요.");
            return;
        }
        // 비밀번호 일치
        if (password !== confirmPassword) {
            setFormError("비밀번호가 일치하지 않습니다.");
            return;
        }
        // 비밀번호 규칙(입력했을 때만 검사)
        if (password && !PW_REGEX.test(password)) {
            setFormError("비밀번호 규칙을 확인해주세요.");
            return;
        }

        try {
            setSaving(true);

            // 기본: 멀티파트(파일 포함)
            const form = new FormData();
            form.append("name", name);
            form.append("phone", phone);
            if (password) form.append("password", password);
            if (profileFile) form.append("profileImage", profileFile);

            // 실제 업데이트 엔드포인트로 교체
            const UPDATE_ENDPOINT = "/users/me";

            await api.patch(UPDATE_ENDPOINT, form, {
                headers: { "Content-Type": "multipart/form-data" },
                withCredentials: true,
            });

            alert("회원정보가 저장되었습니다.");
            navigate("/MyPage");
        } catch (e) {
            const msg = e?.response?.data?.message || e?.message || "저장 실패";
            setFormError(msg);

            // ── 백엔드가 JSON만 받는다면 아래 대안 사용 ──
            // const payload = { name, phone };
            // if (password) payload.password = password;
            // await api.put(UPDATE_ENDPOINT, payload, { withCredentials: true });
        } finally {
            setSaving(false);
        }
    };

    if (loading) return null;

    const avatarSrc =
        profilePreview || me?.profileImage || me?.socialProfileImage || DefaultProfile;

    return (
        <div className="edit-profile-container">
            <header className="edit-header">
                <h2>회원정보 수정</h2>
                <p className="subtitle">아이디/이메일은 변경하지 않습니다.</p>
            </header>

            <form className="edit-form" onSubmit={handleSubmit}>
                {/* 프로필 사진 */}
                <div className="form-row">
                    <label>프로필 사진</label>
                    <div className="input-with-preview">
                        <img
                            src={avatarSrc}
                            alt="프로필 미리보기"
                            className="profile-preview"
                        />
                        <input
                            type="file"
                            accept="image/*"
                            onChange={(e) => onPickFile(e.target.files?.[0])}
                        />
                        <button
                            type="button"
                            className="reset-profile-btn"
                            onClick={() => {
                                if (profilePreview?.startsWith("blob:"))
                                    URL.revokeObjectURL(profilePreview);
                                setProfileFile(null);
                                setProfilePreview(null);
                            }}
                        >
                            기본 이미지로
                        </button>
                    </div>
                </div>

                {/* 비밀번호 */}
                <div className="form-row">
                    <label>비밀번호 (선택)</label>
                    <div className="input-with-btn">
                        <input
                            type={showPassword ? "text" : "password"}
                            value={password}
                            onChange={handlePasswordChange}
                            placeholder="변경 시만 입력 (대/소문자+숫자 8자+)"
                        />
                        <button
                            type="button"
                            className="toggle-pw"
                            onClick={() => setShowPassword((s) => !s)}
                        >
                            {showPassword ? "숨기기" : "보기"}
                        </button>
                    </div>
                    {password && (
                        <p
                            className={
                                PW_REGEX.test(password) ? "password-message success" : "password-message"
                            }
                        >
                            {passwordMessage}
                        </p>
                    )}
                </div>

                {/* 비밀번호 확인 */}
                <div className="form-row">
                    <label>비밀번호 확인</label>
                    <input
                        type="password"
                        value={confirmPassword}
                        onChange={(e) => setConfirmPassword(e.target.value)}
                        placeholder="비밀번호 확인 입력"
                    />
                    {confirmPassword &&
                        (confirmPassword === password ? (
                            <p className="success">비밀번호가 일치합니다.</p>
                        ) : (
                            <p className="error">비밀번호가 일치하지 않습니다.</p>
                        ))}
                </div>

                {/* 이름 */}
                <div className="form-row">
                    <label>이름</label>
                    <input
                        type="text"
                        value={name}
                        onChange={(e) => setName(e.target.value)}
                        placeholder="이름 입력"
                    />
                </div>

                {/* 전화번호 */}
                <div className="form-row">
                    <label>전화번호</label>
                    <input
                        type="tel"
                        value={phone}
                        onChange={(e) => handlePhone(e.target.value)}
                        placeholder="하이픈(-) 없이 입력 (숫자 10~11자리)"
                    />
                </div>

                {/* 전체 오류 */}
                {formError && <p className="error">{formError}</p>}

                {/* 버튼 */}
                <div className="form-actions">
                    <Link to="/MyPage" className="btn cancel">
                        취소
                    </Link>
                    <button
                        type="submit"
                        className="btn primary"
                        disabled={
                            saving ||
                            (password && (!PW_REGEX.test(password) || password !== confirmPassword))
                        }
                    >
                        {saving ? "저장 중..." : "저장"}
                    </button>
                </div>
            </form>
        </div>
    );
};

export default MyPageEditProfile;
