import React, { useState } from "react";
import { Link, useNavigate } from "react-router-dom";
import "../components/Practice.css";
import MainLogo from "../assets/MainLogo.png";

// 인성 면접 질문
const personalityQuestions = [
    "지원 동기가 무엇인가요?",
    "성격의 장점과 단점은 무엇이 있나요?",
    "같이 일하고 싶은/싫은 스타일의 사람은 누구인가요?",
    "인생에서 가장 열심히 했던 일이나 순간은 언제인가요?",
    "간단한 자기소개 부탁드립니다.",
    "저희 회사에 지원한 동기가 무엇인가요?",
    "자신의 능력 밖을 벗어난 업무가 주어진다면 어떻게 하겠습니까?",
    "입사 후 포부가 무엇입니까?",
    "당신에게 일이 왜 중요합니까?",
    "직장은 어떤 면을 보고 선택합니까?",
    "인생에서 가장 필요한 사항은 무엇이라 생각하나요?",
    "본인이 리더로 추진했던 일이 있습니까? 있다면 어떤 성과가 나왔는지 말해보세요.",
    "회사 근무를 하면서 가장 중요하다고 생각하는 것은 무엇입니까?",
    "본인은 따라가는 스타일입니까, 아니면 주도하는 스타일입니까?",
    "어떤 일에 적극적으로 임한 순간은 언제였습니까?",
    "어떤 경영 스타일일때 자신의 능력이 최대가 되나요?",
    "상사의 말이 확실히 틀렸을 때는 어떻게 할 것인가",
    "입사 후 회사와 맞지 않는다면 어떻게 하시겠습니까?",
    "어떤 상황에서 스트레스를 받나요?",
    "본인의 업무스타일은 어떤 유형인가요?",
    "상사와 의견이 다를 때는 어떻게 대처할 것입니까?",
    "상사가 부당한 업무지시를 시킨다면 어떻게 할 것입니까?",
    "노래방에서 몇 시간이나 놀 수 있습니까?",
    "단체활동에서 의견 충돌이 일어날 경우 어떻게 해결하겠습니까?",
    "대인관계에서 가장 중요하게 생각하는 것은 무엇입니까?",
    "하고싶지 않았던 부서에 배정되도 괜찮은가?",
    "인생에서 가장 행복했던 경험이 무엇인가요?",
    "존경하는 인물이 누구인가요? 그 이유는 무엇인가요?",
    "남들이 해보지 않은 특별한 경험을 해 본적이 있나요?",
    "최근에 읽은 기사가 있나요? 어떤 내용인가요?",
];
// 직무 면접 질문
const jobQuestions = [
    "트러블 슈팅 경험이 있으신가요?",
    "어떤 언어를 가장 많이 사용하시나요? 그 이유는 무엇인가요?",
    "좋은 소프트웨어란 무엇이라고 생각하나요?",
    "어떤 개발자가 되고 싶으신가요?",
    "지금까지 어떤 프로젝트를 해보셨나요?",
    "전에 진행했던 프로젝트의 아키텍처를 설명해 보세요.",
    "자신이 제출한 포트폴리오를 설명해 보세요.",
    "사용할 줄 아는 프로그래밍 언어는 어떤 것들이 있으며 얼마나 능숙하나요?",
    "queue에대해 설명해 보세요.",
    "본인이 사용할 줄 아는 언어들의 차이점은 무엇인가요?",
    "프로세스와 스레드의 차이점은 무엇인가요?",
    "본인이 지원한 job가 무슨 일을 한다고 생각하나요?",
    "업데이트한 내용에 큰 문제가 생겼다면 어떻게 할 것인가?",
    "http와 https의 차이에 대해서 설명해보세요.",
    "GET방식과 POST방식의 차이에 대하여 설명하세요.",
    "컴포넌트와 모듈의 차이를 설명해보세요",
    "객체 지향과 절차지향의 차이점을 설명해 보세요.",
    "객체지향 언어의 특징을 설명해보세요.",
    "오버라이딩 오버로딩의 차이점과 특징에대해 설명해 보세요.",
    "교육 프로그램에 참가하게 된 계기가 무엇인가요?",
    "어떤 서비스를 개발하고 싶은가요?",
    "회사 기술 스택에 맞추어 단기간 내에 언어와 프레임워크를 학습해야 할 때, 어떻게 공부하고 해결할 것인가?",
    "개발에 관심을 가지게 된 계기가 무엇인가요?",
    "비IT 동료와 효과적으로 의사소통하려면 어떻게 해야 합니까?",
    "Stack 과 Queue에대해 설명해주세요.",
    "좋은 코드란 무엇이라 생각하시나요?",
    "개발 능력 향상을 위해 어떤 것을 하고 계신가요?",
    "추상화에 대해 설명해주세요.",
    "MVC패턴에 대해 설명해주세요",
    "세션과 쿠키의 차이점에대해 설명해주세요.",
    "Spring DI에 대해 설명해주세요.",
    "ORM에 대해 설명해 보세요",
    "데이터베이스 옵티마이저에 대해 설명해주세요",
    "JPA를 사용하면 좋은점이 무엇인가요?",
    "JWT에 대해 설명해주세요, 사용하면 장점과 단점?",
    "Enum 사용해보셨나요? Enum이란 무엇인가요?",
    "REST API란? REST API 명령어(CRUD)에대해 아시는것 설명해주세요.",
    "HTTP METHOD에 대해 설명해보세요",
    "HTTP 상태코드에 대해 아는대로 말해보세요.",
    "DB에서 인덱스를 사용했을때 장점이 무엇이있나요?",
    "가비지 컬렉션에대해 설명해보세요.",
    "브라우저 렌더링 과정을 설명해주세요.",
    "CORS는 무엇인지, 이를 처리해본 경험이 있다면 말씀해주세요.",
    "타입스크립트를 사용하는 이유는 무엇인가요?",
    "브라우저는 어떻게 동작하는지 설명해주세요.",
    "스프링프레임워크에대해 설명해보세요.",
    "RDBMS와 NOSQL의 차이에대해 설명해보세요.",
    "MyBatis의 장점에대해 설명해보세요.",
    "싱글톤 패턴에 대해 설명해주세요",
    "GET,POST를 어떻게 다르게 쓰는지 말씀해 주세요.",
    "호이스팅에 대해서 설명해보세요.",
    "this의 용법 아는대로 설명해주세요.",
    "SPA에대해 설명해 주세요.",
    "var let const 의 차이점에대해 설명해주세요.",
    "Promise와 Callback의 차이를 설명해주세요.",
    "async, await 사용방법을 설명해주세요.",
    "함수 선언형과 함수 표현식의 차이에 대해 설명해주세요.",
    "자바스크립트가 유동적인 언어인 이유는 무엇인가요?",
    "CSS에서 margin과 padding에 대해 설명해주세요.",
    "브라우저 저장소에는 무엇이 있고 그것들의 차이점을 설명해주세요.",
    "이벤트 버블링에 대해서 말씀해 주세요.",
    "SASS, SCSS를 사용해본적이 있나요? 기존 CSS와 비교할 때 어떤면이 더 좋은가요?",
    "Vue와 React의 차이는 무엇인가요?",
    "Vue에서 양방향 데이터가 일어나는 원리에 대해서 설명해주세요.",
    "MVVM모델에 대해서 설명해주세요.",
    "ES6에서 Arrow 함수를 언제 쓰나요?",
    "Javascript에서 this란 무엇인가요?",
    "Frond-End 성능 최적화란 무엇인가요? 성능 최적화 경험이 있다면 설명해 주세요.",
    "CSR과 SSR의 차이점은 무엇인가요?",
    "주소창에 naver.com을 입력하면 어떤 일이 생기나요?",
    "AJAX에대해 설명해보세요.",
    "리플렉션이란 무엇인가요?(ios)",
    "constraint layout에대해 설명해주세요.",
    "액티비티와 프래그먼트에 대해 설명해주세요.",
    "안드로이드 빌드 프로세스에대해 설명해주세요.",
    "Bounds 와 Frame 의 차이점을 설명해주세요.",
    "ARC란 무엇인지 설명해주세요.(ios)",
    "Copy On Write는 어떤 방식으로 동작하는지 설명해주세요.(ios)",
    "Optional 이란 무엇인지 설명해주세요.(ios)",
    "Http Request Code 에 대해 아는대로 설명해 보세요.(ios)",
    "안드로이드 스튜디오의 Thread 에 대해 설명해보세요.",
    "Android Activity Life Cycle 에 대해 설명해보세요.",
    "안드로이드의 4대 컴포넌트에 대해서 설명해보세요.",
    "안드로이드의 액티비티와 액티비티의 수명주기에 대해서 설명해보세요.",
    "Weak와 Strong에 대해 설명하세요.(ios)",
    "Escaping Closure의 개념이 무엇인가요?(ios)",
    "Swift에서 Class와 Struct의 차이는 무엇인가요?(ios)",
    "Autolayout Constraint의 Priority의 개념이 무엇이고, 어떤상황에 사용하나요?(ios)",
    "안드로이드의 메니페이스 파일에 대해서 설명해보세요.",
    "코틀린의 장점에 대해 설명하세요.",
    "안드로이드의 테스크란?",
    "고유값(eigen value)와 고유벡터(eigen vector)에 대해 설명해주세요. 그리고 왜 중요할까요?",
    "확률 모형과 확률 변수는 무엇일까요?",
    "좋은 feature란 무엇인가요. 이 feature의 성능을 판단하기 위한 방법에는 어떤 것이 있나요?",
    "상관관계는 인과관계를 의미하지 않는다라는 말이 있습니다. 설명해주실 수 있나요?",
    "나만의 feature selection 방식을 설명해봅시다.",
    "아웃라이어의 판단하는 기준은 무엇인가요?",
    "평균(mean)과 중앙값(median)중에 어떤 케이스에서 뭐를 써야할까요?",
    "신뢰 구간의 정의는 무엇인가요?",
    "샘플링(Sampling)과 리샘플링(Resampling)에 대해 설명해주세요. 리샘플링은 무슨 장점이 있을까요?",
    "엔트로피(entropy)에 대해 설명해주세요.",
    "회귀 / 분류시 알맞은 metric은 무엇일까요?",
    "정규화를 왜 해야할까요? 정규화의 방법은 무엇이 있나요?",
    "텍스트 더미에서 주제를 추출해야 합니다. 어떤 방식으로 접근해 나가시겠나요?",
    "네트워크 관계를 시각화해야 할 경우 어떻게 해야할까요?",
    "히스토그램의 가장 큰 문제는 무엇인가요?",
    "워드클라우드는 보기엔 예쁘지만 약점이 있습니다. 어떤 약점일까요?",
    "좋은 모델의 정의는 무엇일까요?",
    "L1, L2 정규화에 대해 설명해주세요",
    "인덱스는 크게 Hash 인덱스와 B+Tree 인덱스가 있습니다. 이것은 무엇일까요?",
    "딥러닝은 무엇인가요? 딥러닝과 머신러닝의 차이는?",
    "DevOps를 설명해주세요.",
    "DevOps 구현을위한 모범 사례는 무엇인가요?",
    "DevOps의 주요 구성 요소는 무엇입니까?",
    "지속적인 통합을 설명해주세요.",
    "프로젝트에서 DevOps를 구현해야 할 때 어떻게 접근 하시겠습니까?",
    "연속 테스트를 설명하세요.",
    "오늘날 업계에서 사용되는 10 대 DevOps 도구는 무엇입니까? 아는대로 말씀해주세요.",
    "DevOps에서 사용되는 스크립팅 도구는 무엇입니까?",
    "DevOps 성공을 위해 따랐던 몇 가지 지표를 설명하십시오.",
    "DevOps의 경력 관점에서 기대하는 것은 무엇입니까?",
];

const Practice = () => {
    const [questionType, setQuestionType] = useState(null);
    const [questions, setQuestions] = useState([]);
    const [selected, setSelected] = useState([]);
    const navigate = useNavigate();

    // 유형 선택
    const handleTypeClick = (type) => {
        setQuestionType(type);
        if (type === "personality") {
            setQuestions(personalityQuestions);
        } else {
            setQuestions(jobQuestions);
        }
        setSelected([]);
    };

    // 질문 선택/해제
    const toggleSelect = (q) => {
        if (selected.includes(q)) {
            setSelected(selected.filter((item) => item !== q));
        } else {
            setSelected([...selected, q]);
        }
    };

    // 랜덤 질문 뽑고 interview 페이지로 이동
    const handleStart = () => {
        if (selected.length === 0) return alert("질문을 선택해주세요!");

        // /PracticeInterview 페이지로 이동하면서 선택된 질문 배열 전체 전달
        navigate("/PracticeInterview", { state: { questionList: selected } });
    };

    return (
        <div className="home-container">
            <div className="doc-feedback-container">
                {/* 제목/설명 */}
                <div className="doc-feedback-header">
                    <h2>나 혼자 연습</h2>
                    <p>
                        원하는 인성 및 직무 면접 질문을 선택하여 나만의 답변을 체계적으로
                        준비하세요.
                    </p>
                </div>

                {/* 메인 질문 선택 UI */}
                <div className="practice-containerr">
                    <aside className="sidebar">
                        <h3>면접 유형 선택</h3>
                        <button
                            className={`type-btn ${
                                questionType === "personality" ? "active" : ""
                            }`}
                            onClick={() => handleTypeClick("personality")}
                        >
                            인성 유형 질문
                        </button>
                        <button
                            className={`type-btn ${questionType === "job" ? "active" : ""}`}
                            onClick={() => handleTypeClick("job")}
                        >
                            직무 유형 질문
                        </button>
                    </aside>

                    <section className="questions-section">
                        <div className="questions-header">
                            <h3>총 질문 개수 : {questions.length}</h3>
                        </div>
                        <div className="questions-list">
                            {questions.map((q, idx) => (
                                <label
                                    key={idx}
                                    className={`question-item ${
                                        selected.includes(q) ? "selected" : ""
                                    }`}
                                >
                                    <input
                                        type="checkbox"
                                        checked={selected.includes(q)}
                                        onChange={() => toggleSelect(q)}
                                    />
                                    <span className="question-text">{q}</span>
                                </label>
                            ))}
                        </div>

                        <div className="footer">
                            <p>선택 된 질문 : {selected.length}개</p>
                            <button className="start-btn" onClick={handleStart}>
                                시작하기
                            </button>
                        </div>
                    </section>
                </div>
            </div>
        </div>
    );
};

export default Practice;
