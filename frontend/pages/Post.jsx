import React, { useState, useEffect } from "react";
import { Link, useNavigate } from "react-router-dom";
import "../components/Post.css";
import MainLogo from "../assets/MainLogo.png";

// 테스트용 더미 데이터
const dummyPosts = [
    {
        id: 1,
        date: "09/03 14:20",
        title: "○○○회사 인식은 어떤가요?",
        author: "익명",
        comments: 4,
        likes: 0,
    },
    {
        id: 2,
        date: "09/03 13:59",
        title: "면접볼 때 긴장 안하는 팁",
        author: "가나다",
        comments: 6,
        likes: 8,
    },
    {
        id: 3,
        date: "09/03 13:50",
        title: "자기소개서 작성 팁",
        author: "나다라",
        comments: 2,
        likes: 3,
    },
    {
        id: 4,
        date: "09/03 13:30",
        title: "면접 준비 자료 공유",
        author: "라마바",
        comments: 1,
        likes: 0,
    },
    {
        id: 5,
        date: "09/03 13:10",
        title: "이력서 사진 어떻게 찍어야 할까?",
        author: "사아자",
        comments: 3,
        likes: 1,
    },
    {
        id: 6,
        date: "09/03 12:50",
        title: "면접 복장 질문",
        author: "차카타",
        comments: 0,
        likes: 2,
    },
];

const POSTS_PER_PAGE = 5; // 🔹 변경: 한 페이지당 5개

const Post = () => {
    const [posts, setPosts] = useState([]);
    const [currentPage, setCurrentPage] = useState(1); // 🔹 변경: 페이지 상태
    const navigate = useNavigate(); // 🔹 변경: 게시글 클릭 시 상세페이지 이동

    useEffect(() => {
        setPosts(dummyPosts);
        // 🔹 향후 API 호출 예시
        // fetch("/api/posts")
        //   .then(res => res.json())
        //   .then(data => setPosts(data));
    }, []);

    const totalPages = Math.ceil(posts.length / POSTS_PER_PAGE); // 🔹 변경: 총 페이지 계산

    const displayedPosts = posts.slice(
        (currentPage - 1) * POSTS_PER_PAGE,
        currentPage * POSTS_PER_PAGE
    ); // 🔹 변경: 현재 페이지에 보여줄 게시글

    const goToNextPage = () => {
        if (currentPage < totalPages) setCurrentPage((prev) => prev + 1);
    };

    const goToPrevPage = () => {
        if (currentPage > 1) setCurrentPage((prev) => prev - 1);
    };

    const handlePostClick = (id) => {
        navigate(`/PostDetail/${id}`); // 🔹 변경: 게시글 클릭 시 상세페이지 이동
    };

    return (
        <div className="home-container">
            {/* 상단 로고 + 메뉴 */}
            <header className="home-header">
                <div className="logo">
                    <img src={MainLogo} alt="CareerFit 로고" />
                </div>
                <nav className="nav-menu">
                    <Link
                        to="/Post"
                        className={window.location.pathname === "/Post" ? "active" : ""}
                    >
                        게시판
                    </Link>
                    <Link
                        to="/MyPage"
                        className={window.location.pathname === "/MyPage" ? "active" : ""}
                    >
                        마이페이지
                    </Link>
                    <Link to="/">로그아웃</Link>
                </nav>
            </header>

            <div className="doc-feedback-container">
                {/* 제목/설명 */}
                <div className="doc-feedback-header">
                    <h2>게시판</h2>
                    <p>커리어핏 회원들과 함께 다양한 취업 정보·팁을 공유해요!</p>
                </div>

                {/* 게시판 테이블 */}
                <table className="post-table">
                    <thead>
                    <tr>
                        <th>게시 시간</th>
                        <th>글 제목</th>
                        <th>작성자</th>
                        <th>댓글수</th>
                        <th>추천</th>
                    </tr>
                    </thead>
                    <tbody>
                    {displayedPosts.map(
                        (
                            post // 🔹 변경: 페이지별 게시글만 렌더링
                        ) => (
                            <tr
                                key={post.id}
                                onClick={() => handlePostClick(post.id)}
                                style={{ cursor: "pointer" }}
                            >
                                {" "}
                                {/* 🔹 변경: 클릭 가능 */}
                                <td>{post.date}</td>
                                <td>{post.title}</td>
                                <td>{post.author}</td>
                                <td>{post.comments}</td>
                                <td>{post.likes}</td>
                            </tr>
                        )
                    )}
                    </tbody>
                </table>

                {/* 페이지 이동 버튼 */}
                <div className="pagination">
                    {" "}
                    {/* 🔹 변경: 버튼 스타일 적용 */}
                    <button onClick={goToPrevPage} disabled={currentPage === 1}>
                        &lt; 이전페이지
                    </button>
                    <span>
            {currentPage} / {totalPages}
          </span>
                    <button onClick={goToNextPage} disabled={currentPage === totalPages}>
                        다음페이지 &gt;
                    </button>
                </div>
            </div>

            {/* 글쓰기 버튼 항상 하단 */}
            <div className="write-button-container">
                {" "}
                {/* 🔹 변경: 아래 여백 확보 */}
                <Link to="/PostWriting" className="write-button">
                    글 쓰기
                </Link>
            </div>
        </div>
    );
};

export default Post;
