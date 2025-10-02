// DocReview.jsx

import React, { useState, useMemo } from "react";
import api from "@/client/axios";
import "../components/DocReview.css";

const byteLen = (str) => new Blob([str ?? ""]).size;

const DocReview = () => {
    const [inputText, setInputText] = useState("");
    const [file, setFile] = useState(null);
    const [loading, setLoading] = useState(false);
    const [result, setResult] = useState({ summary: "", suggestions: [] });

    const counts = useMemo(
        () => ({
            chars: inputText.length,
            bytesWithSpace: byteLen(inputText),
            bytesNoSpace: byteLen(inputText.replace(/\s/g, "")),
        }),
        [inputText]
    );

    const outText = useMemo(() => {
        const lines = [
            "[요약]",
            result.summary || "",
            "",
            "[개선 제안]",
            ...(result.suggestions || []).map(
                (s, i) => `${i + 1}. ${s.title}\n- ${s.detail}`
            ),
        ];
        return lines.join("\n");
    }, [result]);

    const outCounts = useMemo(
        () => ({
            chars: outText.length,
            bytesWithSpace: byteLen(outText),
            bytesNoSpace: byteLen(outText.replace(/\s/g, "")),
        }),
        [outText]
    );

    const handleCheck = async () => {
        setLoading(true);
        try {
            const form = new FormData();
            if (file) {
                form.append("file", file);
            } else {
                const blob = new Blob([inputText || ""], { type: "text/plain" });
                form.append(
                    "file",
                    new File([blob], "resume.txt", { type: "text/plain" })
                );
            }
            const options = { language: "ko", tone: "formal", suggestionCount: 5 };
            form.append(
                "options",
                new Blob([JSON.stringify(options)], { type: "application/json" })
            );

            const { data } = await api.post("/resume/analyze", form, {
                headers: { "Content-Type": "multipart/form-data" },
            });
            setResult({
                summary: data.summary || "",
                suggestions: data.suggestions || [],
            });
        } catch (e) {
            alert("분석 요청 실패: " + (e?.response?.data?.message || e.message));
        } finally {
            setLoading(false);
        }
    };

    return (
        <div className="doc-feedback-container">
            <div className="doc-feedback-header">
                <h2>AI 자기소개서 피드백</h2>
                <p>
                    파일 업로드 또는 아래 입력창에 텍스트를 붙여넣고 '점검하기'로
                    분석해보세요.
                </p>
            </div>

            <div className="doc-feedback-boxes">
                {/* LEFT */}
                <div className="input-box">
                    <input
                        type="file"
                        accept=".pdf,.doc,.docx,.txt"
                        onChange={(e) => setFile(e.target.files?.[0] || null)}
                        style={{ marginBottom: 10 }}
                    />
                    <textarea
                        placeholder="내용을 직접 작성하거나, 복사해서 붙여 넣어주세요."
                        value={inputText}
                        onChange={(e) => setInputText(e.target.value)}
                    />
                    <div className="bottom-row">
                        <div className="char-count">
                            글자수: {counts.chars} | 공백포함: {counts.bytesWithSpace} Byte |
                            공백제외: {counts.bytesNoSpace} Byte
                        </div>
                        <button
                            className="check-btn"
                            onClick={handleCheck}
                            disabled={loading}
                        >
                            {loading ? "점검중..." : "점검하기"}
                        </button>
                    </div>
                </div>
                {/* RIGHT */}

                <div className="output-box">
                    <textarea readOnly value={outText} />
                    <div className="bottom-row">
                        <div className="char-count-right">
                            {(() => {
                                const filteredText = outText
                                    .replace(/\[요약\]|\[개선 제안\]/g, "")
                                    .replace(/\r?\n/g, "");

                                return (
                                    <>
                                        글자수: {filteredText.length} | 공백포함:{" "}
                                        {byteLen(filteredText)} Byte | 공백제외:{" "}
                                        {byteLen(filteredText.replace(/\s/g, ""))} Byte
                                    </>
                                );
                            })()}
                        </div>
                        <button
                            className="clear-btn"
                            onClick={() => setResult({ summary: "", suggestions: [] })}
                        >
                            내용 삭제
                        </button>
                    </div>
                </div>
            </div>
        </div>
    );
};

export default DocReview;