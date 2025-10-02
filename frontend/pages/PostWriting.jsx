import React, { useState } from "react";
import { Link, useNavigate } from "react-router-dom";
import "../components/PostWriting.css";
import MainLogo from "../assets/MainLogo.png";

const PostWriting = () => {
    const navigate = useNavigate();

    // 🔹 로그인 여부 (예시: true/false, 나중에 실제 로그인 상태로 교체)
    const isLoggedIn = true;

    const [title, setTitle] = useState("");
    const [content, setContent] = useState("");
    const [isAnonymous, setIsAnonymous] = useState(true);
    const [image, setImage] = useState(null);

    const handleSubmit = (e) => {
        e.preventDefault();

        if (!isLoggedIn) {
            alert("로그인해야 글쓰기 가능합니다.");
            return;
        }

        if (!title || !content) {
            alert("제목과 내용을 입력해주세요.");
            return;
        }

        // 🔹 실제 API 호출 부분
        const postData = {
            title,
            content,
            author: isAnonymous ? "익명" : "실명",
            image,
        };

        console.log("게시글 제출:", postData);
        alert("게시글이 등록되었습니다.");

        // 완료 후 게시판으로 이동
        navigate("/Post");
    };

    return (
        <div className="home-container">
            <div className="doc-feedback-container">
                <div className="doc-feedback-header">
                    <h2>게시글 작성</h2>
                    <p>커리어핏 회원들과 취업 정보·팁을 공유해요!</p>
                </div>

                {!isLoggedIn && (
                    <p style={{ color: "red", textAlign: "center" }}>
                        로그인해야 글쓰기가 가능합니다.
                    </p>
                )}

                <form className="post-writing-form" onSubmit={handleSubmit}>
                    <label>
                        제목
                        <input
                            type="text"
                            maxLength={100}
                            value={title}
                            onChange={(e) => setTitle(e.target.value)}
                            required
                        />
                    </label>

                    <label>
                        내용 (5000자 이내)
                        <textarea
                            maxLength={5000}
                            rows={10}
                            value={content}
                            onChange={(e) => setContent(e.target.value)}
                            required
                        />
                    </label>

                    <div className="author-choice">
                        <label>
                            <input
                                type="radio"
                                checked={isAnonymous}
                                onChange={() => setIsAnonymous(true)}
                            />
                            익명
                        </label>
                        <label>
                            <input
                                type="radio"
                                checked={!isAnonymous}
                                onChange={() => setIsAnonymous(false)}
                            />
                            실명
                        </label>
                    </div>

                    <label>
                        사진 첨부
                        <input
                            type="file"
                            accept="image/*"
                            onChange={(e) => setImage(e.target.files[0])}
                        />
                    </label>

                    <button type="submit" className="submit-button">
                        완료
                    </button>
                </form>
            </div>
        </div>
    );
};

export default PostWriting;
