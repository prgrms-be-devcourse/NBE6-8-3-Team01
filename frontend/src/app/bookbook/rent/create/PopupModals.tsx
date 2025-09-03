import React from 'react';
import { useRouter } from "next/navigation";

interface PopupModalsProps {
    // 글 작성 완료 팝업
    showPopup: boolean;
    onClosePopup: () => void;
    
    // AI 조회 실패 팝업
    showAiFailurePopup: boolean;
    onCloseAiFailurePopup: () => void;
    
    // 주소 선택 팝업
    isAddressPopupOpen: boolean;
    onCloseAddressPopup: () => void;
    onSelectAddress: (address: string) => void;
}

export default function PopupModals({
    showPopup,
    onClosePopup,
    showAiFailurePopup,
    onCloseAiFailurePopup,
    isAddressPopupOpen,
    onCloseAddressPopup,
    onSelectAddress
}: PopupModalsProps) {
    const router = useRouter();

    return (
        <>
            {/* 글 작성 팝업 */}
            {showPopup && (
                <div
                    className="fixed inset-0 flex items-center justify-center bg-black/50 z-50"
                    onClick={onClosePopup}
                >
                    <div
                        className="bg-white rounded-xl p-8 shadow-lg flex flex-col items-center"
                        onClick={e => e.stopPropagation()}
                    >
                        <div className="mb-6 text-lg font-semibold">
                            글이 작성되었습니다.
                        </div>
                        <button
                            onClick={() => {
                                onClosePopup();
                                router.push(`/bookbook/rent`);
                            }}
                            className="px-6 py-2 text-white rounded-lg font-bold bg-[#D5BAA3] hover:bg-[#C2A794]"
                        >
                            확인
                        </button>
                    </div>
                </div>
            )}

            {/* AI 조회 실패 팝업 */}
            {showAiFailurePopup && (
                <div 
                    className="fixed inset-0 flex items-center justify-center bg-black/50 z-50 p-4"
                    onClick={onCloseAiFailurePopup}
                >
                    <div
                        className="bg-white rounded-xl p-6 sm:p-8 shadow-lg w-full max-w-sm mx-4"
                        onClick={e => e.stopPropagation()}
                    >
                        <h2 className="text-xl sm:text-2xl font-bold text-gray-800 mb-4 text-center">
                            AI 조회 실패
                        </h2>
                        <hr className="border-t-2 border-gray-300 mb-6" />
                        
                        <p className="text-center text-gray-600 mb-6">
                            책 정보를 자동으로 찾을 수 없습니다.<br />
                            책 검색 기능을 이용해주세요.
                        </p>
                        
                        <div className="flex justify-center">
                            <button
                                onClick={onCloseAiFailurePopup}
                                className="px-6 py-2 text-white rounded-lg font-bold bg-[#D5BAA3] hover:bg-[#C2A794]"
                            >
                                확인
                            </button>
                        </div>
                    </div>
                </div>
            )}
        </>
    );
}
