import React from 'react';

interface FormFieldsSectionProps {
    // 글 제목
    title: string;
    onTitleChange: (value: string) => void;
    
    // 책 상태
    bookCondition: string;
    onBookConditionChange: (value: string) => void;
    conditions: string[];
    
    // 주소
    selectedAddress: string;
    onAddressPopupOpen: () => void;
    
    // 글 내용
    contents: string;
    onContentsChange: (value: string) => void;
    
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
}

export default function FormFieldsSection({
    title,
    onTitleChange,
    bookCondition,
    onBookConditionChange,
    conditions,
    selectedAddress,
    onAddressPopupOpen,
    contents,
    onContentsChange,
    bookTitle,
    onBookTitleChange,
    author,
    onAuthorChange,
    publisher,
    onPublisherChange,
    category,
    onCategoryChange,
    description,
    onDescriptionChange
}: FormFieldsSectionProps) {
    return (
        <>
            {/* 글 제목 */}
            <div>
                <label htmlFor="postTitle" className="block text-gray-700 text-base font-bold mb-2">
                    글 제목
                </label>
                <input
                    type="text"
                    id="postTitle"
                    className="w-full p-3 border border-gray-300 rounded-lg focus:ring-blue-500 focus:border-blue-500 text-gray-900"
                    placeholder="글 제목을 입력해주세요."
                    value={title}
                    onChange={(e) => onTitleChange(e.target.value)}
                    required
                />
            </div>

            {/* 책 상태, 주소 입력 부분 */}
            <div className="grid grid-cols-1 md:grid-cols-3 gap-6">
                {/* 책 상태 */}
                <div className='md:col-span-1'>
                    <label htmlFor="bookCondition" className="block text-gray-700 text-base font-bold mb-2">
                        책 상태
                    </label>
                    <select
                        id="bookCondition"
                        className="w-full p-3 border border-gray-300 rounded-lg focus:ring-blue-500 focus:border-blue-500 text-gray-900 h-12"
                        value={bookCondition}
                        onChange={(e) => onBookConditionChange(e.target.value)}
                        required
                    >
                        <option value="" disabled>책 상태를 선택하세요</option>
                        {conditions.map((cond) => (
                            <option key={cond} value={cond}>{cond}</option>
                        ))}
                    </select>
                </div>

                {/* 주소 입력 */}
                <div className='md:col-span-2'>
                    <label htmlFor="address" className="block text-gray-700 text-base font-bold mb-2">
                        주소
                    </label>
                    <div className="flex items-center space-x-2">
                        <input
                            type="text"
                            id="address"
                            className="w-full p-3 border border-gray-300 rounded-lg focus:ring-blue-500 focus:border-blue-500 text-gray-900 h-12"
                            value={selectedAddress || '주소를 선택해주세요'}
                            readOnly
                            required
                        />
                        <button
                            type="button"
                            onClick={onAddressPopupOpen}
                            className="px-4 py-3 whitespace-nowrap text-white font-semibold rounded-lg shadow-md bg-[#D5BAA3] hover:bg-[#C2A794]"
                        >
                            선택
                        </button>
                    </div>
                </div>
            </div>

            {/* 글 내용 */}
            <div>
                <label htmlFor="contents" className="block text-gray-700 text-base font-bold mb-2">
                    글 내용
                </label>
                <textarea
                    id="contents"
                    rows={6}
                    className="w-full p-3 border border-gray-300 rounded-lg focus:ring-blue-500 focus:border-blue-500 text-gray-900 resize-y"
                    placeholder="책에 대한 설명, 상태 등을 자세히 적어주세요."
                    value={contents}
                    onChange={(e) => onContentsChange(e.target.value)}
                    maxLength={500}
                    required
                ></textarea>
                <div className="text-right text-sm text-gray-500 mt-1">
                    {contents.length}/500
                </div>
            </div>

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
                    maxLength={500}
                    required
                ></textarea>
                <div className="text-right text-sm text-gray-500 mt-1">
                    {description.length}/500
                </div>
            </div>
        </>
    );
}
