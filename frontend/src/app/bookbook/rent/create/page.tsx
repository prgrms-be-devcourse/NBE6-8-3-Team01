// 25.09.01 현준
// src/app/bookbook/rent/create/page.tsx
// 글 작성을 위한 페이지

"use client";

import React, { useEffect, useState } from 'react';
import { useRouter } from "next/navigation";

// 분리한 AddressSelectionPopup 컴포넌트를 import
import AddressSelectionPopup from '@/app/components/AddressSelectionPopup';

interface BookSearchResult {
    bookTitle: string;
    author: string;
    publisher: string;
    pubDate: string;
    coverImageUrl: string;
    category: string;
    bookDescription: string;
}

// 새로운 타입 정의 : 현재 유저 정보
interface CurrentUserDto {
    userId: number;
    userStatus: 'ACTIVE' | 'INACTIVE' | 'SUSPENDED';
}

export default function BookRentPage() {
    const [title, setTitle] = useState('');
    const [bookImage, setBookImage] = useState<File | null>(null);
    const [bookCondition, setBookCondition] = useState('');
    const [address, setAddress] = useState('');
    const [contents, setContents] = useState('');
    const [bookTitle, setBookTitle] = useState('');
    const [author, setAuthor] = useState('');
    const [publisher, setPublisher] = useState('');
    const [category, setCategory] = useState('');
    const [description, setDescription] = useState('');

    // OCR 처리 상태 관리
    const [isOcrProcessing, setIsOcrProcessing] = useState(false);
    const [ocrResult, setOcrResult] = useState<{
        extractedText: string;
        detectedBookTitle: string | null;
        confidence: number;
        searchResults: any[] | null;
    } | null>(null);
    
    // 프로그레스 바 진행률 상태 추가
    const [progressValue, setProgressValue] = useState(0);

    // 자동 입력 상태 표시
    const [isAutoFilled, setIsAutoFilled] = useState(false);
    const [autoFillSource, setAutoFillSource] = useState<'ocr' | 'manual' | null>(null);

    // Toast 메시지 상태 추가
    const [toastMessage, setToastMessage] = useState<string | null>(null);
    const [toastType, setToastType] = useState<'success' | 'error' | 'info' | null>(null);

    // 토스트 메세지를 보여주는 함수
    const showToast = (message: string, type: 'success' | 'error' | 'info') => {
        setToastMessage(message);
        setToastType(type);
        setTimeout(() => {
            setToastMessage(null);
            setToastType(null);
        }, type === 'info' ? 2000 : 3000); // info 메시지는 2초로 단축
    }

    const [showPopup, setShowPopup] = useState(false);

    const [searchQuery, setSearchQuery] = useState('');
    const [showBookSearchModal, setShowBookSearchModal] = useState(false);
    const [searchResults, setSearchResults] = useState<BookSearchResult[]>([]);

    // 페이지네이션 관련 상태 추가 및 수정
    const [currentPage, setCurrentPage] = useState(1);
    const itemsPerPage = 10; // 백엔드 MaxResults와 동일하게 10으로 설정
    const [hasMoreResults, setHasMoreResults] = useState(false);

    const defaultImageUrl = 'https://i.postimg.cc/pLC9D2vW/noimg.gif';
    const [previewImageUrl, setPreviewImageUrl] = useState<string>(defaultImageUrl);

         // 주소 선택 관련 상태 추가
     const [isAddressPopupOpen, setIsAddressPopupOpen] = useState(false); // 팝업 출력 용
     const [selectedAddress, setSelectedAddress] = useState(''); // 선택된 주소
     
     // AI 조회 실패 팝업 상태 추가
     const [showAiFailurePopup, setShowAiFailurePopup] = useState(false);

    useEffect(() => {
        if (bookImage) {
            const objectUrl = URL.createObjectURL(bookImage);
            setPreviewImageUrl(objectUrl);
            return () => URL.revokeObjectURL(objectUrl);
        } else {
            setPreviewImageUrl(defaultImageUrl);
        }
    }, [bookImage]);

    const conditions = ['최상 (깨끗함)', '상 (사용감 적음)', '중 (사용감 있음)', '하 (손상 있음)'];

    const router = useRouter();



    // AI 분석 기반 자동 글 내용 생성 함수
    const generateAutoContents = (book: any, ocrResult: any) => {
        const confidence = (ocrResult.confidence * 100).toFixed(1);
        
        // 책 상태 분석 (OCR 신뢰도 기반)
        let conditionAnalysis = '';
        if (ocrResult.confidence > 0.7) {
            conditionAnalysis = '책 상태가 아주 좋습니다.';
        } else if (ocrResult.confidence > 0.6) {
            conditionAnalysis = '책 상태가 양호합니다.';
        } else if (ocrResult.confidence > 0.5) {
            conditionAnalysis = '약간의 사용감이 있지만 양호합니다.';
        } else {
            conditionAnalysis = '사용감이 다소 있습니다.';
        }

        // 자동 완성 되는 글 내용 부분
        return `"${book.bookTitle}"를 대여해드립니다.
${book.publisher}에서 출간한 ${book.author} 작가의 책입니다.
${book.category || '다양한 분야'}에 관심 있는 분들께 추천합니다.

${conditionAnalysis}`;
    };

    // ESC 키로 로딩 팝업 닫기
    useEffect(() => {
        const handleEscKey = (event: KeyboardEvent) => {
            if (event.key === 'Escape' && isOcrProcessing) {
                // ESC 키로는 로딩을 중단할 수 없음 (처리 중이므로)
                // 단순히 사용자에게 알림만 표시
                showToast('AI 분석이 진행 중입니다. 잠시만 기다려주세요.', 'info');
            }
        };

        document.addEventListener('keydown', handleEscKey);
        return () => document.removeEventListener('keydown', handleEscKey);
    }, [isOcrProcessing]);

    // 추가된 로직 : 페이지 로드 시, 유저 상태 확인
    useEffect(() => {
        const checkUserStatus = async () => {
            try {
                // 현재 로그인된 유저 정보를 가져오는 API 엔드포인트 호출
                const res = await fetch(`${process.env.NEXT_PUBLIC_API_BASE_URL}/api/v1/bookbook/users/status`, {
                    method: "GET",
                    credentials: "include",
                });
                
                if (res.ok) {
                    // 서버 응답이 `RsData`로 감싸져 있으므로, `data` 필드를 추출해야 합니다.
                    const responseData = await res.json();
                    const currentUser: CurrentUserDto = responseData.data;
                    
                    // 만약 유저 상태가 'SUSPENDED'라면
                    if (currentUser.userStatus === 'SUSPENDED') {
                        // 경고창을 띄우고
                        alert('정지된 회원입니다.');
                        // 홈 페이지로 리다이렉트
                        router.push(`/bookbook`);
                    }
                } else {
                    // 유저 정보 가져오기 실패 시 로그인 페이지 등으로 리다이렉트
                    // 예를 들어, 인증 실패 시
                    if (res.status === 401) {
                         alert('로그인이 필요합니다.');
                         router.push('/login');
                    }
                }
            } catch (error) {
                console.error("유저 상태 확인 중 오류 발생:", error);
                // 네트워크 오류 발생 시에도 홈으로 보내거나 에러 메시지 표시
                alert('유저 정보를 가져오는 중 오류가 발생했습니다.');
                router.push('/');
            }
        };

        checkUserStatus();
    }, [router]); // router 객체를 의존성 배열에 추가

    const resetForm = () => {
        setTitle('');
        setBookImage(null);
        setBookCondition('');
        setSelectedAddress('');
        setAddress('');
        setContents('');
        setBookTitle('');
        setAuthor('');
        setPublisher('');
        setCategory('');
        setSearchQuery('');
        setDescription('');
        setSearchResults([]);
        setShowBookSearchModal(false);
        setCurrentPage(1); // 폼 초기화 시 페이지도 1로 초기화
        setHasMoreResults(false);
    };
    
    // OCR 실패 시 책 관련 필드만 초기화하는 함수
    const resetBookFields = () => {
        setBookTitle('');
        setAuthor('');
        setPublisher('');
        setCategory('');
        setDescription('');
        setTitle('');
        setContents('');
        setIsAutoFilled(false);
        setAutoFillSource(null);
    };

    // OCR 자동 실행 추가
    const handleImageChange = async (e: React.ChangeEvent<HTMLInputElement>) => {
        if (e.target.files && e.target.files[0]) {

            const selectedFile = e.target.files[0];

            // 기존 이미지 설정 로직
            setBookImage(selectedFile);

            // 이미지 선택과 동시에 OCR 실행 
            try{
                await handleOcrBookSearch(selectedFile);
            }catch(error){
                console.error('OCR 자동 실행 중 오류 발생', error);
                showToast('OCR 자동 실행 중 오류가 발생했습니다.', 'error');
            }
            
        }
    };

    // start 파라미터를 받는 handleBookSearch 함수로 변경
    const handleBookSearch = async (pageNumber: number) => {
        if(!searchQuery.trim()){
            showToast('검색어를 입력해주세요.', 'error');
            return;
        }

        // 백엔드 책 검색 API 호출 시 start 파라미터 추가
        const backendSearchApiUrl = `${process.env.NEXT_PUBLIC_API_BASE_URL}/api/v1/bookbook/searchbook?query=${encodeURIComponent(searchQuery)}&start=${pageNumber}`;

        try{
            const response = await fetch(backendSearchApiUrl);
            if(!response.ok){
                const errorData = await response.text();
                console.error('백엔드 책 검색 API 요청 실패:', response.status, response.statusText, errorData);
                showToast(`책 검색 API 요청 실패: ${response.status} ${response.statusText}`, 'error');
                return;
            }

            const data: BookSearchResult[] = await response.json(); 

            if(data && data.length > 0){
                setSearchResults(data);
                // 가져온 결과 수가 itemsPerPage와 같으면 다음 페이지가 더 있을 수 있다고 가정
                setHasMoreResults(data.length === itemsPerPage);
                setShowBookSearchModal(true);
                setCurrentPage(pageNumber); // 검색 성공 시 현재 페이지 업데이트
            } else {
                showToast('검색 결과가 없습니다. 직접 입력해주세요.', 'error');
                setSearchResults([]);
                setHasMoreResults(false);
                setShowBookSearchModal(false); // 결과 없으면 모달 닫기
            }
        } catch (error) {
            console.error('책 검색 중 오류 발생', error);
            showToast('책 검색 중 오류가 발생했습니다. 잠시 후 다시 시도하세요.', 'error');
            setSearchResults([]);
            setHasMoreResults(false);
            setShowBookSearchModal(false);
        }
    };

    // 책 선택 시 폼 필드 채우는 함수
    const selectBook = (book: BookSearchResult) => {
        setBookTitle(book.bookTitle);
        setAuthor(book.author);
        setPublisher(book.publisher);
        setCategory(book.category || ''); // 카테고리 필드 추가
        setDescription(book.bookDescription || ''); // 책 설명 필드 추가
        setShowBookSearchModal(false); // 모달 닫기
    };

    // OCR + 알라딘 검색 통합 함수
    const handleOcrBookSearch = async (imageFile: File): Promise<boolean> => {
        setIsOcrProcessing(true);
        setOcrResult(null);
        setProgressValue(0); // 프로그레스 바 초기화
        
        // 프로그레스 바 애니메이션 시작
        const progressInterval = setInterval(() => {
            setProgressValue(prev => {
                if (prev >= 90) {
                    clearInterval(progressInterval);
                    return 90; // 90%에서 멈춤 (실제 완료 시 100%로 설정)
                }
                return prev + Math.random() * 15; // 랜덤하게 진행
            });
        }, 200);

        // 디버깅을 위한 상세 로깅
        console.log('🔍 OCR 요청 시작:', {
            fileName: imageFile.name,
            fileSize: `${(imageFile.size / 1024 / 1024).toFixed(2)}MB`,
            fileType: imageFile.type
        });
        
        // 사용자에게 처리 중임을 알림
        showToast('AI가 책 표지를 분석 중입니다... (3-5초 소요)', 'info');
        
        const formData = new FormData();
        formData.append('file', imageFile);
        
        try {
            const startTime = Date.now();
            const response = await fetch(`${process.env.NEXT_PUBLIC_API_BASE_URL}/api/v1/bookbook/ocr-book-search`, {
                method: 'POST',
                body: formData,
                credentials: 'include', // 기존 패턴과 일치
            });
            
            if (response.ok) {
                const rsData = await response.json();

                console.log('📨 백엔드 응답 데이터:', rsData);
                // RsData 구조 검증
                if (!rsData.data) {
                    console.error('❌ RsData.data가 null입니다:', rsData);
                    showToast('서버 응답 형식이 올바르지 않습니다.', 'error');
                    return false;
                }
                
                const result = rsData.data;

                // 응답 데이터 구조 로깅
                console.log('OCR 분석 결과:', {
                    extractedText: result.extractedText?.substring(0, 100) + '...',
                    detectedBookTitle: result.detectedBookTitle,
                    confidence: result.confidence,
                    searchResultsCount: result.searchResults?.length || 0
                });
                
                // OCR 결과 저장
                setOcrResult(result);
                
                                 // 검색 결과가 있으면 자동으로 첫 번째 결과로 모든 필드 채우기
                 if (result.searchResults && result.searchResults.length > 0) {
                     const book = result.searchResults[0];
                     console.log('자동 선택된 도서:', book.bookTitle);
                     
                                           // 1. 책 정보 자동 입력
                      setBookTitle(book.bookTitle);
                      setAuthor(book.author);
                      setPublisher(book.publisher);
                      setCategory(book.category || '');
                      setDescription(book.bookDescription || '');
                      
                      // 2. 글 제목 자동 생성: "[책 제목]"
                      const autoTitle = `[${book.bookTitle}]`;
                      setTitle(autoTitle);
                      
                      // 3. 글 내용 자동 생성: AI 분석 기반 설명
                      const autoContents = generateAutoContents(book, result);
                      setContents(autoContents);
                      
                      // 4. 책 검색 상자에도 제목 입력
                      setSearchQuery(book.bookTitle);
                      
                      // 자동 입력 상태 설정
                      setIsAutoFilled(true);
                      setAutoFillSource('ocr');
                      
                      showToast(`"${book.bookTitle}" 모든 정보가 자동으로 입력되었습니다!`, 'success');
                      return true;
                     
                 } else if (result.detectedBookTitle) {
                     // 검색 결과가 0건인 경우 모든 필드 초기화 (책 제목 포함)
                     if (!result.searchResults || result.searchResults.length === 0) {
                         console.log('검색 결과가 0건이므로 모든 필드 초기화');
                         resetBookFields();
                         
                         // 모든 필드를 비우고 (책 제목도 포함)
                         setBookTitle('');
                         setContents('');
                         
                         // AI 조회 실패 팝업 표시
                         setShowAiFailurePopup(true);
                         return false;
                     }
                     
                     // 이 부분은 실제로는 도달하지 않음 (위에서 이미 처리됨)
                     // 하지만 혹시 모를 상황을 대비해 남겨둠
                     console.log('예상치 못한 상황: 제목은 감지되었지만 검색 결과 처리 로직에 도달');
                     return false;
                    
                } else {
                    // OCR 실패
                    console.log('OCR 감지 실패 - 신뢰도:', result.confidence);
                    showToast('책 제목을 인식하지 못했습니다. 수동으로 입력해주세요.', 'error');
                    
                    // OCR 실패 시 책 관련 필드 초기화
                    resetBookFields();
                    
                    return false;
                }
            } else {
                // 상세한 에러 로깅
            const errorData = await response.json().catch(() => null);
            console.error('OCR API 오류:', {
                status: response.status,
                statusText: response.statusText,
                errorData: errorData
            });
            
            const errorMessage = errorData?.msg || `HTTP ${response.status}: ${response.statusText}`;
            showToast(`${errorMessage}`, 'error');
            
            // API 오류 시에도 책 관련 필드 초기화
            resetBookFields();
            
            return false;
            }
            
        } catch (error) {
            console.error('OCR 네트워크 오류:', error);
            showToast('네트워크 오류가 발생했습니다. 수동으로 검색해주세요.', 'error');
            
            // 네트워크 오류 시에도 책 관련 필드 초기화
            resetBookFields();
            
            return false;
            
        } finally {
            setProgressValue(100); // 프로그레스 바 100% 완료
            setTimeout(() => {
                setIsOcrProcessing(false);
                setProgressValue(0); // 상태 초기화
            }, 500); // 0.5초 후 팝업 닫기
            console.log('OCR 처리 완료');
        }
    };

    // 백엔드 API (POST /rent)로 데이터 전송.
    // 1. 이미지 파일이 있다면 먼저 이미지 업로드 API로 전송하여 URL을 받습니다.
    // 2. 받은 이미지 URL과 폼 데이터를 조합하여 대여글 생성 API로 전송합니다.
    const handleSubmit = async (e: React.FormEvent<HTMLFormElement>) => {
        e.preventDefault();

        let imageUrl = 'https://i.postimg.cc/pLC9D2vW/noimg.gif'; // 기본 이미지 URL

        // ✅ 핵심 로직: bookImage가 null이고 previewImageUrl이 defaultImageUrl과 같으면 등록 막기
        if (!bookImage) {
            showToast('책 이미지를 등록해 주세요.', 'error');
            return;
        }

        if(bookImage){
            const imageFormData = new FormData();
            imageFormData.append('file', bookImage);

            try{
                const imageUploadRes = await fetch(`${process.env.NEXT_PUBLIC_API_BASE_URL}/api/v1/bookbook/upload-image`, {
                    method: "POST",
                    body: imageFormData,
                });

                if(imageUploadRes.ok){
                    const data = await imageUploadRes.json();
                    imageUrl = data.imageUrl;
                }else{
                    const errorText = await imageUploadRes.text();
                    console.error('이미지 업로드 실패', errorText);
                    showToast(`이미지 업로드 실패: ${imageUploadRes.statusText || errorText}`, 'error');
                    return;
                }
            }catch(error){
                console.error('이미지 업로드 중 네트워크 오류', error);
                showToast('이미지 업로드 중 오류가 발생했습니다.', 'error');
                return;
            }
        }

        const formData = {
            title: title,
            bookCondition: bookCondition,
            bookImage: imageUrl,
            address: selectedAddress,
            contents: contents,
            rentStatus: 'AVAILABLE', // 백엔드의 RentStatus.AVAILABLE과 동일한 문자열
            bookTitle: bookTitle,
            author: author,
            publisher: publisher,
            category: category,
            description: description
        };

        // 백엔드 Rent 페이지 생성 POST 요청으로 전송
        try{
            const res = await fetch(`${process.env.NEXT_PUBLIC_API_BASE_URL}/bookbook/rent/create`, {
            method: "POST",
                credentials: "include",
                headers: {"Content-Type": "application/json"},
                body: JSON.stringify(formData),
            });

            if(res.ok){
                resetForm();
                setShowPopup(true);
            } else {

                const errorData = await res.json();
                console.error('책 등록 실패', errorData);
                showToast(`책 등록에 실패했습니다. ${errorData.msg || res.statusText}`, 'error');
            }
        } catch(error) {
            console.error('책 등록 중 네트워크 에러', error);
            showToast('책 등록 중 네트워크 에러가 발생했습니다.', 'error');
        }
    };

    return (
        <div className="min-h-screen bg-gray-100 flex items-center justify-center py-8 px-4 sm:py-12 sm:px-16 md:py-16 md:px-24 font-inter">
            <div className="bg-white py-6 px-8 sm:py-8 sm:px-10 md:py-10 md:px-12 rounded-xl shadow-lg w-full max-w-4xl">
                <h1 className="text-2xl sm:text-3xl font-bold text-gray-800 mb-4 text-left">
                    중고 책 등록하기
                </h1>
                <hr className="border-t-2 border-gray-300 mb-6 sm:mb-8" />

                <form onSubmit={handleSubmit} className="space-y-6">
                    
                    {/* 수정된 이미지 업로드 섹션 */}
                    <div>
                        <label htmlFor="bookImage" className="block text-gray-700 text-base font-bold mb-2">
                            책 이미지 업로드 {isOcrProcessing && <span className="text-blue-500">(AI 분석 중...)</span>}
                        </label>
                        <div className="flex flex-col items-start space-y-3">
                            <input
                                type="file"
                                id="bookImage"
                                className="hidden" // 기본 파일 입력을 숨김
                                onChange={handleImageChange}
                                accept="image/*" // 이미지 파일만 선택 가능하도록 제한
                                disabled={isOcrProcessing} // OCR 처리 중 비활성화
                            />
                            <label
                                htmlFor="bookImage" // '사진 올리기' 버튼(label)을 클릭하면, 브라우저는 자동으로 숨겨진 <input type="file">을 클릭한 것처럼 동작
                                className={`w-full sm:w-auto px-4 py-2 text-white font-semibold rounded-lg shadow-md focus:outline-none focus:ring-2 focus:ring-offset-2 cursor-pointer text-center
                                    ${isOcrProcessing 
                                        ? 'bg-gray-400 cursor-not-allowed' 
                                        : 'bg-[#D5BAA3] hover:bg-[#C2A794] focus:ring-[#D5BAA3]'
                                    }`}
                            >
                                {isOcrProcessing ? 'AI 분석 중...' : '사진 올리기'}
                            </label>     
                            
                            {/* 이미지 미리보기 */}
                            <div className="relative">
                                <img
                                    src={previewImageUrl}
                                    alt="책 이미지"
                                    className="w-[200px] h-[150px] object-cover rounded-lg"
                                />
                                
                                                         {/* OCR 처리 중일 때는 간단한 로딩 표시만 */}
                             {isOcrProcessing && (
                                 <div className="absolute inset-0 bg-black bg-opacity-20 flex items-center justify-center rounded-lg">
                                     <div className="text-white text-center">
                                         <div className="w-6 h-6 border-2 border-white border-t-transparent rounded-full animate-spin"></div>
                                     </div>
                                 </div>
                             )}
                                
                                {/* 자동 입력 성공 표시 */}
                                {isAutoFilled && autoFillSource === 'ocr' && (
                                    <div className="absolute top-2 right-2 bg-green-500 text-white px-2 py-1 rounded text-xs">
                                        AI 자동 입력
                                    </div>
                                )}
                            </div>
                        </div>
                    </div>

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
                            onChange={(e) => setTitle(e.target.value)}
                            required
                        />
                    </div>
                    
                                {/* AI 분석 중 로딩 팝업 */}
                                             {isOcrProcessing && (
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
                     )}

                    {/* 책 상태, 주소 입력 부분 */}
                    <div className="grid grid-cols-1 md:grid-cols-3 gap-6">

                        {/* 책 상태 */}
                        <div className='md:col-span-1'> {/* 1:2 비율을 위해 md:col-span-1 추가 */}
                            <label htmlFor="bookCondition" className="block text-gray-700 text-base font-bold mb-2">
                                책 상태
                            </label>
                            {/* 책 상태 토글 */}
                            <select
                                id="bookCondition"
                                className="w-full p-3 border border-gray-300 rounded-lg focus:ring-blue-500 focus:border-blue-500 text-gray-900 h-12"
                                value={bookCondition}
                                onChange={(e) => setBookCondition(e.target.value)}
                                required
                            >
                                <option value="" disabled>책 상태를 선택하세요</option>
                                {conditions.map((cond) => (
                                    <option key={cond} value={cond}>{cond}</option>
                                ))}
                            </select>
                        </div>

                        {/* 주소 입력 */}
                        <div className='md:col-span-2'> {/* 1:2 비율을 위해 md:col-span-2 추가 */}
                            <label htmlFor="address" className="block text-gray-700 text-base font-bold mb-2">
                                주소
                            </label>

                            {/* 주소 선택 필드를 텍스트와 버튼으로 변경 */}
                            <div className="flex items-center space-x-2">
                                {/* 주소 선택 인풋 상자 */}
                                <input
                                    type="text"
                                    id="address"
                                    className="w-full p-3 border border-gray-300 rounded-lg focus:ring-blue-500 focus:border-blue-500 text-gray-900 h-12"
                                    value={selectedAddress || '주소를 선택해주세요'} // 선택된 주소 표시
                                    readOnly // 직접 입력 방지
                                    required
                                />
                                {/* 책 선택 버튼 */}
                                <button
                                    type="button"
                                    onClick={() => setIsAddressPopupOpen(true)} // 주소 팝업 열기
                                    className="px-4 py-3 whitespace-nowrap text-white font-semibold rounded-lg shadow-md bg-[#D5BAA3] hover:bg-[#C2A794] "
                                >
                                    선택
                                </button>
                            </div>

                        </div>
                    </div>

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
                            onChange={(e) => setContents(e.target.value)}
                            maxLength={500}
                            required
                        ></textarea>
                        <div className="text-right text-sm text-gray-500 mt-1">
                            {contents.length}/500
                        </div>
                    </div>

                    <div className="flex flex-col items-center justify-end space-y-3 sm:space-y-0 sm:flex-row sm:space-x-3">
                        {/* 책 도움말 말풍선 */}
                        <div className="relative w-full sm:w-auto">
                            {/* 책 검색 상자 */}
                            <input
                                type="text"
                                placeholder="책 제목 입력"
                                className="w-full sm:w-auto p-2 border border-gray-300 rounded-lg focus:ring-blue-500 focus:border-blue-500 text-gray-900"
                                value={searchQuery}
                                onChange={(e) => setSearchQuery(e.target.value)}
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
                            className="px-6 py-2 text-white font-semibold rounded-lg shadow-md
                            bg-[#D5BAA3] hover:bg-[#C2A794]"
                            onClick={() => handleBookSearch(1)}
                        >
                            책 검색하기
                        </button>
                    </div>

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
                            onChange={(e) => setBookTitle(e.target.value)}
                            required
                        />
                    </div>

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
                                onChange={(e) => setAuthor(e.target.value)}
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
                                onChange={(e) => setPublisher(e.target.value)}
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
                                onChange={(e) => setCategory(e.target.value)}
                                required
                            />
                        </div>
                    </div>

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
                            onChange={(e) => setDescription(e.target.value)}
                            maxLength={500}
                            required
                        ></textarea>
                        <div className="text-right text-sm text-gray-500 mt-1">
                            {description.length}/500
                        </div>
                    </div>

                    <div className="pt-4 flex justify-center">
                        <button
                            type="submit"
                            className="w-64 py-3 text-white text-lg font-semibold rounded-lg shadow-md transition duration-200
                            bg-[#D5BAA3] hover:bg-[#C2A794]"
                        >
                            등록하기
                        </button>
                    </div>
                </form>
            </div>

            {/* 토스트 메시지 컴포넌트 */}
            {toastMessage && (
                <div
                    className={`fixed bottom-8 left-1/2 -translate-x-1/2 px-6 py-3 rounded-lg shadow-lg text-white font-semibold text-center z-50 max-w-sm
                        ${toastType === 'success' ? 'bg-green-500' : 
                          toastType === 'error' ? 'bg-red-500' : 
                          toastType === 'info' ? 'bg-blue-500' : 'bg-gray-500'}`}
                >
                    {toastMessage}
                </div>
            )}
        
            {/* 책 검색 결과 팝업 모달 */}
            {showBookSearchModal && (
                <div
                    className="fixed inset-0 flex items-center justify-center bg-black/50 z-50 p-4"
                    onClick={() => setShowBookSearchModal(false)}
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
                                                selectBook(book); // 책 선택 함수 호출
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
                                onClick={() => handleBookSearch(currentPage - 1)} // 이전 버튼 클릭 시
                                disabled={currentPage === 1}
                                className="px-4 py-2 rounded-lg bg-[#D5BAA3] text-white disabled:opacity-50"
                            >
                                이전
                            </button>
                            {/* 현재 페이지 번호와 총 페이지 수를 정확히 알 수 없으므로, 현재 페이지 정보만 표시하거나, 다음 페이지가 있는지 여부로 대체 */}
                            <span>
                                페이지 {currentPage}
                            </span>
                            <button
                                onClick={() => handleBookSearch(currentPage + 1)} // 다음 버튼 클릭 시
                                disabled={!hasMoreResults} // 다음 페이지 결과가 없을 경우 비활성화
                                className="px-4 py-2 rounded-lg bg-[#D5BAA3] text-white disabled:opacity-50"
                            >
                                다음
                            </button>
                        </div>

                        <div className="mt-6 flex justify-center">
                            <button
                                onClick={() => setShowBookSearchModal(false)}
                                className="px-6 py-2 text-white rounded-lg font-bold bg-gray-500 hover:bg-gray-600"
                            >
                                닫기
                            </button>
                        </div>
                    </div>
                </div>
            )}   

            {/* 글 작성 팝업 */}
            {showPopup && (
                <div
                  className="fixed inset-0 flex items-center justify-center bg-black/50 z-50"
                  onClick={() => setShowPopup(false)}
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
                        setShowPopup(false);
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
                     onClick={() => setShowAiFailurePopup(false)}
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
                                 onClick={() => setShowAiFailurePopup(false)}
                                 className="px-6 py-2 text-white rounded-lg font-bold bg-[#D5BAA3] hover:bg-[#C2A794]"
                             >
                                 확인
                             </button>
                         </div>
                     </div>
                 </div>
             )}
             
             {/* 주소 선택 팝업 */}
             <AddressSelectionPopup
                 isOpen={isAddressPopupOpen}
                 onClose={() => setIsAddressPopupOpen(false)}
                 onSelectAddress={(address: string) => {
                     setSelectedAddress(address);
                     setIsAddressPopupOpen(false);
                 }}
             />
        </div>
    )
};