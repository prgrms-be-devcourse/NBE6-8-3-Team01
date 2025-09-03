import React from 'react';

interface AiLoadingPopupProps {
    isVisible: boolean;
    progressValue: number;
}

export default function AiLoadingPopup({ isVisible, progressValue }: AiLoadingPopupProps) {
    if (!isVisible) return null;

    return (
        <div className="fixed inset-0 bg-black/50 bg-opacity-40 backdrop-blur-sm flex items-center justify-center z-[60]">
            <div className="bg-white bg-opacity-95 backdrop-blur-md rounded-2xl p-8 shadow-2xl max-w-sm mx-4 text-center">
                
                {/* 메인 메시지 */}
                <h3 className="text-xl font-bold text-gray-800 mb-4">
                    AI가 이미지를 분석하고 있습니다
                </h3>
                
                {/* 진행 단계 표시 */}
                <div className="space-y-3 mb-6">
                    <div className="flex items-center space-x-3">
                        <div className="w-6 h-6 bg-green-500 rounded-full flex items-center justify-center">
                            <div className="w-2 h-2 bg-white rounded-full"></div>
                        </div>
                        <span className="text-sm text-gray-600">이미지 업로드 완료</span>
                    </div>
                    <div className="flex items-center space-x-3">
                        <div className={`w-6 h-6 rounded-full flex items-center justify-center ${
                            progressValue > 30 ? 'bg-blue-500' : 'bg-gray-300'
                        }`}>
                            <div className={`w-2 h-2 rounded-full ${
                                progressValue > 30 ? 'bg-white animate-pulse' : 'bg-gray-400'
                            }`}></div>
                        </div>
                        <span className={`text-sm ${
                            progressValue > 30 ? 'text-gray-600' : 'text-gray-400'
                        }`}>
                            {progressValue > 30 ? 'AI 이미지 분석 중...' : 'AI 이미지 분석 대기'}
                        </span>
                    </div>
                    <div className="flex items-center space-x-3">
                        <div className={`w-6 h-6 rounded-full flex items-center justify-center ${
                            progressValue > 70 ? 'bg-blue-500' : 'bg-gray-300'
                        }`}>
                            <div className={`w-2 h-2 rounded-full ${
                                progressValue > 70 ? 'bg-white animate-pulse' : 'bg-gray-400'
                            }`}></div>
                        </div>
                        <span className={`text-sm ${
                            progressValue > 70 ? 'text-gray-600' : 'text-gray-400'
                        }`}>
                            {progressValue > 70 ? '도서 정보 검색 중...' : '도서 정보 검색 대기'}
                        </span>
                    </div>
                </div>
                
                {/* 로딩 애니메이션 */}
                <div className="flex justify-center space-x-1 mb-4">
                    <div className="w-2 h-2 bg-blue-500 rounded-full animate-bounce" style={{animationDelay: '0ms'}}></div>
                    <div className="w-2 h-2 bg-blue-500 rounded-full animate-bounce" style={{animationDelay: '150ms'}}></div>
                    <div className="w-2 h-2 bg-blue-500 rounded-full animate-bounce" style={{animationDelay: '300ms'}}></div>
                </div>
                
                {/* 프로그레스 바 */}
                <div className="w-full bg-gray-200 rounded-full h-2 mb-4 overflow-hidden">
                    <div 
                        className="bg-gradient-to-r from-blue-500 to-purple-600 h-2 rounded-full transition-all duration-300 ease-out"
                        style={{width: `${progressValue}%`}}
                    ></div>
                </div>
                
                {/* 진행률 표시 */}
                <div className="text-sm text-gray-600 mb-2">
                    {Math.round(progressValue)}%
                </div>
                
                {/* 예상 소요 시간 */}
                <p className="text-sm text-gray-500">
                    평균 3-5초 소요됩니다
                </p>
            </div>
        </div>
    );
}
