export interface PageResponse<T> {
    content: T[];
    pageInfo: PageInfo;
}

export interface PageInfo {
    currentPage: number;
    totalPages: number;
    totalElements: number;
    currentPageElements: number;
    size: number;
}