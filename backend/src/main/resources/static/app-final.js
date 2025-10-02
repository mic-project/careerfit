// ✅ 백엔드 서버 주소 (로컬 Spring Boot 애플리케이션)
const APPLICATION_SERVER_URL = "http://localhost:8080/";

// 🔧 즉시 실행 디버깅 로그
console.log("🚀 app-final.js 파일이 로드되었습니다! (최종 수정 버전 - 버튼 상태 수정)");

// OpenVidu 객체
var OV;
var session;

// DOM 요소
const joinBtn = document.getElementById('join-btn');
const joinExistingBtn = document.getElementById('join-existing-btn');
const leaveBtn = document.getElementById('leave-btn');
const sessionIdInput = document.getElementById('session-id-input');

// 이벤트 리스너
joinBtn.addEventListener('click', () => joinSession(true));
joinExistingBtn.addEventListener('click', () => joinSession(false));
leaveBtn.addEventListener('click', leaveSession);
window.addEventListener('beforeunload', leaveSession);


async function joinSession(isNewSession) {
    try {
        if (isNewSession) {
            const sessionId = await createNewSession();
            sessionIdInput.value = sessionId;
            const token = await getToken(sessionId);
            await connectToSession(token);
        } else {
            const sessionId = sessionIdInput.value;
            if (!sessionId) {
                alert("참여할 세션 ID를 입력해주세요.");
                return;
            }
            const token = await getToken(sessionId);
            await connectToSession(token);
        }
    } catch (error) {
        console.error("세션 연결 중 오류 발생:", error);
        alert("세션 연결에 실패했습니다: " + error.message);
    }
}

async function connectToSession(token) {
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

        session.connect(token, { clientData: 'User ' + Math.floor(Math.random() * 100) })
            .then(() => {
                console.log("✅ OpenVidu 세션 연결 성공!");

                // Publisher 초기화 전에 미디어 권한 확인
                console.log("🎥 Publisher 초기화 중...");

                const publisher = OV.initPublisher('publisher', {
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

                        joinBtn.disabled = true;
                        joinExistingBtn.disabled = true;
                        sessionIdInput.disabled = true;
                        leaveBtn.disabled = false;

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

    joinBtn.disabled = false;
    joinExistingBtn.disabled = false;
    sessionIdInput.disabled = false;
    leaveBtn.disabled = true;
    document.getElementById('publisher').innerHTML = '';
    document.getElementById('subscriber').innerHTML = '';
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

        // 3. OpenVidu 세션에 접속
        await connectToSession(token);

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
        const videoElement = document.querySelector('#publisher video');
        if (!videoElement || !videoElement.srcObject) {
            throw new Error("비디오 스트림을 찾을 수 없습니다.");
        }

        const stream = videoElement.srcObject;
        console.log("🎬 비디오 스트림 획득:", stream);

        // MediaRecorder 설정
        const options = {
            mimeType: 'video/webm;codecs=vp9', // 또는 'video/mp4'
            videoBitsPerSecond: 2500000 // 2.5 Mbps
        };

        // 브라우저 호환성 체크
        if (!MediaRecorder.isTypeSupported(options.mimeType)) {
            options.mimeType = 'video/webm'; // 폴백
        }

        mediaRecorder = new MediaRecorder(stream, options);
        recordedChunks = [];

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

        // 녹화 시작
        mediaRecorder.start(1000); // 1초마다 데이터 청크 생성
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
        const recordedBlob = new Blob(recordedChunks, {
            type: 'video/webm'
        });

        console.log("📦 녹화 파일 생성 완료:", {
            size: recordedBlob.size,
            type: recordedBlob.type
        });

        // 파일을 서버로 업로드
        await uploadRecordingToServer(recordedBlob);

    } catch (error) {
        console.error("❌ 녹화 처리 중 오류:", error);
        alert("녹화 처리 중 오류가 발생했습니다: " + error.message);
    }
}

/**
 * 녹화 파일을 서버로 업로드합니다.
 */
async function uploadRecordingToServer(blob) {
    try {
        const formData = new FormData();
        const fileName = `practice-${currentSessionId}-${Date.now()}.webm`;
        formData.append('file', blob, fileName);
        formData.append('sessionId', currentSessionId);

        console.log("📤 서버로 녹화 파일 업로드 시작...", fileName);

        const response = await fetch(APPLICATION_SERVER_URL + 'api/openvidu/upload-recording', {
            method: 'POST',
            body: formData
        });

        if (!response.ok) {
            throw new Error(`업로드 실패: ${response.status}`);
        }

        const result = await response.text();
        console.log("✅ 녹화 파일 업로드 성공:", result);

        alert("🎉 녹화 파일이 성공적으로 S3에 업로드되었습니다!");

        // 녹화 목록 새로고침
        if (typeof loadRecordingsList === 'function') {
            setTimeout(() => loadRecordingsList(), 1000);
        }

    } catch (error) {
        console.error("❌ 녹화 파일 업로드 실패:", error);
        alert("녹화 파일 업로드에 실패했습니다: " + error.message);
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
        joinBtn.disabled = true;
        joinExistingBtn.disabled = true;
        sessionIdInput.disabled = true;
        leaveBtn.disabled = false;
        console.log("✅ UI를 연습 중 상태로 변경");
    } else {
        soloPracticeBtn.textContent = '🎥 혼자 면접 연습하기';
        soloPracticeBtn.dataset.state = 'ready';
        joinBtn.disabled = false;
        joinExistingBtn.disabled = false;
        sessionIdInput.disabled = false;
        leaveBtn.disabled = true;
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

            console.log("🔄 이전 비디오 정리 완료");

            // 플레이어 표시
            videoPlayer.style.display = 'block';

            // 새 이벤트 리스너 추가
            playbackVideo.addEventListener('loadstart', handleLoadStart);
            playbackVideo.addEventListener('canplay', handleCanPlay);
            playbackVideo.addEventListener('error', handleError);
            playbackVideo.addEventListener('loadedmetadata', handleLoadedMetadata);

            // progress 이벤트로 로딩 상태 확인
            playbackVideo.addEventListener('progress', function() {
                console.log("📊 비디오 로딩 진행률:", playbackVideo.buffered.length);
            });

            // 비디오 URL을 백엔드 프록시를 통해 로드
            const proxyUrl = `/api/openvidu/recordings/video?url=${encodeURIComponent(videoUrl)}`;
            console.log("🔗 프록시 비디오 URL 설정:", proxyUrl);
            playbackVideo.src = proxyUrl;
            playbackVideo.load();

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

    // 비디오 정지 및 URL 제거
    playbackVideo.pause();
    playbackVideo.src = '';

    // 플레이어 숨김
    videoPlayer.style.display = 'none';

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

// DOMContentLoaded 이벤트 리스너
document.addEventListener('DOMContentLoaded', initializeApp);

// 만약 DOM이 이미 로드된 경우를 대비한 백업
if (document.readyState === 'loading') {
    console.log("🔄 DOM 로딩 중, DOMContentLoaded 대기...");
} else {
    console.log("🔄 DOM 이미 로드됨, 즉시 초기화 실행...");
    initializeApp();
}