// 25.09.03 현준
// 글 작성을 위한 페이지
"use client";

import React, { useEffect, useState } from 'react';
import { useRouter } from "next/navigation";

// 분리한 AddressSelectionPopup 컴포넌트를 import
import AddressSelectionPopup from '@/app/components/AddressSelectionPopup';
// AI 작성 모드 선택 컴포넌트를 import
import AiModeSelector from './AiModeSelector';
// 책 검색 섹션 컴포넌트를 import
import BookSearchSection from './BookSearchSection';
// 이미지 업로드 섹션 컴포넌트를 import
import ImageUploadSection from './ImageUploadSection';
// AI 분석 로딩 팝업 컴포넌트를 import
import AiLoadingPopup from './AiLoadingPopup';
// 폼 필드 섹션 컴포넌트들을 import
import PostFormSection from './PostFormSection';
import BookFormSection from './BookFormSection';
// 팝업 모달들 컴포넌트를 import
import PopupModals from './PopupModals';
// 토스트 알림 모듈을 import
import ToastNotification, { useToast, ToastType } from './ToastNotificationModule';

interface BookSearchResult {
    bookTitle: string;
    author: string;
    publisher: string;
    pubDate: string;
    coverImageUrl: string;
    category: string;
    bookDescription: string;
}

// OCR 결과에 대한 구체적인 타입 정의
interface OcrResult {
    extractedText: string;
    detectedBookTitle: string | null;
    confidence: number;
    searchResults: BookSearchResult[] | null;
}

// 새로운 타입 정의 : 현재 유저 정보
interface CurrentUserDto {
    userId: number;
    userStatus: 'ACTIVE' | 'INACTIVE' | 'SUSPENDED';
}

export default function BookRentPage() {
    // 상수 정의 - 매직 넘버 제거
    const CONSTANTS = {
        PROGRESS_ANIMATION_INTERVAL: 200,
        PROGRESS_INCREMENT_MAX: 15,
        PROGRESS_THRESHOLD_90: 90,
        PROGRESS_THRESHOLD_100: 100,
        OCR_POPUP_DELAY: 500,
        CONFIDENCE_THRESHOLD_HIGH: 0.7,
        CONFIDENCE_THRESHOLD_MEDIUM: 0.6,
        CONFIDENCE_THRESHOLD_LOW: 0.5,
        TOAST_DELAY_INFO: 2000,
        TOAST_DELAY_DEFAULT: 3000,
        ITEMS_PER_PAGE: 10,
        MAX_CONTENT_LENGTH: 500,
        DEFAULT_IMAGE_URL: 'https://i.postimg.cc/pLC9D2vW/noimg.gif'
    } as const;

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
    const [ocrResult, setOcrResult] = useState<OcrResult | null>(null);
    
    // 프로그레스 바 진행률 상태 추가
    const [progressValue, setProgressValue] = useState(0);

    // 자동 입력 상태 표시
    const [isAutoFilled, setIsAutoFilled] = useState(false);
    const [autoFillSource, setAutoFillSource] = useState<'ocr' | 'manual' | null>(null);

    // 토스트 알림 훅 사용
    const { toastState, showToast } = useToast();

    const [showPopup, setShowPopup] = useState(false);

    const [searchQuery, setSearchQuery] = useState('');
    const [showBookSearchModal, setShowBookSearchModal] = useState(false);
    const [searchResults, setSearchResults] = useState<BookSearchResult[]>([]);

    // 페이지네이션 관련 상태 추가 및 수정
    const [currentPage, setCurrentPage] = useState(1);
    const [hasMoreResults, setHasMoreResults] = useState(false);

    const [previewImageUrl, setPreviewImageUrl] = useState<string>(CONSTANTS.DEFAULT_IMAGE_URL);

    // 주소 선택 관련 상태 추가
    const [isAddressPopupOpen, setIsAddressPopupOpen] = useState(false); // 팝업 출력 용
    const [selectedAddress, setSelectedAddress] = useState(''); // 선택된 주소
    
    // AI 조회 실패 팝업 상태 추가
    const [showAiFailurePopup, setShowAiFailurePopup] = useState(false);
    
    // AI 작성 모드 상태 추가
    const [isAiModeEnabled, setIsAiModeEnabled] = useState(true);
    
    // AI 모드 변경 시 OCR 결과 초기화
    useEffect(() => {
        if (!isAiModeEnabled) {
            // AI 모드가 비활성화되면 OCR 결과와 자동 입력 상태 초기화
            setOcrResult(null);
            setIsAutoFilled(false);
            setAutoFillSource(null);
        }
    }, [isAiModeEnabled]);

    useEffect(() => {
        if (bookImage) {
            const objectUrl = URL.createObjectURL(bookImage);
            setPreviewImageUrl(objectUrl);
            return () => URL.revokeObjectURL(objectUrl);
        } else {
            setPreviewImageUrl(CONSTANTS.DEFAULT_IMAGE_URL);
        }
    }, [bookImage]);

    const conditions = ['최상 (깨끗함)', '상 (사용감 적음)', '중 (사용감 있음)', '하 (손상 있음)'];

    const router = useRouter();



    // AI 분석 기반 자동 글 내용 생성 함수
    const generateAutoContents = (book: BookSearchResult, ocrResult: OcrResult) => {
        const confidence = (ocrResult.confidence * 100).toFixed(1);
        
        // 책 상태 분석 (OCR 신뢰도 기반)
        let conditionAnalysis = '';
        if (ocrResult.confidence > CONSTANTS.CONFIDENCE_THRESHOLD_HIGH) {
            conditionAnalysis = '책 상태가 아주 좋습니다.';
        } else if (ocrResult.confidence > CONSTANTS.CONFIDENCE_THRESHOLD_MEDIUM) {
            conditionAnalysis = '책 상태가 양호합니다.';
        } else if (ocrResult.confidence > CONSTANTS.CONFIDENCE_THRESHOLD_LOW) {
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
                         // 토스트 메시지로 알림
                         showToast('정지된 회원입니다.', 'error' as ToastType);
                         // 홈 페이지로 리다이렉트
                         router.push(`/bookbook`);
                     }
                 } else {
                     // 유저 정보 가져오기 실패 시 로그인 페이지 등으로 리다이렉트
                     // 예를 들어, 인증 실패 시
                     if (res.status === 401) {
                         showToast('로그인이 필요합니다.', 'error' as ToastType);
                         router.push('/login');
                     }
                 }
             } catch (error) {
                 console.error("유저 상태 확인 중 오류 발생:", error);
                 // 네트워크 오류 발생 시에도 홈으로 보내거나 에러 메시지 표시
                 showToast('유저 정보를 가져오는 중 오류가 발생했습니다.', 'error' as ToastType);
                 router.push('/');
             }
        };

        checkUserStatus();
    }, [router]); // router 객체를 의존성 배열에 추가

    // 통합된 초기화 함수들
    const resetForm = () => {
        // 모든 폼 필드 초기화
        setTitle('');
        setBookCondition('');
        setAddress('');
        setContents('');
        setBookTitle('');
        setAuthor('');
        setPublisher('');
        setCategory('');
        setDescription('');
        
        // 이미지 및 주소 초기화
        setBookImage(null);
        setSelectedAddress('');
        
        // 검색 관련 상태 초기화
        setSearchQuery('');
        setSearchResults([]);
        setShowBookSearchModal(false);
        setCurrentPage(1);
        setHasMoreResults(false);
        
        // OCR 상태 초기화
        setIsAutoFilled(false);
        setAutoFillSource(null);
    };
    
    // OCR 실패 시 책 관련 필드만 초기화하는 함수 (중복 제거)
    const resetBookFields = () => {
        // 책 관련 필드만 초기화
        setBookTitle('');
        setAuthor('');
        setPublisher('');
        setCategory('');
        setDescription('');
        setTitle('');
        setContents('');
        
        // OCR 상태 초기화
        setIsAutoFilled(false);
        setAutoFillSource(null);
    };

    // OCR 자동 실행 추가
    const handleImageChange = async (e: React.ChangeEvent<HTMLInputElement>) => {
        if (e.target.files && e.target.files[0]) {

            const selectedFile = e.target.files[0];

            // 기존 이미지 설정 로직
            setBookImage(selectedFile);

            // AI 모드가 활성화된 경우에만 OCR 실행
            if (isAiModeEnabled) {
                try{
                    await handleOcrBookSearch(selectedFile);
                }catch(error){
                    console.error('OCR 자동 실행 중 오류 발생', error);
                    showToast('OCR 자동 실행 중 오류가 발생했습니다.', 'error' as ToastType);
                }
            } else {
                // AI 모드가 비활성화된 경우 단순 이미지 등록만
                showToast('이미지가 등록되었습니다.  수동으로 정보를 입력해주세요.', 'info' as ToastType);
            }
            
        }
    };

    // start 파라미터를 받는 handleBookSearch 함수로 변경
    const handleBookSearch = async (pageNumber: number) => {
        if(!searchQuery.trim()){
            showToast('검색어를 입력해주세요.', 'error' as ToastType);
            return;
        }

        // 백엔드 책 검색 API 호출 시 start 파라미터 추가
        const backendSearchApiUrl = `${process.env.NEXT_PUBLIC_API_BASE_URL}/api/v1/bookbook/searchbook?query=${encodeURIComponent(searchQuery)}&start=${pageNumber}`;

        try{
            const response = await fetch(backendSearchApiUrl);
            if(!response.ok){
                const errorData = await response.text();
                console.error('백엔드 책 검색 API 요청 실패:', response.status, response.statusText, errorData);
                showToast(`책 검색 API 요청 실패: ${response.status} ${response.statusText}`, 'error' as ToastType);
                return;
            }

            const data: BookSearchResult[] = await response.json(); 

            if(data && data.length > 0){
                setSearchResults(data);
                // 가져온 결과 수가 CONSTANTS.ITEMS_PER_PAGE와 같으면 다음 페이지가 더 있을 수 있다고 가정
                setHasMoreResults(data.length === CONSTANTS.ITEMS_PER_PAGE);
                setShowBookSearchModal(true);
                setCurrentPage(pageNumber); // 검색 성공 시 현재 페이지 업데이트
            } else {
                showToast('검색 결과가 없습니다. 직접 입력해주세요.', 'error' as ToastType);
                
                // 검색 상태 초기화를 한 번에 처리
                setSearchResults([]);
                setHasMoreResults(false);
                setShowBookSearchModal(false);
            }
        } catch (error) {
            console.error('책 검색 중 오류 발생', error);
            showToast('책 검색 중 오류가 발생했습니다. 잠시 후 다시 시도하세요.', 'error' as ToastType);
            
            // 검색 상태 초기화를 한 번에 처리
            setSearchResults([]);
            setHasMoreResults(false);
            setShowBookSearchModal(false);
        }
    };

    // 책 선택 시 폼 필드 채우는 함수 (중복 제거)
    const selectBook = (book: BookSearchResult) => {
        // 책 정보를 한 번에 업데이트
        setBookTitle(book.bookTitle);
        setAuthor(book.author);
        setPublisher(book.publisher);
        setCategory(book.category || '');
        setDescription(book.bookDescription || '');
        
        // 모달 닫기
        setShowBookSearchModal(false);
    };

    // 프로그레스 바 애니메이션 시작 함수
    const startProgressAnimation = () => {
        const progressInterval = setInterval(() => {
            setProgressValue(prev => {
                if (prev >= CONSTANTS.PROGRESS_THRESHOLD_90) {
                    clearInterval(progressInterval);
                    return CONSTANTS.PROGRESS_THRESHOLD_90; // 90%에서 멈춤 (실제 완료 시 100%로 설정)
                }
                return prev + Math.random() * CONSTANTS.PROGRESS_INCREMENT_MAX; // 랜덤하게 진행
            });
        }, CONSTANTS.PROGRESS_ANIMATION_INTERVAL);
        
        return progressInterval;
    };

    // OCR API 호출 함수
    const callOcrApi = async (imageFile: File): Promise<Response> => {
        const formData = new FormData();
        formData.append('file', imageFile);
        
        return await fetch(`${process.env.NEXT_PUBLIC_API_BASE_URL}/api/v1/bookbook/ocr-book-search`, {
            method: 'POST',
            body: formData,
            credentials: 'include',
        });
    };

    // OCR 응답 처리 함수
    const processOcrResponse = async (response: Response): Promise<OcrResult | null> => {
        if (!response.ok) {
            const errorData = await response.json().catch(() => null);
            console.error('OCR API 오류:', {
                status: response.status,
                statusText: response.statusText,
                errorData: errorData
            });
            
            const errorMessage = errorData?.msg || `HTTP ${response.status}: ${response.statusText}`;
            showToast(`${errorMessage}`, 'error' as ToastType);
            return null;
        }

        const rsData = await response.json();
        
        // RsData 구조 검증
        if (!rsData.data) {
            console.error('❌ RsData.data가 null입니다:', rsData);
            showToast('서버 응답 형식이 올바르지 않습니다.', 'error' as ToastType);
            return null;
        }

        const result = rsData.data;
        
        // 응답 데이터 구조 로깅
        console.log('OCR 분석 결과:', {
            extractedText: result.extractedText?.substring(0, 100) + '...',
            detectedBookTitle: result.detectedBookTitle,
            confidence: result.confidence,
            searchResultsCount: result.searchResults?.length || 0
        });

        return result;
    };

    // 책 정보 자동 입력 함수
    const autoFillBookFields = (book: BookSearchResult, result: OcrResult): boolean => {
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
        
        showToast(`"${book.bookTitle}" 모든 정보가 자동으로 입력되었습니다!`, 'success' as ToastType);
        return true;
    };

    // OCR 에러 처리 함수
    const handleOcrError = (result: OcrResult | null): boolean => {
        if (!result) {
            resetBookFields();
            return false;
        }

        if (result.searchResults && result.searchResults.length > 0) {
            // 검색 결과가 있으면 자동 입력
            const book = result.searchResults[0];
            console.log('자동 선택된 도서:', book.bookTitle);
            return autoFillBookFields(book, result);
        } else if (result.detectedBookTitle) {
            // 검색 결과가 0건인 경우
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
        } else {
            // OCR 실패
            console.log('OCR 감지 실패 - 신뢰도:', result.confidence);
            showToast('책 제목을 인식하지 못했습니다. 수동으로 입력해주세요.', 'error' as ToastType);
            resetBookFields();
            return false;
        }

        return false;
    };

    // OCR + 알라딘 검색 통합 함수 (중복 제거)
    const handleOcrBookSearch = async (imageFile: File): Promise<boolean> => {
        // OCR 상태를 한 번에 초기화
        setIsOcrProcessing(true);
        setOcrResult(null);
        setProgressValue(0);
        
        // 프로그레스 바 애니메이션 시작
        const progressInterval = startProgressAnimation();

        // 디버깅을 위한 상세 로깅
        console.log('🔍 OCR 요청 시작:', {
            fileName: imageFile.name,
            fileSize: `${(imageFile.size / 1024 / 1024).toFixed(2)}MB`,
            fileType: imageFile.type
        });
        
        // 사용자에게 처리 중임을 알림
        showToast('AI가 책 표지를 분석 중입니다... (3-5초 소요)', 'info');
        
        try {
            // OCR API 호출
            const response = await callOcrApi(imageFile);
            
            // 응답 처리
            const result = await processOcrResponse(response);
            
            if (result) {
                // OCR 결과 저장
                setOcrResult(result);
                
                // 에러 처리 및 자동 입력
                return handleOcrError(result);
            }
            
            return false;
            
        } catch (error) {
            console.error('OCR 네트워크 오류:', error);
            showToast('네트워크 오류가 발생했습니다. 수동으로 검색해주세요.', 'error' as ToastType);
            resetBookFields();
            return false;
            
        } finally {
            // OCR 상태를 한 번에 정리
            setProgressValue(CONSTANTS.PROGRESS_THRESHOLD_100);
            setTimeout(() => {
                setIsOcrProcessing(false);
                setProgressValue(0);
            }, CONSTANTS.OCR_POPUP_DELAY);
            console.log('OCR 처리 완료');
        }
    };

    // 백엔드 API (POST /rent)로 데이터 전송.
    // 1. 이미지 파일이 있다면 먼저 이미지 업로드 API로 전송하여 URL을 받습니다.
    // 2. 받은 이미지 URL과 폼 데이터를 조합하여 대여글 생성 API로 전송합니다.
    const handleSubmit = async (e: React.FormEvent<HTMLFormElement>) => {
        e.preventDefault();

        let imageUrl = CONSTANTS.DEFAULT_IMAGE_URL; // 기본 이미지 URL

        // ✅ 핵심 로직: bookImage가 null이고 previewImageUrl이 defaultImageUrl과 같으면 등록 막기
        if (!bookImage) {
            showToast('책 이미지를 등록해 주세요.', 'error' as ToastType);
            return;
        }

        // 이미지 업로드 처리
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
                    showToast(`이미지 업로드 실패: ${imageUploadRes.statusText || errorText}`, 'error' as ToastType);
                    return;
                }
            }catch(error){
                console.error('이미지 업로드 중 네트워크 오류', error);
                showToast('이미지 업로드 중 오류가 발생했습니다.', 'error' as ToastType);
                return;
            }
        }

        // 폼 데이터를 한 번에 구성
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
                // 에러 응답 처리
                const errorData = await res.json();
                console.error('책 등록 실패', errorData);
                showToast(`책 등록에 실패했습니다. ${errorData.msg || res.statusText}`, 'error' as ToastType);
            }
        } catch(error) {
            // 네트워크 에러 처리
            console.error('책 등록 중 네트워크 에러', error);
            showToast('책 등록 중 네트워크 에러가 발생했습니다.', 'error' as ToastType);
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
                    
                                                              {/* AI 작성 모드 선택 */}
                     <AiModeSelector 
                         isAiModeEnabled={isAiModeEnabled}
                         onToggle={setIsAiModeEnabled}
                     />

                                          {/* 이미지 업로드 섹션 */}
                     <ImageUploadSection
                         bookImage={bookImage}
                         previewImageUrl={previewImageUrl}
                         isAiModeEnabled={isAiModeEnabled}
                         isOcrProcessing={isOcrProcessing}
                         onImageChange={handleImageChange}
                     />

                                          {/* AI 분석 중 로딩 팝업 (AI 모드에서만) */}
                     <AiLoadingPopup 
                         isVisible={isAiModeEnabled && isOcrProcessing}
                         progressValue={progressValue}
                     />

                    {/* 게시글 폼 필드 섹션 */}
                    <PostFormSection
                        title={title}
                        onTitleChange={setTitle}
                        bookCondition={bookCondition}
                        onBookConditionChange={setBookCondition}
                        conditions={conditions}
                        selectedAddress={selectedAddress}
                        onAddressPopupOpen={() => setIsAddressPopupOpen(true)}
                        contents={contents}
                        onContentsChange={setContents}
                        maxContentLength={CONSTANTS.MAX_CONTENT_LENGTH}
                    />

                    {/* 책 검색 섹션 */}
                    <BookSearchSection
                        searchQuery={searchQuery}
                        onSearchQueryChange={setSearchQuery}
                        onBookSelect={selectBook}
                        onSearch={handleBookSearch}
                        searchResults={searchResults}
                        showBookSearchModal={showBookSearchModal}
                        onCloseModal={() => setShowBookSearchModal(false)}
                        currentPage={currentPage}
                        hasMoreResults={hasMoreResults}
                    />

                    {/* 책 정보 폼 필드 섹션 */}
                    <BookFormSection
                        bookTitle={bookTitle}
                        onBookTitleChange={setBookTitle}
                        author={author}
                        onAuthorChange={setAuthor}
                        publisher={publisher}
                        onPublisherChange={setPublisher}
                        category={category}
                        onCategoryChange={setCategory}
                        description={description}
                        onDescriptionChange={setDescription}
                        maxContentLength={CONSTANTS.MAX_CONTENT_LENGTH}
                    />
                
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

            {/* 팝업 모달들 */}
            <PopupModals
                showPopup={showPopup}
                onClosePopup={() => setShowPopup(false)}
                showAiFailurePopup={showAiFailurePopup}
                onCloseAiFailurePopup={() => setShowAiFailurePopup(false)}
                isAddressPopupOpen={isAddressPopupOpen}
                onCloseAddressPopup={() => setIsAddressPopupOpen(false)}
                onSelectAddress={(address: string) => {
                    setSelectedAddress(address);
                    setIsAddressPopupOpen(false);
                }}
            />
            
            {/* 토스트 알림 컴포넌트 */}
            <ToastNotification 
                message={toastState.message} 
                type={toastState.type} 
            />
            
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