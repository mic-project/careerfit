// ✅ 백엔드 서버 주소 (로컬 Spring Boot 애플리케이션)
const APPLICATION_SERVER_URL = "http://localhost:8080/";

// 🔧 즉시 실행 디버깅 로그
console.log("🎉 app-ultimate.js 파일이 로드되었습니다! (완전 최종 버전 - 비디오 재생 수정)");

// OpenVidu 객체
var OV;
var session;

// 채팅 관련 변수
var chatEnabled = false;
var currentConnectionId = null;
var currentUserName = null;

// DOM 요소 (안전하게 가져오기)
const joinBtn = document.getElementById('join-btn');
const joinExistingBtn = document.getElementById('join-existing-btn');
const leaveBtn = document.getElementById('leave-btn');
const sessionIdInput = document.getElementById('session-id-input');

// 이벤트 리스너 (요소가 존재할 때만 추가)
if (joinBtn) {
    joinBtn.addEventListener('click', () => joinSession(true));
}
if (joinExistingBtn) {
    joinExistingBtn.addEventListener('click', () => joinSession(false));
}
if (leaveBtn) {
    leaveBtn.addEventListener('click', leaveSession);
}
window.addEventListener('beforeunload', leaveSession);


async function joinSession(isNewSession) {
    try {
        if (isNewSession) {
            const sessionId = await createNewSession();
            sessionIdInput.value = sessionId;
            const token = await getToken(sessionId);
            await connectToSession(token, 'publisher');

            // 비디오 채팅 컨테이너 표시
            const videoChatContainer = document.getElementById('video-chat-container');
            if (videoChatContainer) {
                videoChatContainer.style.display = 'flex';
            }
        } else {
            const sessionId = sessionIdInput.value;
            if (!sessionId) {
                alert("참여할 세션 ID를 입력해주세요.");
                return;
            }
            const token = await getToken(sessionId);
            await connectToSession(token, 'publisher');

            // 비디오 채팅 컨테이너 표시
            const videoChatContainer = document.getElementById('video-chat-container');
            if (videoChatContainer) {
                videoChatContainer.style.display = 'flex';
            }
        }
    } catch (error) {
        console.error("세션 연결 중 오류 발생:", error);
        alert("세션 연결에 실패했습니다: " + error.message);
    }
}

async function connectToSession(token, publisherTarget = 'publisher') {
    return new Promise((resolve, reject) => {
        console.log("🔗 OpenVidu 세션 연결 시도...", token);

        OV = new OpenVidu();
        session = OV.initSession();

        // 새 사용자 스트림
        session.on('streamCreated', (event) => {
            console.log("👥 새로운 스트림 생성됨:", event.stream);
            session.subscribe(event.stream, 'subscriber');
        });

        session.on('streamDestroyed', () => {
            console.log("👋 상대방이 나갔습니다.");
        });

        session.on('exception', (exception) => {
            console.warn("⚠️ OpenVidu 예외:", exception);
        });

        session.on('connectionCreated', (event) => {
            console.log("🔗 연결 생성됨:", event.connection);
        });

        session.on('sessionDisconnected', (event) => {
            console.log("🔌 세션 연결 해제됨:", event);
        });

        // 채팅 메시지 수신 이벤트
        session.on('signal:chat', (event) => {
            console.log("💬 채팅 메시지 수신:", event);

            // 자신이 보낸 메시지는 무시 (이미 화면에 표시했으므로)
            if (event.from.connectionId === currentConnectionId) {
                console.log("🔄 자신의 메시지 무시 (중복 방지)");
                return;
            }

            // 상대방의 사용자명도 함께 전달
            const senderName = event.from.data ? JSON.parse(event.from.data).clientData : '상대방';
            displayChatMessage(event.data, event.from.connectionId, false, senderName);
        });

        // 브라우저별 고유 사용자명 생성
        const userName = generateUserName();
        currentUserName = userName;

        session.connect(token, { clientData: userName })
            .then(() => {
                console.log("✅ OpenVidu 세션 연결 성공!");

                // 현재 연결 ID 저장 (채팅에서 자신의 메시지 구분용)
                // session.connection을 통해 현재 연결 정보 가져오기
                if (session.connection) {
                    currentConnectionId = session.connection.connectionId;
                    console.log("🆔 현재 연결 ID:", currentConnectionId);
                } else {
                    console.warn("⚠️ 세션 연결 정보를 가져올 수 없습니다.");
                }

                // Publisher 초기화 전에 미디어 권한 확인
                console.log("🎥 Publisher 초기화 중...");

                const publisher = OV.initPublisher(publisherTarget, {
                    audioSource: undefined,
                    videoSource: undefined,
                    publishAudio: true,
                    publishVideo: true,
                    resolution: '640x480',
                    frameRate: 30,
                    insertMode: 'APPEND',
                    mirror: true
                });

                // Publisher 이벤트 리스너
                publisher.on('accessAllowed', () => {
                    console.log("✅ 미디어 액세스 허용됨");
                });

                publisher.on('accessDenied', () => {
                    console.error("❌ 미디어 액세스 거부됨");
                    reject(new Error("카메라/마이크 접근이 거부되었습니다."));
                });

                publisher.on('videoElementCreated', (event) => {
                    console.log("📹 비디오 요소 생성됨:", event.element);
                });

                publisher.on('streamCreated', (event) => {
                    console.log("🎬 Publisher 스트림 생성됨:", event.stream);
                });

                // 세션에 Publisher 게시
                session.publish(publisher)
                    .then(() => {
                        console.log("📡 Publisher 게시 성공!");

                        // 요소가 존재할 때만 disabled 설정
                        if (joinBtn) joinBtn.disabled = true;
                        if (joinExistingBtn) joinExistingBtn.disabled = true;
                        if (sessionIdInput) sessionIdInput.disabled = true;
                        if (leaveBtn) leaveBtn.disabled = false;

                        // 채팅 기능 활성화
                        enableChat();

                        resolve(); // 성공 시 resolve
                    })
                    .catch((error) => {
                        console.error("❌ Publisher 게시 실패:", error);
                        reject(new Error("비디오 스트림 게시에 실패했습니다: " + error.message));
                    });
            })
            .catch(error => {
                console.error('❌ 세션 연결 중 오류:', error.code, error.message);
                reject(new Error("세션 연결에 실패했습니다. 토큰이 올바른지 확인하세요."));
            });
    });
}

function leaveSession() {
    if (session) session.disconnect();
    session = null;
    OV = null;

    // 요소가 존재할 때만 disabled 설정
    if (joinBtn) joinBtn.disabled = false;
    if (joinExistingBtn) joinExistingBtn.disabled = false;
    if (sessionIdInput) sessionIdInput.disabled = false;
    if (leaveBtn) leaveBtn.disabled = true;
    // 요소가 존재할 때만 innerHTML 초기화
    const publisherElement = document.getElementById('publisher');
    const subscriberElement = document.getElementById('subscriber');
    if (publisherElement) publisherElement.innerHTML = '';
    if (subscriberElement) subscriberElement.innerHTML = '';

    // 비디오 채팅 컨테이너 숨기기
    const videoChatContainer = document.getElementById('video-chat-container');
    if (videoChatContainer) {
        videoChatContainer.style.display = 'none';
    }

    // 채팅 기능 비활성화
    disableChat();

    console.log('통화 종료');
}

// --- ✅ Spring Boot 백엔드 API를 호출하도록 수정 ---

async function createNewSession() {
    // ✅ 백엔드의 '/api/openvidu/sessions' 엔드포인트 호출
    const response = await fetch(APPLICATION_SERVER_URL + 'api/openvidu/sessions', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' }
        // ✅ body는 백엔드에서 필요 없으므로 제거 (필요 시 빈 객체 {} 전송)
    });
    if (!response.ok) throw new Error(`HTTP error! status: ${response.status}`);
    // ✅ 백엔드가 String(텍스트)를 반환하므로 .json()이 아닌 .text() 사용
    const sessionId = await response.text();
    console.log("새로운 세션 생성:", sessionId);
    return sessionId;
}

async function getToken(sessionId) {
    // ✅ 백엔드의 '/api/openvidu/sessions/{sessionId}/connections' 엔드포인트 호출
    // ✅ 'connection' -> 'connections' 오타 수정
    const response = await fetch(APPLICATION_SERVER_URL + 'api/openvidu/sessions/' + sessionId + '/connections', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' }
    });
    if (!response.ok) throw new Error(`HTTP error! status: ${response.status}`);
    // ✅ 백엔드가 String(텍스트)를 반환하므로 .json()이 아닌 .text() 사용
    const token = await response.text();
    console.log("토큰 수신:", token);
    return token;
}

// === 혼자 면접 연습하기 기능 (MediaRecorder API 사용) ===

let mediaRecorder = null; // MediaRecorder 인스턴스
let recordedChunks = []; // 녹화된 데이터 청크들
let currentSessionId = null; // 현재 세션 ID 저장
let isRecording = false; // 녹화 상태
let recordingMimeType = null; // 녹화에 사용된 MIME 타입 저장

// "혼자 면접 연습하기" 버튼 이벤트 리스너는 DOMContentLoaded에서 처리

/**
 * MediaRecorder를 사용한 개인 연습 세션을 시작합니다.
 */
async function startSoloPractice() {
    try {
        console.log("🎥 개인 면접 연습 세션을 시작합니다...");

        // 1. 일반 세션 생성 (녹화 설정 불필요)
        const sessionResponse = await fetch(APPLICATION_SERVER_URL + 'api/openvidu/sessions', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({})
        });

        if (!sessionResponse.ok) {
            throw new Error('백엔드에서 세션 생성에 실패했습니다.');
        }

        const sessionId = await sessionResponse.text();
        currentSessionId = sessionId;
        console.log("✅ 세션 생성 완료:", sessionId);

        // 2. 토큰 발급
        const token = await getToken(sessionId);
        console.log("✅ 토큰 발급 완료:", token);

        // 3. OpenVidu 세션에 접속 (solo practice용 publisher 사용)
        await connectToSession(token, 'solo-publisher');

        // 4. MediaRecorder로 클라이언트 사이드 녹화 시작
        await startClientRecording();

        // 5. UI 업데이트
        updateUIForPractice(true);

        alert(`🎬 혼자 연습하기가 시작되었습니다!\n세션 ID: ${sessionId}\n브라우저에서 녹화가 진행 중입니다.`);

    } catch (error) {
        console.error("❌ 개인 면접 연습 시작 중 오류 발생:", error);
        alert("세션 시작에 실패했습니다: " + error.message);
    }
}

/**
 * MediaRecorder를 사용하여 클라이언트 사이드 녹화를 시작합니다.
 */
async function startClientRecording() {
    try {
        // Publisher의 스트림을 가져오기
        const videoElement = document.querySelector('#solo-publisher video');
        if (!videoElement || !videoElement.srcObject) {
            throw new Error("비디오 스트림을 찾을 수 없습니다.");
        }

        const stream = videoElement.srcObject;
        console.log("🎬 비디오 스트림 획득:", stream);

        // MediaRecorder 설정 - WebM VP9 우선 사용 (재생 호환성 최우선)
        let options;

        // 🔄 WebM VP9를 최우선으로 시도 (재생 호환성 최우선)
        if (MediaRecorder.isTypeSupported('video/webm;codecs=vp9,opus')) {
            options = {
                mimeType: 'video/webm;codecs=vp9,opus',
                videoBitsPerSecond: 800000,  // 800kbps
                audioBitsPerSecond: 96000    // 96kbps 오디오
            };
            console.log("🎥 WebM VP9 사용 (재생 호환성 최우선)");
        } else if (MediaRecorder.isTypeSupported('video/webm;codecs=vp8,opus')) {
            options = {
                mimeType: 'video/webm;codecs=vp8,opus',
                videoBitsPerSecond: 700000,
                audioBitsPerSecond: 96000
            };
            console.log("🎥 WebM VP8 사용 (재생 호환성)");
        } else if (MediaRecorder.isTypeSupported('video/webm')) {
            options = {
                mimeType: 'video/webm',
                videoBitsPerSecond: 600000,
                audioBitsPerSecond: 96000
            };
            console.log("🎥 WebM 기본 사용");
        } else if (MediaRecorder.isTypeSupported('video/mp4;codecs=avc1.42E01E,mp4a.40.2')) {
            options = {
                mimeType: 'video/mp4;codecs=avc1.42E01E,mp4a.40.2',
                videoBitsPerSecond: 800000,
                audioBitsPerSecond: 96000
            };
            console.log("🎥 H.264 Main Profile 사용 (MP4 폴백)");
        } else if (MediaRecorder.isTypeSupported('video/mp4')) {
            options = {
                mimeType: 'video/mp4',
                videoBitsPerSecond: 600000,
                audioBitsPerSecond: 96000
            };
            console.log("🎥 MP4 기본 형식 사용 (폴백)");
        } else {
            // 마지막 폴백
            options = {
                videoBitsPerSecond: 600000
            };
            console.log("⚠️ 기본 설정 사용 (브라우저 자동 선택 - 위험)");
        }

        mediaRecorder = new MediaRecorder(stream, options);
        recordedChunks = [];

        // MediaRecorder가 생성된 후 실제 mimeType 저장
        recordingMimeType = mediaRecorder.mimeType;
        console.log("💾 실제 사용된 MIME 타입 저장:", recordingMimeType);

        // 이벤트 리스너 설정
        mediaRecorder.ondataavailable = (event) => {
            if (event.data.size > 0) {
                recordedChunks.push(event.data);
                console.log("📹 녹화 데이터 청크 추가:", event.data.size, "bytes");
            }
        };

        mediaRecorder.onstop = async () => {
            console.log("⏹️ MediaRecorder 중지됨");
            await handleRecordingComplete();
        };

        mediaRecorder.onerror = (event) => {
            console.error("❌ MediaRecorder 오류:", event.error);
        };

        // 녹화 시작 - fragmentation 완전 방지를 위해 매우 큰 청크 간격 사용
        mediaRecorder.start(10000); // 10초마다 데이터 청크 생성 (fragmentation 완전 방지)
        isRecording = true;
        console.log("🎥 클라이언트 사이드 녹화 시작!");

    } catch (error) {
        console.error("❌ 클라이언트 녹화 시작 실패:", error);
        throw error;
    }
}

/**
 * 녹화를 중지하고 파일을 생성합니다.
 */
async function stopClientRecording() {
    if (mediaRecorder && isRecording) {
        console.log("🛑 녹화 중지 중...");
        mediaRecorder.stop();
        isRecording = false;
    }
}

/**
 * 녹화 완료 후 처리 (파일 생성 및 업로드)
 */
async function handleRecordingComplete() {
    try {
        if (recordedChunks.length === 0) {
            throw new Error("녹화된 데이터가 없습니다.");
        }

        // Blob 생성
        // 저장된 MIME 타입 사용 (mediaRecorder가 null일 수 있음)
        const actualMimeType = recordingMimeType || 'video/webm'; // 폴백
        console.log("🎥 실제 녹화 형식:", actualMimeType);

        const recordedBlob = new Blob(recordedChunks, {
            type: actualMimeType
        });

        console.log("📦 녹화 파일 생성 완료:", {
            size: recordedBlob.size,
            type: recordedBlob.type,
            chunksCount: recordedChunks.length
        });

        // 기본 검증
        if (recordedBlob.size === 0) {
            throw new Error("녹화된 데이터가 비어있습니다.");
        }

        if (recordedChunks.length === 0) {
            throw new Error("녹화 데이터 청크가 없습니다.");
        }

        // 파일을 서버로 업로드
        await uploadRecordingToServer(recordedBlob, actualMimeType);

    } catch (error) {
        console.error("❌ 녹화 처리 중 오류:", error);
        alert("녹화 처리 중 오류가 발생했습니다: " + error.message);
    }
}

/**
 * 녹화 파일을 서버로 업로드합니다.
 */
async function uploadRecordingToServer(blob, mimeType) {
    const MAX_RETRIES = 3;
    const RETRY_DELAY = 1000; // 1초

    for (let attempt = 1; attempt <= MAX_RETRIES; attempt++) {
        try {
            console.log(`📤 업로드 시도 ${attempt}/${MAX_RETRIES}:`, { blobSize: blob.size, mimeType });

            // 최소 크기 검증 (50KB 미만이면 대기)
            if (blob.size < 50 * 1024) {
                console.warn("⚠️ 파일 크기가 너무 작음, 잠시 대기 후 재시도...");
                await new Promise(resolve => setTimeout(resolve, 500));
            }

            // MIME 타입에 따라 파일 확장자 결정
            let fileExtension = '.webm'; // 기본값
            if (mimeType.includes('mp4')) {
                fileExtension = '.mp4';
            } else if (mimeType.includes('webm')) {
                fileExtension = '.webm';
            }

            const formData = new FormData();
            const sessionId = currentSessionId || 'anonymous';
            const fileName = `practice-${sessionId}-${Date.now()}${fileExtension}`;
            formData.append('file', blob, fileName);
            formData.append('sessionId', sessionId);

            console.log("📤 서버로 녹화 파일 업로드 시작...", fileName);
            console.log("🎥 파일 형식:", mimeType);

            // 타임아웃 설정
            const controller = new AbortController();
            const timeoutId = setTimeout(() => controller.abort(), 30000); // 30초 타임아웃

            const response = await fetch(APPLICATION_SERVER_URL + 'api/openvidu/upload-recording', {
                method: 'POST',
                body: formData,
                signal: controller.signal
            });

            clearTimeout(timeoutId);

            if (!response.ok) {
                const errorText = await response.text().catch(() => '응답 본문 읽기 실패');
                throw new Error(`업로드 실패: ${response.status} ${response.statusText} - ${errorText}`);
            }

            const result = await response.text();
            console.log("✅ 녹화 파일 업로드 성공:", result);

            alert("🎉 녹화 파일이 성공적으로 S3에 업로드되었습니다!");

            // 녹화 목록 새로고침
            if (typeof loadRecordingsList === 'function') {
                setTimeout(() => loadRecordingsList(), 1000);
            }
            return; // 성공하면 함수 종료

        } catch (error) {
            console.error(`❌ 업로드 시도 ${attempt} 실패:`, error);

            if (attempt === MAX_RETRIES) {
                // 마지막 시도 실패
                alert(`❌ 녹화 파일 업로드에 실패했습니다 (${MAX_RETRIES}회 시도): ${error.message}`);
                return;
            }

            // 재시도 전 대기 (지수적 백오프)
            const delay = RETRY_DELAY * attempt;
            console.log(`⏳ ${delay}ms 후 재시도...`);
            await new Promise(resolve => setTimeout(resolve, delay));
        }
    }
}

/**
 * 혼자 연습하기를 종료합니다.
 */
async function endSoloPractice() {
    try {
        console.log("🛑 endSoloPractice 함수 시작");

        // 버튼 상태 확인
        const soloPracticeBtn = document.getElementById('solo-practice-btn');
        const currentState = soloPracticeBtn.dataset.state;
        console.log("🔍 현재 버튼 상태:", currentState);

        // practicing 상태가 아니면 실행하지 않음
        if (currentState !== 'practicing') {
            console.log("⚠️ 연습 중이 아니므로 endSoloPractice 무시 (현재 상태:", currentState, ")");
            return;
        }

        // 상태를 processing으로 변경
        soloPracticeBtn.dataset.state = 'processing';
        soloPracticeBtn.textContent = '⏳ 종료 중...';

        // 녹화 중지
        if (isRecording) {
            console.log("🛑 녹화 중지 중...");
            await stopClientRecording();
        }

        // 세션 종료
        console.log("🔌 세션 종료 중...");
        leaveSession();

        // 상태 초기화
        mediaRecorder = null;
        recordedChunks = [];
        currentSessionId = null;
        isRecording = false;

        // UI 업데이트 (반드시 마지막에)
        console.log("🔄 UI 상태를 준비 상태로 변경 중...");
        updateUIForPractice(false);

        console.log("🎬 혼자 연습하기 종료 완료");

    } catch (error) {
        console.error("❌ 연습 종료 중 오류:", error);
        alert("연습 종료 중 오류가 발생했습니다: " + error.message);

        // 에러 발생 시에도 UI 복원
        updateUIForPractice(false);
    }
}

/**
 * 연습 모드에 따른 UI 업데이트
 */
function updateUIForPractice(isPracticing) {
    const soloPracticeBtn = document.getElementById('solo-practice-btn');
    const joinBtn = document.getElementById('join-btn');
    const joinExistingBtn = document.getElementById('join-existing-btn');
    const leaveBtn = document.getElementById('leave-btn');
    const sessionIdInput = document.getElementById('session-id-input');

    console.log(`🔄 UI 상태 업데이트: isPracticing=${isPracticing}`);

    if (isPracticing) {
        soloPracticeBtn.textContent = '🛑 연습 종료';
        soloPracticeBtn.dataset.state = 'practicing';

        // 비디오 컨테이너 표시
        const videoContainer = document.getElementById('video-container');
        if (videoContainer) {
            videoContainer.style.display = 'block';
        }

        // 상태 표시기 업데이트
        const statusIndicator = document.getElementById('status-indicator');
        const statusText = document.getElementById('status-text');
        if (statusIndicator) {
            statusIndicator.className = 'status-indicator status-recording';
        }
        if (statusText) {
            statusText.textContent = '녹화 중';
        }

        // 요소가 존재할 때만 disabled 설정
        if (joinBtn) joinBtn.disabled = true;
        if (joinExistingBtn) joinExistingBtn.disabled = true;
        if (sessionIdInput) sessionIdInput.disabled = true;
        if (leaveBtn) leaveBtn.disabled = false;
        console.log("✅ UI를 연습 중 상태로 변경");
    } else {
        soloPracticeBtn.textContent = '🎥 면접 연습 시작하기';
        soloPracticeBtn.dataset.state = 'ready';

        // 비디오 컨테이너 숨기기
        const videoContainer = document.getElementById('video-container');
        if (videoContainer) {
            videoContainer.style.display = 'none';
        }

        // 상태 표시기 업데이트
        const statusIndicator = document.getElementById('status-indicator');
        const statusText = document.getElementById('status-text');
        if (statusIndicator) {
            statusIndicator.className = 'status-indicator status-ready';
        }
        if (statusText) {
            statusText.textContent = '준비됨';
        }

        // 요소가 존재할 때만 disabled 설정
        if (joinBtn) joinBtn.disabled = false;
        if (joinExistingBtn) joinExistingBtn.disabled = false;
        if (sessionIdInput) sessionIdInput.disabled = false;
        if (leaveBtn) leaveBtn.disabled = true;
        console.log("✅ UI를 준비 상태로 변경");
    }
}

// ====================== 녹화 목록 및 플레이어 기능 ======================

/**
 * 녹화 목록을 서버에서 가져와서 화면에 표시합니다.
 */
async function loadRecordingsList() {
    try {
        console.log("📋 녹화 목록 불러오는 중...");

        const recordingsList = document.getElementById('recordings-list');
        if (!recordingsList) {
            console.error("❌ recordings-list 엘리먼트를 찾을 수 없음");
            return;
        }

        recordingsList.innerHTML = '<p>녹화 목록을 불러오는 중...</p>';

        const response = await fetch('/api/openvidu/recordings');
        console.log(`📡 API 응답 상태: ${response.status} ${response.statusText}`);

        if (!response.ok) {
            throw new Error(`HTTP ${response.status}: ${response.statusText}`);
        }

        const recordings = await response.json();
        console.log("✅ 녹화 목록 로드 완료:", recordings.length, "개 파일");
        console.log("📄 녹화 목록 상세:", recordings);

        displayRecordingsList(recordings);

    } catch (error) {
        console.error("❌ 녹화 목록 로드 실패:", error);

        const recordingsList = document.getElementById('recordings-list');
        if (recordingsList) {
            recordingsList.innerHTML = '<p style="color: red;">녹화 목록을 불러오는데 실패했습니다: ' + error.message + '</p>';
        }
    }
}

/**
 * 녹화 목록을 화면에 표시합니다.
 */
function displayRecordingsList(recordings) {
    const recordingsList = document.getElementById('recordings-list');

    if (!recordings || recordings.length === 0) {
        recordingsList.innerHTML = '<p>아직 녹화된 영상이 없습니다.</p>';
        return;
    }

    let html = '';
    recordings.forEach((recording, index) => {
        const fileName = recording.key.split('/').pop();
        const fileSize = (recording.size / 1024 / 1024).toFixed(2); // MB 단위
        const lastModified = new Date(recording.lastModified).toLocaleString('ko-KR');

        html += `
            <div class="recording-item">
                <div class="recording-info">
                    <div class="recording-title">📹 ${fileName}</div>
                    <div class="recording-meta">
                        크기: ${fileSize} MB | 생성일: ${lastModified}
                    </div>
                </div>
                <button class="play-btn" onclick="playRecording('${recording.url}', '${fileName}')">
                    ▶️ 재생
                </button>
            </div>
        `;
    });

    recordingsList.innerHTML = html;
}

/**
 * 선택한 녹화 영상을 재생합니다.
 */
function playRecording(videoUrl, fileName) {
    try {
        console.log("🎬 영상 재생 시작:", fileName);
        console.log("📍 비디오 URL:", videoUrl);

        // URL 유효성 검사
        if (!videoUrl || videoUrl.trim() === '') {
            console.error("❌ 비디오 URL이 비어있음");
            alert("비디오 URL이 올바르지 않습니다.");
            return;
        }

        // 먼저 URL 접근성 테스트
        console.log("🔍 URL 접근성 테스트 시작...");
        fetch(videoUrl, { method: 'HEAD' })
            .then(response => {
                console.log("📡 URL 접근성 결과:", response.status, response.statusText);
                console.log("🔗 Response headers:", Array.from(response.headers.entries()));

                if (!response.ok) {
                    console.error("❌ URL 접근 불가:", response.status);
                    alert(`비디오 URL에 접근할 수 없습니다 (${response.status})`);
                    return;
                }

                // URL이 정상적이면 비디오 플레이어 설정
                setupVideoPlayer();
            })
            .catch(error => {
                console.error("❌ URL 접근성 테스트 실패:", error);
                console.log("⚠️ CORS 또는 네트워크 문제 가능, 직접 비디오 로드 시도");

                // CORS 문제일 수 있으니 직접 비디오 로드 시도
                setupVideoPlayer();
            });

        function setupVideoPlayer() {
            const videoPlayer = document.getElementById('video-player');
            const playbackVideo = document.getElementById('playback-video');

            if (!videoPlayer) {
                console.error("❌ video-player 엘리먼트를 찾을 수 없음");
                return;
            }

            if (!playbackVideo) {
                console.error("❌ playback-video 엘리먼트를 찾을 수 없음");
                return;
            }

            // 이전 이벤트 리스너 제거 (중복 방지)
            playbackVideo.removeEventListener('loadstart', handleLoadStart);
            playbackVideo.removeEventListener('canplay', handleCanPlay);
            playbackVideo.removeEventListener('error', handleError);
            playbackVideo.removeEventListener('loadedmetadata', handleLoadedMetadata);

            // 이전 비디오 정리
            playbackVideo.pause();
            playbackVideo.removeAttribute('src');
            playbackVideo.load();

            // fragmented MP4 지원을 위한 설정
            playbackVideo.preload = 'auto';
            playbackVideo.crossOrigin = 'anonymous';

            console.log("🔄 이전 비디오 정리 완료");

            // 플레이어 표시
            videoPlayer.style.display = 'block';

            // 새 이벤트 리스너 추가
            playbackVideo.addEventListener('loadstart', handleLoadStart);
            playbackVideo.addEventListener('canplay', handleCanPlay);
            playbackVideo.addEventListener('error', handleError);
            playbackVideo.addEventListener('loadedmetadata', handleLoadedMetadata);

            // fragmented MP4 지원을 위한 추가 이벤트
            playbackVideo.addEventListener('loadeddata', () => {
                console.log("📦 일부 데이터 로드 완료 - fragmented MP4 호환성 향상");
            });
            playbackVideo.addEventListener('canplaythrough', () => {
                console.log("🎬 전체 재생 가능 - fragmented MP4 완전 로드됨");
            });

            // progress 이벤트로 로딩 상태 확인
            playbackVideo.addEventListener('progress', function() {
                console.log("📊 비디오 로딩 진행률:", playbackVideo.buffered.length);
            });

            // 모든 파일에 대해 백엔드 프록시 사용 (안정성 및 일관성)
            const isWebM = fileName.toLowerCase().includes('.webm');
            const isMP4 = fileName.toLowerCase().includes('.mp4');

            // 🔄 모든 비디오 파일을 백엔드 프록시를 통해 스트리밍 (일관된 헤더와 Range 지원)
            console.log("🔄 모든 파일 - 백엔드 프록시 사용 (안정성 최우선) - v2");
            const proxyUrl = `/api/openvidu/recordings/video?url=${encodeURIComponent(videoUrl)}&t=${Date.now()}`;
            console.log("🔗 프록시 비디오 URL 설정 (캐시 방지):", proxyUrl);

            // DEMUXER 오류 방지를 위해 preload 강화
            playbackVideo.preload = 'auto';
            playbackVideo.src = proxyUrl;

            // 비디오 형식이 지원되지 않을 경우를 대비한 대안 제공 (모든 파일을 프록시 사용)
            const source = document.createElement('source');
            source.src = proxyUrl;
            if (isWebM) {
                source.type = 'video/webm';
            } else if (isMP4) {
                source.type = 'video/mp4';
            } else {
                source.type = 'video/webm'; // 기본값을 WebM으로
            }
            playbackVideo.appendChild(source);

            playbackVideo.load();

            // 만약 WebM이 지원되지 않으면 다운로드 링크 제공
            setTimeout(() => {
                if (playbackVideo.readyState === 0) {
                    console.log("⚠️ WebM 형식이 지원되지 않음. 다운로드 링크 제공");
                    const downloadLink = document.createElement('a');
                    downloadLink.href = videoUrl;
                    downloadLink.download = fileName;
                    downloadLink.textContent = `📥 ${fileName} 다운로드`;
                    downloadLink.style.display = 'block';
                    downloadLink.style.marginTop = '10px';
                    downloadLink.style.padding = '10px';
                    downloadLink.style.backgroundColor = '#007bff';
                    downloadLink.style.color = 'white';
                    downloadLink.style.textDecoration = 'none';
                    downloadLink.style.borderRadius = '5px';

                    const videoPlayerDiv = document.getElementById('video-player');
                    videoPlayerDiv.appendChild(downloadLink);

                    alert("브라우저에서 WebM 형식을 재생할 수 없습니다. 다운로드 링크를 제공했습니다.");
                }
            }, 3000);

            console.log("✅ 비디오 플레이어 설정 완료");

            // 플레이어 위치로 스크롤
            videoPlayer.scrollIntoView({ behavior: 'smooth' });

            // 2초 후 상태 확인
            setTimeout(() => {
                console.log("🔍 2초 후 비디오 상태:");
                console.log("- readyState:", playbackVideo.readyState);
                console.log("- networkState:", playbackVideo.networkState);
                console.log("- currentSrc:", playbackVideo.currentSrc);
                console.log("- videoWidth:", playbackVideo.videoWidth);
                console.log("- videoHeight:", playbackVideo.videoHeight);
                console.log("- duration:", playbackVideo.duration);

                // 자동 재생 시도
                playbackVideo.play().then(() => {
                    console.log("▶️ 자동 재생 성공");
                }).catch(error => {
                    console.log("⚠️ 자동 재생 실패 (사용자 상호작용 필요):", error.message);
                    console.log("👆 사용자가 직접 재생 버튼을 클릭해야 합니다");
                });
            }, 2000);
        }

        function handleLoadStart() {
            console.log("📥 비디오 로딩 시작");
        }

        function handleCanPlay() {
            console.log("▶️ 비디오 재생 준비 완료");
        }

        function handleError(e) {
            const playbackVideo = document.getElementById('playback-video');
            console.error("❌ 비디오 로드 오류:", e);
            console.error("오류 세부사항:", playbackVideo.error);
            if (playbackVideo.error) {
                console.error("- 오류 코드:", playbackVideo.error.code);
                console.error("- 오류 메시지:", playbackVideo.error.message);
            }
            alert("비디오 로드 중 오류가 발생했습니다. 콘솔에서 세부사항을 확인하세요.");
        }

        function handleLoadedMetadata() {
            const playbackVideo = document.getElementById('playback-video');
            console.log("📋 비디오 메타데이터 로드 완료");
            console.log("- 해상도:", playbackVideo.videoWidth + "x" + playbackVideo.videoHeight);
            console.log("- 재생 시간:", playbackVideo.duration + "초");
        }

    } catch (error) {
        console.error("❌ 영상 재생 오류:", error);
        alert("영상 재생 중 오류가 발생했습니다: " + error.message);
    }
}

/**
 * 비디오 플레이어를 닫습니다.
 */
function closeVideoPlayer() {
    const videoPlayer = document.getElementById('video-player');
    const playbackVideo = document.getElementById('playback-video');

    if (playbackVideo) {
        // 비디오 정지 및 URL 제거
        playbackVideo.pause();
        playbackVideo.removeAttribute('src');
        playbackVideo.load(); // 비디오 요소 리셋
    }

    if (videoPlayer) {
        // 플레이어 숨김
        videoPlayer.style.display = 'none';
    }

    console.log("🔄 비디오 플레이어 닫힌");
}

// ====================== 페이지 로드 시 초기화 ======================

/**
 * 페이지가 로드되면 실행되는 초기화 함수
 */
// 🔧 즉시 실행 함수로 이벤트 리스너 등록
function initializeApp() {
    console.log("🚀 앱 초기화 시작...");

    // 혼자 면접 연습하기 버튼
    const soloPracticeBtn = document.getElementById('solo-practice-btn');
    if (soloPracticeBtn) {
        // 기존 이벤트 리스너 제거
        soloPracticeBtn.onclick = null;

        soloPracticeBtn.addEventListener('click', function(event) {
            event.preventDefault();
            event.stopPropagation();

            const state = this.dataset.state || 'ready';
            console.log(`🔍 버튼 클릭됨 - 현재 상태: ${state}, 버튼 텍스트: "${this.textContent}"`);

            if (state === 'ready') {
                console.log("🎥 연습 시작 버튼 클릭됨");
                startSoloPractice();
            } else if (state === 'practicing') {
                console.log("🛑 연습 종료 버튼 클릭됨");
                endSoloPractice();
            } else {
                console.log("⚠️ 처리 중이므로 클릭 무시");
            }
        });

        // 초기 상태 설정
        soloPracticeBtn.dataset.state = 'ready';
        console.log("✅ Solo practice 버튼 이벤트 리스너 등록 완료");
    }

    // 녹화 목록 새로고침 버튼
    const refreshBtn = document.getElementById('refresh-recordings-btn');
    if (refreshBtn) {
        refreshBtn.addEventListener('click', loadRecordingsList);
        console.log("✅ 새로고침 버튼 이벤트 리스너 등록 완료");
    }

    // 모든 영상 삭제 버튼
    const deleteAllBtn = document.getElementById('delete-all-recordings-btn');
    if (deleteAllBtn) {
        deleteAllBtn.addEventListener('click', deleteAllRecordings);
        console.log("✅ 모든 영상 삭제 버튼 이벤트 리스너 등록 완료");
    }

    // 플레이어 닫기 버튼
    const closePlayerBtn = document.getElementById('close-player-btn');
    if (closePlayerBtn) {
        closePlayerBtn.addEventListener('click', closeVideoPlayer);
        console.log("✅ 플레이어 닫기 버튼 이벤트 리스너 등록 완료");
    }

    // 페이지 로드 시 녹화 목록 자동 로드
    console.log("📋 페이지 로드 시 녹화 목록 자동 로드 시작...");
    loadRecordingsList();

    console.log("✅ 모든 이벤트 리스너 등록 완료");
}

// ====================== 채팅 기능 ======================

/**
 * 채팅 기능을 활성화합니다.
 */
function enableChat() {
    console.log("💬 채팅 기능 활성화");
    chatEnabled = true;

    // 채팅은 이미 video-chat-container와 함께 표시됨

    // 채팅 입력 이벤트 리스너 설정
    setupChatEventListeners();

    // 환영 메시지 표시
    displaySystemMessage("채팅이 활성화되었습니다! 실시간으로 메시지를 주고받을 수 있습니다.");
}

/**
 * 채팅 기능을 비활성화합니다.
 */
function disableChat() {
    console.log("💬 채팅 기능 비활성화");
    chatEnabled = false;

    // 채팅은 video-chat-container와 함께 숨겨짐

    // 채팅 내용 초기화
    const chatMessages = document.getElementById('chat-messages');
    if (chatMessages) {
        chatMessages.innerHTML = `
            <div class="chat-info">
                채팅을 시작하세요! 화상통화 중 실시간으로 메시지를 주고받을 수 있습니다.
            </div>
        `;
    }

    // 입력창 초기화
    const chatInput = document.getElementById('chat-input');
    if (chatInput) {
        chatInput.value = '';
    }

    // 연결 ID 초기화
    currentConnectionId = null;
}

/**
 * 채팅 이벤트 리스너를 설정합니다.
 */
function setupChatEventListeners() {
    const chatInput = document.getElementById('chat-input');
    const chatSendBtn = document.getElementById('chat-send-btn');

    if (chatInput && chatSendBtn) {
        // 전송 버튼 클릭 이벤트
        chatSendBtn.addEventListener('click', sendChatMessage);

        // Enter 키 이벤트
        chatInput.addEventListener('keypress', function(event) {
            if (event.key === 'Enter' && !event.shiftKey) {
                event.preventDefault();
                sendChatMessage();
            }
        });

        console.log("✅ 채팅 이벤트 리스너 설정 완료");
    }
}

/**
 * 채팅 메시지를 전송합니다.
 */
function sendChatMessage() {
    if (!chatEnabled || !session) {
        console.warn("⚠️ 채팅이 비활성화되어 있거나 세션이 없습니다.");
        return;
    }

    const chatInput = document.getElementById('chat-input');
    if (!chatInput) return;

    const message = chatInput.value.trim();
    if (message === '') {
        console.warn("⚠️ 빈 메시지는 전송할 수 없습니다.");
        return;
    }

    try {
        // OpenVidu Signal API를 사용하여 메시지 전송
        session.signal({
            data: message,
            type: 'chat',
            to: [] // 모든 참가자에게 전송
        }).then(() => {
            console.log("📤 채팅 메시지 전송 성공:", message);

            // 자신의 메시지 화면에 표시
            displayChatMessage(message, currentConnectionId, true, currentUserName);

            // 입력창 초기화
            chatInput.value = '';
        }).catch((error) => {
            console.error("❌ 채팅 메시지 전송 실패:", error);
            alert("메시지 전송에 실패했습니다: " + error.message);
        });

    } catch (error) {
        console.error("❌ 채팅 메시지 전송 중 오류:", error);
        alert("메시지 전송 중 오류가 발생했습니다: " + error.message);
    }
}

/**
 * 채팅 메시지를 화면에 표시합니다.
 */
function displayChatMessage(message, senderConnectionId, isOwnMessage, senderName) {
    const chatMessages = document.getElementById('chat-messages');
    if (!chatMessages) return;

    // 첫 번째 메시지인 경우 안내 메시지 제거
    const chatInfo = chatMessages.querySelector('.chat-info');
    if (chatInfo) {
        chatInfo.remove();
    }

    // 메시지 요소 생성
    const messageElement = document.createElement('div');
    messageElement.className = `chat-message ${isOwnMessage ? 'own' : ''}`;

    const now = new Date();
    const timeString = now.toLocaleTimeString('ko-KR', {
        hour: '2-digit',
        minute: '2-digit'
    });

    const displayName = isOwnMessage ? (currentUserName || '나') : (senderName || '상대방');

    messageElement.innerHTML = `
        <div class="chat-message-content">
            <div class="chat-message-info">${displayName} • ${timeString}</div>
            <div class="chat-message-text">${escapeHtml(message)}</div>
        </div>
    `;

    // 메시지 추가
    chatMessages.appendChild(messageElement);

    // 스크롤을 맨 아래로 이동
    chatMessages.scrollTop = chatMessages.scrollHeight;

    console.log(`💬 메시지 표시: ${displayName} - ${message}`);
}

/**
 * 시스템 메시지를 표시합니다.
 */
function displaySystemMessage(message) {
    const chatMessages = document.getElementById('chat-messages');
    if (!chatMessages) return;

    // 첫 번째 메시지인 경우 안내 메시지 제거
    const chatInfo = chatMessages.querySelector('.chat-info');
    if (chatInfo) {
        chatInfo.remove();
    }

    const systemElement = document.createElement('div');
    systemElement.className = 'chat-info';
    systemElement.style.margin = '10px 0';
    systemElement.style.fontSize = '0.9em';
    systemElement.style.color = '#28a745';
    systemElement.textContent = message;

    chatMessages.appendChild(systemElement);

    // 스크롤을 맨 아래로 이동
    chatMessages.scrollTop = chatMessages.scrollHeight;
}

/**
 * 브라우저별 고유 사용자명을 생성합니다.
 */
function generateUserName() {
    // localStorage에서 기존 사용자명 확인
    let userName = localStorage.getItem('openvidu_username');

    if (!userName) {
        // 브라우저 정보를 기반으로 사용자명 생성
        const browserInfo = getBrowserInfo();
        const randomNum = Math.floor(Math.random() * 1000);
        userName = `${browserInfo}_${randomNum}`;

        // localStorage에 저장 (같은 브라우저에서 일관된 이름 사용)
        localStorage.setItem('openvidu_username', userName);
    }

    console.log("👤 생성된 사용자명:", userName);
    return userName;
}

/**
 * 브라우저 정보를 간단하게 가져옵니다.
 */
function getBrowserInfo() {
    const userAgent = navigator.userAgent;

    if (userAgent.includes('Chrome') && !userAgent.includes('Edge')) {
        return 'Chrome';
    } else if (userAgent.includes('Firefox')) {
        return 'Firefox';
    } else if (userAgent.includes('Safari') && !userAgent.includes('Chrome')) {
        return 'Safari';
    } else if (userAgent.includes('Edge')) {
        return 'Edge';
    } else {
        return 'Browser';
    }
}

/**
 * HTML 특수문자를 이스케이프합니다.
 */
function escapeHtml(text) {
    const div = document.createElement('div');
    div.textContent = text;
    return div.innerHTML;
}

// DOMContentLoaded 이벤트 리스너
document.addEventListener('DOMContentLoaded', initializeApp);

// 모든 영상 삭제 함수
async function deleteAllRecordings() {
    if (!confirm("정말로 모든 녹화 영상을 삭제하시겠습니까?\n\n⚠️ 이 작업은 취소할 수 없습니다!")) {
        return;
    }

    try {
        console.log("🗑️ 모든 녹화 영상 삭제 요청...");

        const response = await fetch(APPLICATION_SERVER_URL + 'api/openvidu/recordings/clear-all', {
            method: 'DELETE'
        });

        if (response.ok) {
            const message = await response.text();
            console.log("✅ 삭제 성공:", message);
            alert(message);

            // 목록 새로고침
            loadRecordingsList();
        } else {
            const errorMessage = await response.text();
            console.error("❌ 삭제 실패:", errorMessage);
            alert("삭제 실패: " + errorMessage);
        }
    } catch (error) {
        console.error("❌ 삭제 요청 중 오류:", error);
        alert("삭제 중 오류가 발생했습니다: " + error.message);
    }
}

// 만약 DOM이 이미 로드된 경우를 대비한 백업
if (document.readyState === 'loading') {
    console.log("🔄 DOM 로딩 중, DOMContentLoaded 대기...");
} else {
    console.log("🔄 DOM 이미 로드됨, 즉시 초기화 실행...");
    initializeApp();
}