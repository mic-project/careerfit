import React, { useEffect, useRef, useState } from "react";
import { useLocation, useNavigate } from "react-router-dom";
import "../components/PracticeInterview.css";
import axios from "../client/axios";

const PracticeInterview = () => {
    const location = useLocation();
    const navigate = useNavigate();

    // 선택된 질문 배열 전달
    const { questionList = ["질문이 없습니다."] } = location.state || {};

    const [questionIndex, setQuestionIndex] = useState(
        Math.floor(Math.random() * questionList.length)
    );

    const [answeredIndices, setAnsweredIndices] = useState([]);

    const videoRef = useRef(null);
    const mediaRecorderRef = useRef(null);
    const recordedChunksRef = useRef([]);
    const [recording, setRecording] = useState(false);

    // 웹캠 연결
    useEffect(() => {
        const initCamera = async () => {
            try {
                const stream = await navigator.mediaDevices.getUserMedia({
                    video: true,
                    audio: true,
                });
                if (videoRef.current) {
                    videoRef.current.srcObject = stream;
                }
            } catch (err) {
                console.error("웹캠 접근 오류:", err);
                alert("웹캠 사용 권한이 필요합니다.");
            }
        };
        initCamera();
    }, []);

    const currentQuestion = questionList[questionIndex];

    // 녹화 시작
    const startRecording = () => {
        const stream = videoRef.current.srcObject;
        recordedChunksRef.current = []; // 초기화

        mediaRecorderRef.current = new MediaRecorder(stream);
        mediaRecorderRef.current.ondataavailable = (e) => {
            if (e.data.size > 0) {
                recordedChunksRef.current.push(e.data);
                console.log("📊 녹화 chunk 추가:", e.data.size, "bytes, 총:", recordedChunksRef.current.length, "개");
            }
        };
        mediaRecorderRef.current.start(100); // 100ms마다 데이터 수집
        setRecording(true);

        // 첫 질문이 시작되면 현재 질문을 answeredIndices에 추가
        if (!answeredIndices.includes(questionIndex)) {
            setAnsweredIndices([questionIndex]);
        }
    };

    // 녹화 중지 & S3 업로드
    const stopRecording = () => {
        if (!mediaRecorderRef.current) return;

        mediaRecorderRef.current.onstop = async () => {
            console.log("녹화 데이터 chunks 수:", recordedChunksRef.current.length);

            // Blob 생성
            const blob = new Blob(recordedChunksRef.current, { type: "video/webm" });
            console.log("📹 녹화된 Blob 크기:", blob.size, "bytes");

            if (blob.size === 0) {
                window.alert("녹화된 데이터가 없습니다. 다시 시도해주세요.");
                return;
            }

            // S3에 업로드
            try {
                const sessionId = `practice_${Date.now()}`;
                const formData = new FormData();
                formData.append("file", blob, `recording_${sessionId}.webm`);
                formData.append("sessionId", sessionId);

                console.log("🚀 S3 업로드 시작...");
                const response = await axios.post("/openvidu/upload-recording", formData, {
                    headers: {
                        "Content-Type": "multipart/form-data",
                    },
                });

                console.log("✅ S3 업로드 성공:", response.data);

                window.alert(
                    "녹화가 완료되었습니다.\n마이페이지 -> 연습 기록 에서 확인하실 수 있습니다."
                );
                recordedChunksRef.current = [];
                navigate("/MyPage");
            } catch (error) {
                console.error("❌ S3 업로드 실패:", error);
                window.alert("녹화 파일 업로드에 실패했습니다.");
                recordedChunksRef.current = [];
            }
        };

        mediaRecorderRef.current.stop();
        setRecording(false);
    };

    // 랜덤 다음 질문 (마지막 질문이면 자동 녹화 중지)
    const nextQuestion = () => {
        if (!recording) return;

        // 모든 질문이 끝났으면 자동 녹화 중지
        if (answeredIndices.length >= questionList.length) {
            stopRecording();
            return;
        }

        // 랜덤으로 다음 질문 선택 (이미 본 질문 제외)
        let randomIndex;
        do {
            randomIndex = Math.floor(Math.random() * questionList.length);
        } while (answeredIndices.includes(randomIndex));

        setQuestionIndex(randomIndex);
        setAnsweredIndices([...answeredIndices, randomIndex]);
    };

    return (
        <div className="interview-container">
            <h2>면접 질문</h2>
            <p className="question">{currentQuestion}</p>

            <div className="video-wrapper">
                <video ref={videoRef} autoPlay muted className="video-feed" />
            </div>

            <div className="controls">
                {!recording ? (
                    <button className="record-btn start" onClick={startRecording}>
                        녹화 시작
                    </button>
                ) : answeredIndices.length >= questionList.length ? (
                    <button className="record-btn stop" onClick={stopRecording}>
                        녹화 종료
                    </button>
                ) : (
                    <>
                        <button className="record-btn next" onClick={nextQuestion}>
                            다음 질문
                        </button>
                        <button className="record-btn stop" onClick={stopRecording}>
                            녹화 중지
                        </button>
                    </>
                )}
            </div>
        </div>
    );
};

export default PracticeInterview;
