import "./App.css";
import { Routes, Route, useNavigate } from "react-router-dom";
import { useEffect } from "react";

// pages
import Meeting from "./pages/Meeting";
import Booking from "./pages/Booking";
import BookingDate from "./pages/BookingDate";
import DocReview from "./pages/DocReview.jsx";
import FeatureAuthJoin from "./pages/FeatureAuthJoin.jsx";
import LoginView from "./pages/LoginView.jsx";
import MainPage from "./pages/MainPage.jsx";
import MyPage from "./pages/MyPage.jsx";
import ConsultantPage from "./pages/ConsultantPage.jsx";
import ConsultantEditProfile from "./pages/ConsultantEditProfile.jsx";
import ConsultantSchedule from "./pages/ConsultantSchedule.jsx";
import ConsultantScheduleRegister from "./pages/ConsultantScheduleRegister.jsx";
import AdminPage from "./pages/AdminPage.jsx";
import UseGuide from "./pages/UseGuide.jsx";
import MyPageEditProfile from "./pages/MyPageEditProfile";
import MyPageMeeting from "./pages/MyPageMeeting";
import MyPagePayment from "./pages/MyPagePayment";
import MyPagePractice from "./pages/MyPagePractice";
import MyPageSchedule from "./pages/MyPageSchedule";
import MyPageEvaluate from "./pages/MyPageEvaluate";
import Payment from "./pages/Payment";
import Practice from "./pages/Practice";
import PracticeInterview from "./pages/PracticeInterview";
// import Post from "./pages/Post";
// import PostDetail from "./pages/PostDetail";
// import PostWriting from "./pages/PostWriting.jsx";
import VerifyComplete from "./pages/VerifyComplete.jsx";
import OAuth2Redirect from "./pages/OAuth2Redirect.jsx";
// import NotFound from "./pages/NotFound";

import Layout from "./pages/Layout";

function App() {
    // 🔐 토큰 만료 시 로그인(본인 인증) 페이지로 이동
    const navigate = useNavigate();
    useEffect(() => {
        const handleUnauthorized = () => {
            console.log("세션 만료, 본인 인증 페이지로 이동");
            navigate("/FeatureAuthJoin");
        };
        window.addEventListener("unauthorized", handleUnauthorized);
        return () => window.removeEventListener("unauthorized", handleUnauthorized);
    }, [navigate]);

    return (
        <>
            <Routes>
                {/* 공통 헤더 없는 페이지 */}
                <Route path="/" element={<LoginView />} />
                {/* 필요하면 활성화 */}
                <Route path="/login" element={<LoginView />} />
                <Route path="/FeatureAuthJoin" element={<FeatureAuthJoin />} />
                <Route path="/verify-complete" element={<VerifyComplete />} />
                <Route path="/oauth2/redirect" element={<OAuth2Redirect />} />

                {/* 공통 헤더 적용 레이아웃 */}
                <Route element={<Layout />}>
                    <Route path="/Meeting" element={<Meeting />} />
                    <Route path="/MainPage" element={<MainPage />} />
                    <Route path="/Booking" element={<Booking />} />
                    <Route path="/BookingDate" element={<BookingDate />} />
                    <Route path="/DocReview" element={<DocReview />} />
                    <Route path="/MyPage" element={<MyPage />} />
                    <Route path="/ConsultantPage" element={<ConsultantPage />} />
                    <Route
                        path="/ConsultantEditProfile"
                        element={<ConsultantEditProfile />}
                    />
                    <Route path="/ConsultantSchedule" element={<ConsultantSchedule />} />
                    <Route
                        path="/ConsultantScheduleRegister"
                        element={<ConsultantScheduleRegister />}
                    />
                    <Route path="/admin" element={<AdminPage />} />
                    <Route path="/UseGuide" element={<UseGuide />} />
                    <Route path="/MyPageEditProfile" element={<MyPageEditProfile />} />
                    <Route path="/MyPageMeeting" element={<MyPageMeeting />} />
                    <Route path="/MyPagePayment" element={<MyPagePayment />} />
                    <Route path="/MyPagePractice" element={<MyPagePractice />} />
                    <Route path="/MyPageSchedule" element={<MyPageSchedule />} />
                    <Route path="/MyPageEvaluate" element={<MyPageEvaluate />} />
                    <Route path="/Payment" element={<Payment />} />
                    <Route path="/Practice" element={<Practice />} />
                    <Route path="/PracticeInterview" element={<PracticeInterview />} />
                    {/* <Route path="/Post" element={<Post />} />
          <Route path="/PostDetail/:id" element={<PostDetail />} />
          <Route path="/PostWriting" element={<PostWriting />} /> */}
                </Route>

                {/* 필요하면 404 페이지 추가 */}
                {/* <Route path="*" element={<NotFound />} /> */}
            </Routes>
        </>
    );
}

export default App;
