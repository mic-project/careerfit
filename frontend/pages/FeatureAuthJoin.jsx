import React, { useState, useEffect, useMemo } from "react";
import api from "@/client/axios";
import { useNavigate } from "react-router-dom";
import "../components/FeatureAuthJoin.css";
import MainLogo from "../assets/MainLogo.png";

const FORM_KEY = "signup_draft";

const FeatureAuthJoin = () => {
    const navigate = useNavigate();

    // --- 소셜 신규 가입 플래그 (SuccessHandler가 ?isNew=1로 보냄)
    const isSocial = useMemo(() => {
        const params = new URLSearchParams(window.location.search);
        return params.get("isNew") === "1";
    }, []);

    // --- 일반 가입 폼 상태
    const [id, setId] = useState("");
    const [password, setPassword] = useState("");
    const [email, setEmail] = useState("");
    const [verificationCode, setVerificationCode] = useState("");
    const [name, setName] = useState("");
    const [birthDate, setBirthDate] = useState("");
    const [gender, setGender] = useState("");
    const [phoneNumber, setPhoneNumber] = useState("");
    const [idMessage, setIdMessage] = useState("");
    const [passwordMessage, setPasswordMessage] = useState("");

    // 이메일 인증 관련
    const [emailSuccess, setEmailSuccess] = useState("");
    const [emailError, setEmailError] = useState("");
    const [verifySuccess, setVerifySuccess] = useState("");
    const [verifyError, setVerifyError] = useState("");

    // 결과 메시지
    const [signUpSuccess, setSignUpSuccess] = useState("");
    const [signUpError, setSignUpError] = useState("");

    // 1) 폼 자동 복원 (소셜 모드일 때는 굳이 복원할 필요 없지만 유지해도 무해)
    useEffect(() => {
        try {
            const saved = JSON.parse(localStorage.getItem(FORM_KEY) || "{}");
            if (saved.id) setId(saved.id);
            if (saved.password) setPassword(saved.password);
            if (saved.name) setName(saved.name);
            if (saved.birthDate) setBirthDate(saved.birthDate);
            if (saved.gender) setGender(saved.gender);
            if (saved.phoneNumber) setPhoneNumber(saved.phoneNumber);
            if (saved.email) setEmail(saved.email);
        } catch {}
    }, []);

    // 2) 폼 자동 저장
    useEffect(() => {
        const draft = { id, password, name, birthDate, gender, phoneNumber, email };
        localStorage.setItem(FORM_KEY, JSON.stringify(draft));
    }, [id, password, name, birthDate, gender, phoneNumber, email]);

    // 3) 이메일 인증(일반 가입용) – 소셜 모드에선 무시돼요
    useEffect(() => {
        const params = new URLSearchParams(window.location.search);
        if (params.get("verified") === "1") {
            setVerifySuccess("이메일 인증이 완료되었습니다. 가입을 마무리해 주세요.");
            const e = params.get("email");
            if (e) {
                setEmail(e);
                localStorage.setItem("verifiedEmail", e);
            }
            localStorage.setItem("emailVerified", "1");
        }

        const onStorage = (ev) => {
            if (ev.key === "emailVerified" && ev.newValue === "1") {
                setVerifySuccess(
                    "이메일 인증이 완료되었습니다. 가입을 마무리해 주세요."
                );
                const e2 = localStorage.getItem("verifiedEmail");
                if (e2) setEmail(e2);
            }
        };
        window.addEventListener("storage", onStorage);
        return () => window.removeEventListener("storage", onStorage);
    }, []);

    // --- 검증/핸들러들 (일반 가입용)
    const handleIdChange = (e) => {
        const value = e.target.value;
        setId(value);
        const regex = /^(?=.*[a-z])(?=.*\d)[a-z\d]{5,}$/;
        setIdMessage(
            regex.test(value)
                ? "사용 가능한 아이디입니다."
                : "영문 소문자, 숫자를 포함하여 5자 이상 입력하세요."
        );
    };

    const handlePasswordChange = (e) => {
        const value = e.target.value;
        setPassword(value);
        const regex = /^(?=.*[a-z])(?=.*[A-Z])(?=.*\d).{8,}$/;
        setPasswordMessage(
            regex.test(value)
                ? "사용 가능한 비밀번호입니다."
                : "영문 대/소문자, 숫자를 포함하여 8자 이상 입력하세요."
        );
    };

    const sendVerificationEmail = async () => {
        setEmailError("")
        setEmailSuccess("");
        if (!email) {
            setEmailError("이메일을 입력하세요.");
            return;
        }
        try {
            const res = await api.get("auth/check-email", { params: { email } });
            const body = res?.data ?? {};
            const payload = body.data ?? body.result ?? body;
            const available =
                payload?.available ??
                payload?.isAvailable ??
                payload?.availableEmail ??
                null;

            if (available === false) {
                setEmailError(
                    "이미 가입된 이메일입니다. 로그인 또는 비밀번호 찾기를 이용해 주세요."
                );
                return;
            }

            const send = await api.post("auth/email/send", { email });
            if (send.status === 200) {
                setEmailSuccess(
                    "인증 메일을 보냈습니다. 메일함에서 링크를 눌러 인증을 완료해 주세요."
                );
            } else {
                setEmailError("이메일 전송 실패: 상태코드 " + send.status);
            }
        } catch (e) {
            const msg = e?.response?.data?.message ?? e?.message ?? "서버 오류";
            setEmailError("이메일 전송 실패: " + msg);
        }
    };

    // --- 일반 가입 인증 완료 플래그
    const isVerified =
        !!verifySuccess || localStorage.getItem("emailVerified") === "1";

    // --- 제출
    const handleSignUp = async () => {
        setSignUpError("");
        setSignUpSuccess("");

        try {
            if (isSocial) {
                // ✅ 소셜 최종 가입: 서버가 HttpOnly 쿠키(SOCIAL_JOIN)를 읽어 처리
                await api.post("auth/signup/social", {
                    agreeTos: "Y",
                    phone: phoneNumber,
                    nickName: id || name || "회원",
                });

                // 토큰 쿠키는 서버가 내려줌 → 홈으로 이동
                localStorage.removeItem(FORM_KEY);
                localStorage.removeItem("emailVerified");
                localStorage.removeItem("verifiedEmail");
                localStorage.setItem("userName", name || id || "회원");
                window.location.replace("/MainPage"); // 뒤로가기 방지
                return;
            }

            // ✅ 일반(이메일/비번) 가입
            if (!id || !password || !name || !email || !birthDate || !gender) {
                setSignUpError("모든 필수 정보를 입력하고 이메일 인증을 완료해주세요.");
                return;
            }
            if (!isVerified) {
                setSignUpError("이메일 인증을 먼저 완료해 주세요.");
                return;
            }

            const response = await api.post("auth/signup", {
                email,
                password,
                passwordConfirm: password,
                name,
                phone: phoneNumber,
                birthDate,
                username: id,
            });

            if (response.status === 200) {
                localStorage.setItem("userName", name);
                localStorage.removeItem("emailVerified");
                localStorage.removeItem("verifiedEmail");
                localStorage.removeItem(FORM_KEY);
                navigate("/Login"); // 라우트 대소문자는 앱 기준으로 통일
            }
        } catch (error) {
            setSignUpError(
                "회원가입 실패: " + (error.response?.data?.message || "서버 오류")
            );
        }
    };

    return (
        <>
            <header className="main-header">
                <div
                    className="logo"
                    onClick={() => navigate("/")}
                    style={{ cursor: "pointer" }}
                >
                    <img src={MainLogo} alt="CareerFit 로고" />
                </div>
            </header>

            <div className="signup-wrapper">
                <div className="signup-container">
                    <h2 className="signup-title">
                        {isSocial ? "소셜 회원가입" : "회원가입"}
                    </h2>

                    {/* 소셜 모드에서는 ID/비밀번호/이메일 인증을 숨깁니다 */}
                    {!isSocial && (
                        <>
                            <label>아이디</label>
                            <input
                                type="text"
                                value={id}
                                onChange={handleIdChange}
                                placeholder="5자 이상의 영문 소문자, 숫자만 사용"
                                autoComplete="username"
                            />
                            {idMessage && (
                                <p
                                    className={
                                        idMessage.includes("사용 가능")
                                            ? "success-message"
                                            : "error-message"
                                    }
                                >
                                    {idMessage}
                                </p>
                            )}

                            <label>비밀번호</label>
                            <input
                                type="password"
                                value={password}
                                onChange={handlePasswordChange}
                                placeholder="8자 이상의 영문 대/소문자, 숫자 포함"
                                autoComplete="new-password"
                            />
                            {passwordMessage && (
                                <p
                                    className={
                                        passwordMessage.includes("사용 가능")
                                            ? "success-message"
                                            : "error-message"
                                    }
                                >
                                    {passwordMessage}
                                </p>
                            )}
                        </>
                    )}

                    {/* 공통 추가정보 */}
                    <label>이름 (실명)</label>
                    <input
                        type="text"
                        placeholder="이름 입력"
                        value={name}
                        onChange={(e) => setName(e.target.value)}
                    />

                    <label>생년월일</label>
                    <input
                        type="date"
                        value={birthDate}
                        onChange={(e) => setBirthDate(e.target.value)}
                    />

                    <label>성별</label>
                    <div className="gender-section">
                        <input
                            type="radio"
                            id="male"
                            name="gender"
                            value="M"
                            checked={gender === "M"}
                            onChange={(e) => setGender(e.target.value)}
                        />
                        <label htmlFor="male">남성</label>

                        <input
                            type="radio"
                            id="female"
                            name="gender"
                            value="F"
                            checked={gender === "F"}
                            onChange={(e) => setGender(e.target.value)}
                        />
                        <label htmlFor="female">여성</label>
                    </div>

                    <label>전화번호</label>
                    <input
                        type="text"
                        placeholder="하이픈(-) 없이 입력"
                        value={phoneNumber}
                        onChange={(e) => setPhoneNumber(e.target.value)}
                        inputMode="numeric"
                    />

                    {/* 일반 가입에서만 이메일 인증 영역 표시 */}
                    {!isSocial && (
                        <div className="email-verify-group">
                            <label>이메일 본인인증</label>
                            <div className="email-section">
                                <input
                                    type="email"
                                    placeholder="example@email.com"
                                    value={email}
                                    onChange={(e) => setEmail(e.target.value)}
                                    autoComplete="email"
                                />
                                <button
                                    className="btn"
                                    type="button"
                                    onClick={sendVerificationEmail}
                                >
                                    인증
                                </button>
                            </div>
                            {emailSuccess && (
                                <p className="success-message">{emailSuccess}</p>
                            )}
                            {emailError && <p className="error-message">{emailError}</p>}

                            {verifySuccess && (
                                <p className="success-message">{verifySuccess}</p>
                            )}
                            {verifyError && <p className="error-message">{verifyError}</p>}
                        </div>
                    )}

                    <button className="signup-btnn" onClick={handleSignUp}>
                        {isSocial ? "소셜 가입 완료" : "회원가입"}
                    </button>

                    {signUpSuccess && <p className="success-message">{signUpSuccess}</p>}
                    {signUpError && <p className="error-message">{signUpError}</p>}
                </div>
            </div>
        </>
    );
};

export default FeatureAuthJoin;
