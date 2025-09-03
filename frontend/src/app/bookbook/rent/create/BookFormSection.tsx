import React from 'react';

interface BookFormSectionProps {
    // 책 제목
    bookTitle: string;
    onBookTitleChange: (value: string) => void;
    
    // 저자, 출판사, 카테고리
    author: string;
    onAuthorChange: (value: string) => void;
    publisher: string;
    onPublisherChange: (value: string) => void;
    category: string;
    onCategoryChange: (value: string) => void;
    
    // 책 설명
    description: string;
    onDescriptionChange: (value: string) => void;
    
    // 상수 값들
    maxContentLength: number;
}

export default function BookFormSection({
    bookTitle,
    onBookTitleChange,
    author,
    onAuthorChange,
    publisher,
    onPublisherChange,
    category,
    onCategoryChange,
    description,
    onDescriptionChange,
    maxContentLength
}: BookFormSectionProps) {
    return (
        <>
            {/* 책 제목 */}
            <div>
                <label htmlFor="bookTitle" className="block text-gray-700 text-base font-bold mb-2">
                    책 제목
                </label>
                <input
                    type="text"
                    id="bookTitle"
                    className="w-full p-3 border border-gray-300 rounded-lg focus:ring-blue-500 focus:border-blue-500 text-gray-900"
                    placeholder="예: 식탁 위의 세계사"
                    value={bookTitle}
                    onChange={(e) => onBookTitleChange(e.target.value)}
                    required
                />
            </div>

            {/* 저자, 출판사, 카테고리 */}
            <div className="grid grid-cols-1 md:grid-cols-3 gap-6">
                <div>
                    <label htmlFor="author" className="block text-gray-700 text-base font-bold mb-2">
                        저자
                    </label>
                    <input
                        type="text"
                        id="author"
                        className="w-full p-3 border border-gray-300 rounded-lg focus:ring-blue-500 focus:border-blue-500 text-gray-900"
                        placeholder="예: 이영숙"
                        value={author}
                        onChange={(e) => onAuthorChange(e.target.value)}
                        required
                    />
                </div>
                <div>
                    <label htmlFor="publisher" className="block text-gray-700 text-base font-bold mb-2">
                        출판사
                    </label>
                    <input
                        type="text"
                        id="publisher"
                        className="w-full p-3 border border-gray-300 rounded-lg focus:ring-blue-500 focus:border-blue-500 text-gray-900"
                        placeholder="예: 장비"
                        value={publisher}
                        onChange={(e) => onPublisherChange(e.target.value)}
                        required
                    />
                </div>
                <div>
                    <label htmlFor="category" className="block text-gray-700 text-base font-bold mb-2">
                        카테고리
                    </label>
                    <input
                        type="text"
                        id="category"
                        className="w-full p-3 border border-gray-300 rounded-lg focus:ring-blue-500 focus:border-blue-500 text-gray-900"
                        placeholder="예: 역사"
                        value={category}
                        onChange={(e) => onCategoryChange(e.target.value)}
                        required
                    />
                </div>
            </div>

            {/* 책 설명 */}
            <div>
                <label htmlFor="description" className="block text-gray-700 text-base font-bold mb-2">
                    책 설명
                </label>
                <textarea
                    id="description"
                    rows={3}
                    className="w-full p-3 border border-gray-300 rounded-lg focus:ring-blue-500 focus:border-blue-500 text-gray-900 resize-y"
                    placeholder="책에 대한 간략한 설명을 입력하거나, 검색된 내용을 확인하세요."
                    value={description}
                    onChange={(e) => onDescriptionChange(e.target.value)}
                    maxLength={maxContentLength}
                    required
                ></textarea>
                <div className="text-right text-sm text-gray-500 mt-1">
                    {description.length}/{maxContentLength}
                </div>
            </div>
        </>
    );
}
