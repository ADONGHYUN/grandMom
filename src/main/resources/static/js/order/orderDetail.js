const PORTONE_STORE_ID = document.body.dataset.storeId;
const KAKAO_CHANNEL_KEY = document.body.dataset.kakaoKey;

// 💡 2. 전역 상태 변수
let serverOrderId = null;
let expectedAmount = null;

document.addEventListener('DOMContentLoaded', () => {
const payBtn = document.getElementById('payBtn');
    if (payBtn) {
        payBtn.addEventListener('click', handlePayment);
    }
});

async function handlePayment() {
    const orderForm = document.getElementById('orderForm');
    const payBtn = document.getElementById('payBtn');

    if (!orderForm.checkValidity()) {
		alert("배송 정보를 입력해주세요.");
        return;
    }

    const originalBtnText = payBtn.innerText;
    payBtn.disabled = true;
    payBtn.innerText = '결제 요청 중...';

    try {
        // 1단계: 주문 정보 서버에 전송 및 orderId, 금액 확정
        const prepareResponse = await prepareOrder();
        serverOrderId = prepareResponse.orderId;
        expectedAmount = prepareResponse.expectedAmount;

        payBtn.innerText = '결제 창 호출 중...';

        // 2단계: PortOne 결제 요청
        const portoneResponse = await requestPortOnePayment(serverOrderId, expectedAmount);
       
		 console.log("portoneResponse::");
		 console.log(portoneResponse);
        // PortOne 결제 실패 처리 (사용자가 취소하거나 오류 발생)
        if (portoneResponse.code !== undefined) {
            // code가 있으면 실패
            alert("결제가 취소되었거나 실패했습니다. 코드: " + portoneResponse.code + ", 메시지: " + portoneResponse.message);
            // ⚠️ TODO: 서버에 주문 상태 정리 (PENDING 주문을 FAILED로) 요청 추가 가능
            return;
        }

        // 3단계: 결제 성공 시 서버에 최종 검증 요청
        payBtn.innerText = '주문 최종 검증 중...';
        await verifyPayment(portoneResponse.paymentId, serverOrderId);
        
        // ✅ 최종 성공: 주문 완료 페이지로 이동
        alert("결제가 성공적으로 완료되었습니다!");
        //window.location.href = `/order/complete?orderId=${serverOrderId}`;

    } catch (error) {
        console.error("결제 처리 중 최종 오류:", error);
        alert(error.message || "결제 처리 중 알 수 없는 오류가 발생했습니다. 고객센터에 문의해주세요.");
    } finally {
        // 💡 UX/안정성 보완 2: 최종적으로 버튼 상태 복구
        payBtn.disabled = false;
        payBtn.innerText = originalBtnText;
    }
}

/**
 * 1단계: 주문 데이터를 서버에 전송하고 orderId와 확정 금액을 받아옴.
 */
async function prepareOrder() {
    const formData = new FormData(document.getElementById('orderForm'));
    const orderData = Object.fromEntries(formData.entries());

    orderData.orderName = document.getElementById('productName').dataset.productname;
    // 클라이언트 금액은 참고용으로만 보냄 (서버에서 반드시 재계산해야 함)
    orderData.clientTotalAmount = parseInt(document.getElementById('price').dataset.price.replace(/,/g, '')); 

    const prepareOrderResponse = await fetch("/order/prepareOrder", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify(orderData),
    });

    if (!prepareOrderResponse.ok) {
        const error = await prepareOrderResponse.json();
        // 💡 실무 보완: 서버에서 전달한 구체적인 에러 메시지 사용
        throw new Error("주문 생성 실패: " + (error.message || response.statusText));
    }
    
    return prepareOrderResponse.json();
}

/**
 * 2단계: PortOne SDK를 호출하여 결제 창을 띄웁니다.
 */
async function requestPortOnePayment(orderId, totalAmount) {
    const paymentId = `payment-${crypto.randomUUID()}`;

    const response = await PortOne.requestPayment({
        storeId: PORTONE_STORE_ID,
        channelKey: KAKAO_CHANNEL_KEY,
        paymentId: paymentId,
        orderName: document.getElementById('productName').dataset.productname,
        totalAmount: totalAmount, // ✅ 서버 확정 금액 사용
        currency: "CURRENCY_KRW",
        payMethod: "EASY_PAY",
        isTestChannel: true,
        redirectUrl: "http://localhost:8080/payment/redirect", 
        customData: {
            orderId: orderId 
        }
    });
	console.log("requestPortOnePayment response::")
	console.log(response)
    return response;
}

/**
 * 3단계: 결제 성공 후, 서버에 최종 검증을 요청합니다.
 */
async function verifyPayment(paymentId, orderId) {
    const response = await fetch("/order/completePayment", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ 
            paymentId: paymentId, 
            orderId: orderId 
        }),
    });

    if (!response.ok) {
        const error = await response.json();
        // 💡 실무 보완: 결제 금액 불일치 등 심각한 오류는 구체적으로 알림
        throw new Error("결제 검증 실패: " + (error.message || "서버 검증 중 오류 발생. 환불 처리되었을 수 있습니다."));
    }
}