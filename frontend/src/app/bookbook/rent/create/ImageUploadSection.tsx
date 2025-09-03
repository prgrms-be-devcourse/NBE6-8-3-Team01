import React from 'react';

interface ImageUploadSectionProps {
    bookImage: File | null;
    previewImageUrl: string;
    isAiModeEnabled: boolean;
    isOcrProcessing: boolean;
    onImageChange: (e: React.ChangeEvent<HTMLInputElement>) => void;
}

export default function ImageUploadSection({
    bookImage,
    previewImageUrl,
    isAiModeEnabled,
    isOcrProcessing,
    onImageChange
}: ImageUploadSectionProps) {
    return (
        <div>
            <label htmlFor="bookImage" className="block text-gray-700 text-base font-bold mb-2">
                책 이미지 업로드 {isAiModeEnabled && isOcrProcessing && <span className="text-blue-500">(AI 분석 중...)</span>}
            </label>
            <div className="flex flex-col items-start space-y-3">
                <input
                    type="file"
                    id="bookImage"
                    className="hidden" // 기본 파일 입력을 숨김
                    onChange={onImageChange}
                    accept="image/*" // 이미지 파일만 선택 가능하도록 제한
                    disabled={isAiModeEnabled && isOcrProcessing} // AI 모드에서만 OCR 처리 중 비활성화
                />
                <label
                    htmlFor="bookImage" // '사진 올리기' 버튼(label)을 클릭하면, 브라우저는 자동으로 숨겨진 <input type="file">을 클릭한 것처럼 동작
                    className={`w-full sm:w-auto px-4 py-2 text-white font-semibold rounded-lg shadow-md focus:outline-none focus:ring-2 focus:ring-offset-2 cursor-pointer text-center
                        ${(isAiModeEnabled && isOcrProcessing)
                            ? 'bg-gray-400 cursor-not-allowed' 
                            : 'bg-[#D5BAA3] hover:bg-[#C2A794] focus:ring-[#D5BAA3]'
                        }`}
                >
                    {(isAiModeEnabled && isOcrProcessing) ? 'AI 분석 중...' : '사진 올리기'}
                </label>     
                
                {/* 이미지 미리보기 */}
                <div className="relative">
                    <img
                        src={previewImageUrl}
                        alt="책 이미지"
                        className="w-[200px] h-[150px] object-cover rounded-lg"
                    />
                    
                    {/* OCR 처리 중일 때는 간단한 로딩 표시만 (AI 모드에서만) */}
                    {isAiModeEnabled && isOcrProcessing && (
                        <div className="absolute inset-0 bg-black bg-opacity-20 flex items-center justify-center rounded-lg">
                            <div className="text-white text-center">
                                <div className="w-6 h-6 border-2 border-white border-t-transparent rounded-full animate-spin"></div>
                            </div>
                        </div>
                    )}
                </div>
            </div>
        </div>
    );
}
