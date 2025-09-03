import React, { useState } from 'react';

interface BookSearchResult {
    bookTitle: string;
    author: string;
    publisher: string;
    pubDate: string;
    coverImageUrl: string;
    category: string;
    bookDescription: string;
}

interface BookSearchSectionProps {
    searchQuery: string;
    onSearchQueryChange: (query: string) => void;
    onBookSelect: (book: BookSearchResult) => void;
    onSearch: (pageNumber: number) => Promise<void>;
    searchResults: BookSearchResult[];
    showBookSearchModal: boolean;
    onCloseModal: () => void;
    currentPage: number;
    hasMoreResults: boolean;
}

export default function BookSearchSection({
    searchQuery,
    onSearchQueryChange,
    onBookSelect,
    onSearch,
    searchResults,
    showBookSearchModal,
    onCloseModal,
    currentPage,
    hasMoreResults
}: BookSearchSectionProps) {
    const defaultImageUrl = 'https://i.postimg.cc/pLC9D2vW/noimg.gif';

    return (
        <>
            {/* 책 검색 입력 섹션 */}
            <div className="flex flex-col items-center justify-end space-y-3 sm:space-y-0 sm:flex-row sm:space-x-3">
                {/* 책 도움말 말풍선 */}
                <div className="relative w-full sm:w-auto">
                    {/* 책 검색 상자 */}
                    <input
                        type="text"
                        placeholder="책 제목 입력"
                        className="w-full sm:w-auto p-2 border border-gray-300 rounded-lg focus:ring-blue-500 focus:border-blue-500 text-gray-900"
                        value={searchQuery}
                        onChange={(e) => onSearchQueryChange(e.target.value)}
                    />
                    {/* 비어있을 때만 출력하는 span 부분 */}
                    {searchQuery.trim() === '' && (
                        <span className="absolute left-1/2 -top-8 -translate-x-1/2 bg-blue-500 text-white text-xs font-semibold px-2 py-1 rounded-md shadow-md whitespace-nowrap">
                            책 검색 기능으로 간편하게 입력하세요!
                        </span>
                    )}
                </div>

                {/* 책 검색 버튼 */}
                <button
                    type="button"
                    className="px-6 py-2 text-white font-semibold rounded-lg shadow-md bg-[#D5BAA3] hover:bg-[#C2A794]"
                    onClick={() => onSearch(1)}
                >
                    책 검색하기
                </button>
            </div>

            {/* 책 검색 결과 팝업 모달 */}
            {showBookSearchModal && (
                <div
                    className="fixed inset-0 flex items-center justify-center bg-black/50 z-50 p-4"
                    onClick={onCloseModal}
                >
                    <div
                        className="bg-white rounded-xl p-6 sm:p-8 shadow-lg w-full max-w-3xl max-h-[90vh] overflow-y-auto"
                        onClick={e => e.stopPropagation()}
                    >
                        <h2 className="text-xl sm:text-2xl font-bold text-gray-800 mb-4 text-center">
                            책 검색 결과
                        </h2>
                        <hr className="border-t-2 border-gray-300 mb-6" />

                        {searchResults.length > 0 ? (
                            <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-6">
                                {searchResults.map((book, index) => (
                                    <div
                                        key={index}
                                        className="border border-gray-200 rounded-lg p-4 flex flex-col items-center text-center shadow-sm hover:shadow-md transition duration-150 cursor-pointer"
                                    >
                                        <img
                                            src={book.coverImageUrl || defaultImageUrl}
                                            alt={book.bookTitle}
                                            className="w-24 h-32 object-cover rounded-md mb-3"
                                        />
                                        <h3 className="font-semibold text-gray-800 text-base mb-1 line-clamp-2">
                                            {book.bookTitle}
                                        </h3>
                                        <p className="text-sm text-gray-600 line-clamp-1">
                                            {book.author} | {book.publisher}
                                        </p>
                                        <p className="text-xs text-gray-500 mt-1">
                                            {book.pubDate}
                                        </p>
                                        <button
                                            className="mt-4 px-4 py-2 text-white font-semibold rounded-lg bg-[#D5BAA3] hover:bg-[#C2A794] text-sm"
                                            onClick={(e) => {
                                                e.stopPropagation();
                                                onBookSelect(book);
                                            }}
                                        >
                                            선택하기
                                        </button>
                                    </div>
                                ))}
                            </div>
                        ) : (
                            <p className="text-center text-gray-600">검색 결과가 없습니다.</p>
                        )}

                        <div className="flex justify-center items-center mt-6 space-x-4">
                            <button
                                onClick={() => onSearch(currentPage - 1)}
                                disabled={currentPage === 1}
                                className="px-4 py-2 rounded-lg bg-[#D5BAA3] text-white disabled:opacity-50"
                            >
                                이전
                            </button>
                            <span>
                                페이지 {currentPage}
                            </span>
                            <button
                                onClick={() => onSearch(currentPage + 1)}
                                disabled={!hasMoreResults}
                                className="px-4 py-2 rounded-lg bg-[#D5BAA3] text-white disabled:opacity-50"
                            >
                                다음
                            </button>
                        </div>

                        <div className="mt-6 flex justify-center">
                            <button
                                onClick={onCloseModal}
                                className="px-6 py-2 text-white rounded-lg font-bold bg-gray-500 hover:bg-gray-600"
                            >
                                닫기
                            </button>
                        </div>
                    </div>
                </div>
            )}
        </>
    );
}
