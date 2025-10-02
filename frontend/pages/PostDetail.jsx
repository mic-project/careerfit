import React from "react";
import { useParams, Link } from "react-router-dom";
import "../components/PostDetail.css";
import MainLogo from "../assets/MainLogo.png";

const dummyPosts = [
    {
        id: 1,
        date: "09/03 14:20",
        title: "○○○회사 인식은 어떤가요?",
        author: "익명",
        comments: 4,
        likes: 0,
        content: "게시글 내용 1",
    },
    {
        id: 2,
        date: "09/03 13:59",
        title: "면접볼 때 긴장 안하는 팁",
        author: "가나다",
        comments: 6,
        likes: 8,
        content: "게시글 내용 2",
    },
    // 필요하면 더 추가
];

const PostDetail = () => {
    const { id } = useParams(); // URL에서 id 가져오기
    const post = dummyPosts.find((p) => p.id === parseInt(id));

    if (!post) return <div>게시글을 찾을 수 없습니다.</div>;

    return (
        <div className="home-container">
            <div className="post-detail-container">
                <Link to="/Post" className="back-link">
                    ← 게시판으로 돌아가기
                </Link>
                <h2>{post.title}</h2>
                <p className="post-meta">
                    {post.date} | 작성자: {post.author}
                </p>
                <div className="post-content">{post.content}</div>
            </div>
        </div>
    );
};

export default PostDetail;
