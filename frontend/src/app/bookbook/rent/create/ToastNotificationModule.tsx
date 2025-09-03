import React, { useState } from 'react';

interface ToastNotificationProps {
    message: string | null;
    type: 'success' | 'error' | 'info' | null;
}

export default function ToastNotification({ message, type }: ToastNotificationProps) {
    if (!message) return null;

    return (
        <div
            className={`fixed bottom-8 left-1/2 -translate-x-1/2 px-6 py-3 rounded-lg shadow-lg text-white font-semibold text-center z-50 max-w-sm transition-all duration-300 ease-in-out
                ${type === 'success' ? 'bg-green-500' : 
                  type === 'error' ? 'bg-red-500' : 
                  type === 'info' ? 'bg-blue-500' : 'bg-gray-500'}`}
        >
            {message}
        </div>
    );
}

// 토스트 메시지 타입 정의
export type ToastType = 'success' | 'error' | 'info';

// 토스트 상태 인터페이스
export interface ToastState {
    message: string | null;
    type: ToastType | null;
}

// 토스트 표시 함수 타입
export type ShowToastFunction = (message: string, type: ToastType) => void;

// 토스트 훅 (상태 관리용)
export const useToast = () => {
    const [toastState, setToastState] = useState<ToastState>({
        message: null,
        type: null
    });

    const showToast: ShowToastFunction = (message: string, type: ToastType) => {
        setToastState({ message, type });
        
        // 자동으로 토스트 숨기기
        setTimeout(() => {
            setToastState({ message: null, type: null });
        }, type === 'info' ? 2000 : 3000); // info는 2초, 나머지는 3초
    };

    const hideToast = () => {
        setToastState({ message: null, type: null });
    };

    return {
        toastState,
        showToast,
        hideToast
    };
};
