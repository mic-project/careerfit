import React, { useState, useEffect } from "react";
import { Link } from "react-router-dom";
import "../components/MyPageEditProfile.css";
import DefaultProfile from "../assets/default-profile.png";
import api from "@/client/axios";

const ConsultantEditProfile = () => {
    const [file, setFile] = useState(null);
    const [preview, setPreview] = useState(null);

    const [password, setPassword] = useState("");
    const [confirmPassword, setConfirmPassword] = useState("");
    const [passwordMessage, setPasswordMessage] = useState("");
    const [showPassword, setShowPassword] = useState(false);

    const [name, setName] = useState("");
    const [phone, setPhone] = useState("");
    const [affiliation, setAffiliation] = useState(""); // 전송 시 company
    const [specialty, setSpecialty] = useState("");
    const [specialtyCareer, setSpecialtyCareer] = useState("");
    const [consultantCareer, setConsultantCareer] = useState("");
    const [introduction, setIntroduction] = useState("");
    const [formError, setFormError] = useState("");

    const handlePasswordChange = (e) => {
        const value = e.target.value;
        setPassword(value);
        const regex = /^(?=.*[a-z])(?=.*[A-Z])(?=.*\d).{8,}$/;
        setPasswordMessage(
            regex.test(value) ? "사용 가능한 비밀번호입니다."
                : "영문 대/소문자, 숫자를 포함하여 8자 이상 입력하세요."
        );
    };

    useEffect(() => {
        api.get("/consultants/me", { withCredentials: true }).then((res) => {
            const d = res.data?.data ?? res.data;
            setName(d?.name || "");
            setPhone(d?.phone || "");
            setAffiliation(d?.company || "");
            setSpecialty(d?.specialty || "");
            setSpecialtyCareer("");
            setConsultantCareer("");
            setIntroduction(d?.introduction || "");
            setPreview(d?.profileImage || null);
        });
    }, []);

    const handleSubmit = async (e) => {
        e.preventDefault();

        if (!name || !phone) {
            setFormError("이름과 전화번호는 필수 입력 사항입니다.");
            return;
        }
        if (password && password !== confirmPassword) {
            setFormError("비밀번호가 일치하지 않습니다.");
            return;
        }

        const nowYear = new Date().getFullYear();
        const toInt = (v) => (v === "" ? null : Number(v));
        const specYears = toInt(specialtyCareer);
        const consYears = toInt(consultantCareer);
        let careerStartYear = null;
        if (specYears) careerStartYear = specYears >= 1900 ? specYears : nowYear - specYears;
        else if (consYears) careerStartYear = consYears >= 1900 ? consYears : nowYear - consYears;

        const form = new FormData();
        form.append("name", name);
        form.append("phone", phone);
        form.append("company", affiliation);
        form.append("specialty", specialty);
        form.append("introduction", introduction);
        if (careerStartYear) form.append("careerStartYear", String(careerStartYear));
        if (file) form.append("profileImage", file);

        await api.put("/consultants/me", form, {
            withCredentials: true,
            headers: { "Content-Type": "multipart/form-data" },
        });

        alert("저장되었습니다.");
    };

    return (
        <div className="edit-profile-container">
            <header className="edit-header">
                <h2>회원정보 수정</h2>
                <p className="subtitle">아이디, 이메일은 변경할 수 없습니다.</p>
            </header>

            <form className="edit-form" onSubmit={handleSubmit}>
                {/* 프로필 사진 */}
                <div className="form-row">
                    <label>프로필 사진</label>
                    <div className="input-with-preview">
                        <img
                            src={preview || DefaultProfile}
                            alt="기본 프로필"
                            className="profile-preview"
                        />
                        <input
                            type="file"
                            accept="image/*"
                            onChange={(e) => {
                                const f = e.target.files?.[0];
                                if (f) {
                                    setFile(f);
                                    setPreview(URL.createObjectURL(f));
                                }
                            }}
                        />
                        <button
                            type="button"
                            className="reset-profile-btn"
                            onClick={() => { setFile(null); setPreview(null); }}
                        >
                            기본 이미지로
                        </button>
                    </div>
                </div>

                {/* 비밀번호 (전송 X, UI만) */}
                <div className="form-row">
                    <label>비밀번호</label>
                    <div className="input-with-btn">
                        <input
                            type={showPassword ? "text" : "password"}
                            value={password}
                            onChange={handlePasswordChange}
                            placeholder="변경할 비밀번호 입력 (미기입 시 유지)"
                        />
                        <button
                            type="button"
                            className="toggle-pw"
                            onClick={() => setShowPassword((s) => !s)}
                        >
                            {showPassword ? "숨기기" : "보기"}
                        </button>
                    </div>
                    {password && <p className="password-message">{passwordMessage}</p>}
                </div>

                {/* 비밀번호 확인 (전송 X) */}
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
                    <input type="text" value={name} onChange={(e) => setName(e.target.value)} placeholder="이름 입력" />
                </div>

                {/* 전화번호 */}
                <div className="form-row">
                    <label>전화번호</label>
                    <input type="tel" value={phone} onChange={(e) => setPhone(e.target.value)} placeholder="하이픈(-) 없이 입력" />
                </div>

                {/* 소속(affiliation -> company) */}
                <div className="form-row">
                    <label>소속</label>
                    <input type="text" value={affiliation} onChange={(e) => setAffiliation(e.target.value)} placeholder="소속 입력" />
                </div>

                {/* 전문분야 */}
                <div className="form-row">
                    <label>전문분야</label>
                    <input type="text" value={specialty} onChange={(e) => setSpecialty(e.target.value)} placeholder="예: 백엔드 개발자" />
                </div>

                {/* 전문분야 경력(화면만) */}
                <div className="form-row">
                    <label>전문분야 경력</label>
                    <input type="text" value={specialtyCareer} onChange={(e) => setSpecialtyCareer(e.target.value)} placeholder="년수 또는 YYYY" />
                </div>

                {/* 컨설턴트 경력(화면만) */}
                <div className="form-row">
                    <label>컨설턴트 경력</label>
                    <input type="text" value={consultantCareer} onChange={(e) => setConsultantCareer(e.target.value)} placeholder="년수 또는 YYYY" />
                </div>

                {/* 소개글 */}
                <div className="form-row">
                    <label>소개글</label>
                    <textarea value={introduction} onChange={(e) => setIntroduction(e.target.value)} placeholder="자유롭게 소개글을 작성해주세요." rows={5} />
                </div>

                {formError && <p className="error">{formError}</p>}

                <div className="form-actions">
                    <Link to="/mypage" className="btn cancel">취소</Link>
                    <button type="submit" className="btn primary">저장</button>
                </div>
            </form>
        </div>
    );
};

export default ConsultantEditProfile;
