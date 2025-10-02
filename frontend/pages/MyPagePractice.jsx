import React, { useEffect, useState } from "react";
import "../components/MyPagePractice.css";
import axios from "../client/axios";

const MyPagePractice = () => {
    const [videos, setVideos] = useState([]);
    const [loading, setLoading] = useState(true);

    useEffect(() => {
        fetchPracticeVideos();
    }, []);

    const fetchPracticeVideos = async () => {
        try {
            setLoading(true);
            const response = await axios.get("/practice-videos/my");

            console.log("🔍 원본 API 응답:", response.data);

            if (response.data.success) {
                const videoData = response.data.data.map((video) => {
                    console.log("🔍 각 영상 데이터:", video);
                    console.log("🔍 video.createdAt 타입:", typeof video.createdAt, "값:", video.createdAt);

                    const createdDate = video.createdAt ? new Date(video.createdAt) : null;
                    console.log("🔍 변환된 Date 객체:", createdDate, "유효한가?", createdDate && !isNaN(createdDate.getTime()));

                    return {
                        id: video.id,
                        title: `면접 연습 #${video.id}`,
                        date: createdDate && !isNaN(createdDate.getTime())
                            ? createdDate.toLocaleString("ko-KR", {
                                year: "numeric",
                                month: "2-digit",
                                day: "2-digit",
                                hour: "2-digit",
                                minute: "2-digit",
                                hour12: false
                            })
                            : "날짜 정보 없음",
                        src: video.videoUrl,
                        showVideo: false,
                    };
                });
                setVideos(videoData);
                console.log("✅ 연습 영상 로드 완료:", videoData.length, "개");
                console.log("✅ 최종 videoData:", videoData);
            } else {
                console.error("❌ 연습 영상 로드 실패:", response.data.message);
            }
        } catch (error) {
            console.error("❌ 연습 영상 조회 오류:", error);
        } finally {
            setLoading(false);
        }
    };

    const toggleVideo = (id) => {
        setVideos((prev) =>
            prev.map((vid) =>
                vid.id === id ? { ...vid, showVideo: !vid.showVideo } : vid
            )
        );
    };

    const handleTitleChange = (id, value) => {
        setVideos((prev) =>
            prev.map((vid) => (vid.id === id ? { ...vid, title: value } : vid))
        );
    };

    const deleteVideo = async (id, e) => {
        e.stopPropagation();
        if (!confirm("이 영상을 삭제하시겠습니까?")) return;

        try {
            const response = await axios.delete(`/practice-videos/${id}`);
            if (response.data.success) {
                setVideos((prev) => prev.filter((vid) => vid.id !== id));
                console.log("✅ 영상 삭제 완료:", id);
            } else {
                alert("영상 삭제에 실패했습니다.");
                console.error("❌ 영상 삭제 실패:", response.data.message);
            }
        } catch (error) {
            alert("영상 삭제 중 오류가 발생했습니다.");
            console.error("❌ 영상 삭제 오류:", error);
        }
    };

    return (
        <div className="practice-container">
            <h2>🎬 나 혼자 연습 영상</h2>
            {loading ? (
                <p className="loading">영상을 불러오는 중...</p>
            ) : videos.length === 0 ? (
                <p className="empty">저장된 영상이 없습니다.</p>
            ) : (
                <ul className="videos-list">
                    {videos.map((vid) => (
                        <li
                            key={vid.id}
                            className="video-item"
                            onClick={() => toggleVideo(vid.id)}
                        >
                            <div className="video-header">
                                <input
                                    className="video-title-input"
                                    value={vid.title}
                                    onChange={(e) => handleTitleChange(vid.id, e.target.value)}
                                    onClick={(e) => e.stopPropagation()}
                                />
                                <button
                                    className="delete-button"
                                    onClick={(e) => deleteVideo(vid.id, e)}
                                >
                                    🗑️ 삭제
                                </button>
                            </div>
                            {vid.showVideo && (
                                <div className="video-wrapper">
                                    <video className="video-player" controls>
                                        <source
                                            src={`${window.location.hostname === 'app.careerfit.net'
                                                ? 'https://api.careerfit.net'
                                                : ''}/api/openvidu/recordings/video?url=${encodeURIComponent(vid.src)}`}
                                            type="video/webm"
                                        />
                                        Your browser does not support the video tag.
                                    </video>
                                </div>
                            )}
                            <div className="video-meta">
                                <span>녹화일: {vid.date}</span>
                            </div>
                        </li>
                    ))}
                </ul>
            )}
        </div>
    );
};

export default MyPagePractice;
