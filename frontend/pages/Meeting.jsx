import React, { useState, useEffect, useRef } from "react";
import { useNavigate } from "react-router-dom";
import { OpenVidu } from "openvidu-browser";
import api from "../client/axios";
import "../components/Meeting.css";

const Meeting = () => {
    const navigate = useNavigate();

    // OpenVidu 상태
    const [OV, setOV] = useState(null);
    const [session, setSession] = useState(null);
    const [sessionId, setSessionId] = useState("");
    const [isConnected, setIsConnected] = useState(false);
    const [currentConnectionId, setCurrentConnectionId] = useState(null);
    const [currentUserName, setCurrentUserName] = useState(null);

    // 채팅 상태
    const [chatMessages, setChatMessages] = useState([]);
    const [chatInput, setChatInput] = useState("");

    const chatMessagesRef = useRef(null);
    const publisherRef = useRef(null);
    const chatSectionRef = useRef(null);

    // 드래그 상태
    const [isDragging, setIsDragging] = useState(false);
    const [dragOffset, setDragOffset] = useState({ x: 0, y: 0 });
    const [chatPosition, setChatPosition] = useState({ x: 0, y: 0 });

    // 사용자명 생성 (브라우저별 고유 ID)
    const generateUserName = () => {
        return `User_${Math.floor(Math.random() * 10000)}`;
    };

    // 세션 생성
    const createNewSession = async () => {
        try {
            const response = await api.post("/openvidu/sessions");
            console.log("✅ 세션 생성 성공:", response.data);
            return response.data;
        } catch (error) {
            console.error("❌ 세션 생성 실패:", error);
            throw error;
        }
    };

    // 토큰 발급
    const getToken = async (sessionId) => {
        try {
            const response = await api.post(
                `/openvidu/sessions/${sessionId}/connections`
            );
            console.log("✅ 토큰 발급 성공:", response.data);
            return response.data;
        } catch (error) {
            console.error("❌ 토큰 발급 실패:", error);
            throw error;
        }
    };

    // 새 세션 시작
    const handleJoinNewSession = async () => {
        try {
            const newSessionId = await createNewSession();
            setSessionId(newSessionId);
            const token = await getToken(newSessionId);
            await connectToSession(token);
        } catch (error) {
            alert("세션 생성에 실패했습니다: " + error.message);
        }
    };

    // 기존 세션 참여
    const handleJoinExistingSession = async () => {
        if (!sessionId) {
            alert("참여할 세션 ID를 입력해주세요.");
            return;
        }
        try {
            const token = await getToken(sessionId);
            await connectToSession(token);
        } catch (error) {
            alert("세션 참여에 실패했습니다: " + error.message);
        }
    };

    // 세션 연결
    const connectToSession = async (token) => {
        console.log("🔗 세션 연결 시작, 토큰:", token);
        const ov = new OpenVidu();
        setOV(ov);

        const newSession = ov.initSession();

        // 스트림 생성 이벤트
        newSession.on("streamCreated", (event) => {
            console.log("📹 새 스트림 생성:", event.stream);
            newSession.subscribe(event.stream, "subscriber-container");
        });

        // 스트림 삭제 이벤트
        newSession.on("streamDestroyed", (event) => {
            console.log("❌ 스트림 종료:", event.stream);
        });

        // 사용자 입장 알림 수신
        newSession.on("signal:user-joined", (event) => {
            const joinedUserName = event.data;
            setChatMessages((prev) => [
                ...prev,
                {
                    message: `>>${joinedUserName}<<님이 입장하셨습니다.`,
                    isSelf: false,
                    isSystem: true,
                    timestamp: new Date().toLocaleTimeString(),
                },
            ]);
        });

        // 채팅 메시지 수신 (본인 메시지 포함)
        newSession.on("signal:chat", (event) => {
            const senderName = event.from.data
                ? JSON.parse(event.from.data).clientData
                : "Unknown";
            const isSelf =
                event.from.connectionId === newSession.connection.connectionId;
            addChatMessage(event.data, isSelf, senderName);
        });

        const userName = generateUserName();
        setCurrentUserName(userName);
        console.log("👤 사용자명:", userName);

        try {
            // 세션 연결
            await newSession.connect(token, { clientData: userName });
            console.log(
                "✅ 세션 연결 성공! Connection ID:",
                newSession.connection.connectionId
            );

            setCurrentConnectionId(newSession.connection.connectionId);
            setSession(newSession);
            setIsConnected(true);

            // 입장 알림 전송
            setTimeout(() => {
                newSession
                    .signal({
                        type: "user-joined",
                        data: userName,
                    })
                    .catch((error) => console.error("입장 알림 전송 실패:", error));
            }, 500);
        } catch (error) {
            console.error("❌ 세션 연결 실패:", error);
            throw error;
        }
    };

    // 세션 종료
    const handleLeaveSession = () => {
        if (session) {
            session.disconnect();
        }
        setSession(null);
        setOV(null);
        setIsConnected(false);
        setChatMessages([]);
        setSessionId("");
    };

    // 채팅 메시지 추가
    const addChatMessage = (message, isSelf, senderName = currentUserName) => {
        setChatMessages((prev) => [
            ...prev,
            {
                message,
                isSelf,
                senderName,
                timestamp: new Date().toLocaleTimeString(),
            },
        ]);
    };

    // 채팅 메시지 전송
    const handleSendMessage = () => {
        if (!chatInput.trim() || !session) return;

        const messageToSend = chatInput;
        setChatInput(""); // 입력창 먼저 비우기

        // 상대방에게 전송 (내 화면에는 signal:chat 이벤트로 표시됨)
        session
            .signal({
                type: "chat",
                data: messageToSend,
            })
            .then(() => {
                console.log("메시지 전송 성공");
            })
            .catch((error) => {
                console.error("메시지 전송 실패:", error);
            });
    };

    // 채팅창 드래그 시작
    const handleChatDragStart = (e) => {
        // 채팅 제목 부분만 드래그 가능
        if (!e.target.classList.contains("chat-title")) {
            return;
        }
        e.preventDefault();
        console.log("드래그 시작");
        setIsDragging(true);
        setDragOffset({
            x: e.clientX - chatPosition.x,
            y: e.clientY - chatPosition.y,
        });
    };

    // 드래그 중
    useEffect(() => {
        const handleMouseMove = (e) => {
            if (!isDragging) return;
            setChatPosition({
                x: e.clientX - dragOffset.x,
                y: e.clientY - dragOffset.y,
            });
        };

        const handleMouseUp = () => {
            if (isDragging) {
                console.log("드래그 종료");
                setIsDragging(false);
            }
        };

        if (isDragging) {
            document.addEventListener("mousemove", handleMouseMove);
            document.addEventListener("mouseup", handleMouseUp);
            return () => {
                document.removeEventListener("mousemove", handleMouseMove);
                document.removeEventListener("mouseup", handleMouseUp);
            };
        }
    }, [isDragging, dragOffset, chatPosition]);

    // 채팅 메시지 자동 스크롤
    useEffect(() => {
        if (chatMessagesRef.current) {
            chatMessagesRef.current.scrollTop = chatMessagesRef.current.scrollHeight;
        }
    }, [chatMessages]);

    // isConnected가 true가 되면 Publisher 초기화
    useEffect(() => {
        if (isConnected && OV && session && !publisherRef.current) {
            const initPublisher = async () => {
                try {
                    console.log("Publisher 초기화 시작...");

                    const publisher = OV.initPublisher("publisher-container", {
                        audioSource: undefined,
                        videoSource: undefined,
                        publishAudio: true,
                        publishVideo: true,
                        resolution: "640x480",
                        frameRate: 30,
                        insertMode: "APPEND",
                        mirror: true,
                    });

                    publisher.on("videoElementCreated", (event) => {
                        console.log("Video element created:", event.element);
                    });

                    publisher.on("accessAllowed", () => {
                        console.log("Camera access allowed!");
                    });

                    publisher.on("accessDenied", () => {
                        console.error("Camera access denied!");
                    });

                    publisherRef.current = publisher;
                    await session.publish(publisher);
                    console.log("Publisher 게시 성공!");
                } catch (error) {
                    console.error("Publisher 초기화 실패:", error);
                }
            };

            initPublisher();
        }
    }, [isConnected, OV, session]);

    // 페이지 언마운트 시 세션 종료
    useEffect(() => {
        return () => {
            if (session) {
                session.disconnect();
            }
        };
    }, [session]);

    return (
        <div className="meeting-container">
            <header className="meeting-header">
                <h1>📞 1:1 화상면접</h1>
                <button className="exit-btn" onClick={() => navigate("/MyPage")}>
                    나가기
                </button>
            </header>

            {!isConnected ? (
                <div className="meeting-lobby">
                    <div className="session-input-group">
                        <label htmlFor="session-id-input">세션 ID (선택사항):</label>
                        <input
                            type="text"
                            id="session-id-input"
                            value={sessionId}
                            onChange={(e) => setSessionId(e.target.value)}
                            placeholder="세션 ID를 입력하거나 비워두세요"
                            disabled={isConnected}
                        />
                    </div>

                    <div className="button-group">
                        <button className="btn btn-primary" onClick={handleJoinNewSession}>
                            🆕 새 화상통화 시작
                        </button>
                        <button
                            className="btn btn-success"
                            onClick={handleJoinExistingSession}
                        >
                            🔗 기존 화상통화 참여
                        </button>
                    </div>
                </div>
            ) : (
                <div className="meeting-content">
                    <div className="video-chat-container">
                        {/* 비디오 영역 */}
                        <div className="video-section">
                            <div className="video-grid">
                                <div className="video-itemm">
                                    <h4>내 화면</h4>
                                    <div className="video-preview">
                                        <div id="publisher-container"></div>
                                    </div>
                                </div>
                                <div className="video-itemm">
                                    <h4>상대방 화면</h4>
                                    <div className="video-preview">
                                        <div id="subscriber-container"></div>
                                    </div>
                                </div>
                            </div>

                            <div className="call-controls">
                                <button className="btn btn-danger" onClick={handleLeaveSession}>
                                    📞 통화 종료
                                </button>
                            </div>
                        </div>

                        {/* 채팅 영역 */}
                        <div
                            className="chat-section"
                            ref={chatSectionRef}
                            style={{
                                transform: `translate(${chatPosition.x}px, ${chatPosition.y}px)`,
                            }}
                        >
                            <h3
                                className="chat-title"
                                onMouseDown={handleChatDragStart}
                                style={{
                                    cursor: isDragging ? "grabbing" : "grab",
                                    userSelect: "none",
                                }}
                            >
                                💬 실시간 채팅
                            </h3>
                            <div className="chat-container">
                                <div
                                    className="chat-messages"
                                    style={{ height: "250px" }}
                                    ref={chatMessagesRef}
                                >
                                    {chatMessages.length === 0 ? (
                                        <div className="chat-info">
                                            채팅을 시작하세요! 화상통화 중 실시간으로 메시지를
                                            주고받을 수 있습니다.
                                        </div>
                                    ) : (
                                        chatMessages.map((msg, index) => (
                                            <div
                                                key={index}
                                                className={`chat-message ${msg.isSelf ? "own" : ""} ${
                                                    msg.isSystem ? "system" : ""
                                                }`}
                                            >
                                                {msg.isSystem ? (
                                                    <div className="chat-system-message">
                                                        {msg.message}
                                                    </div>
                                                ) : (
                                                    <div className="chat-message-content">
                                                        <div className="chat-message-info">
                                                            {msg.senderName} • {msg.timestamp}
                                                        </div>
                                                        <div>{msg.message}</div>
                                                    </div>
                                                )}
                                            </div>
                                        ))
                                    )}
                                </div>
                                <div className="chat-input-container">
                                    <input
                                        type="text"
                                        id="chat-input"
                                        value={chatInput}
                                        onChange={(e) => setChatInput(e.target.value)}
                                        onKeyPress={(e) => e.key === "Enter" && handleSendMessage()}
                                        placeholder="메시지를 입력하세요..."
                                        maxLength={500}
                                    />
                                    <button id="chat-send-btn" onClick={handleSendMessage}>
                                        📤 전송
                                    </button>
                                </div>
                            </div>
                        </div>
                    </div>
                </div>
            )}
        </div>
    );
};

export default Meeting;