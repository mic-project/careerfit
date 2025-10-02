// src/client/portone.js
export async function ensurePortOne() {
    if (window.IMP) return window.IMP;

    await new Promise((resolve, reject) => {
        const s = document.createElement("script");
        s.src = "https://cdn.iamport.kr/v1/iamport.js";
        s.onload = resolve;
        s.onerror = reject;
        document.head.appendChild(s);
    });

    return window.IMP;
}
