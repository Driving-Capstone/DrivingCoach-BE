package com.drivingcoach.backend.domain.driving.domain.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;
import org.springframework.data.domain.Page;

import java.util.List;
import java.util.function.Function;

/**
 * ✅ PageResponse<T>
 *
 * 용도
 *  - Spring Data 의 Page<T> 를 API 응답에 적합한 형태로 변환하여 내려주는 공통 페이지 DTO입니다.
 *  - 목록/페이지네이션이 필요한 거의 모든 엔드포인트에서 재사용 가능합니다.
 *
 * 구성
 *  - content       : 실제 데이터 리스트 (T 타입)
 *  - page          : 현재 페이지(0-base)
 *  - size          : 페이지 크기
 *  - totalElements : 전체 항목 수
 *  - totalPages    : 전체 페이지 수
 *  - hasNext       : 다음 페이지 존재 여부
 *  - hasPrevious   : 이전 페이지 존재 여부
 *
 * 사용 예시
 *  - Page<DrivingRecord> p = service.pageMyRecords(...);
 *  - Page<DrivingRecordResponse> mapped = p.map(DrivingRecordResponse::from);
 *  - return ApiResponse.ok(PageResponse.from(mapped));
 *
 * 추가 헬퍼
 *  - of(page, mapper): Page<S> → PageResponse<T>로 변환(매핑 함수 전달)하는 오버로드 제공
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PageResponse<T> {

    @Schema(description = "페이지 데이터")
    private List<T> content;

    @Schema(description = "현재 페이지(0-base)", example = "0")
    private int page;

    @Schema(description = "페이지 크기", example = "20")
    private int size;

    @Schema(description = "전체 항목 수", example = "153")
    private long totalElements;

    @Schema(description = "전체 페이지 수", example = "8")
    private int totalPages;

    @Schema(description = "다음 페이지 존재 여부", example = "true")
    private boolean hasNext;

    @Schema(description = "이전 페이지 존재 여부", example = "false")
    private boolean hasPrevious;

    /* ------------------------ 정적 팩토리 ------------------------ */

    /**
     * Page<T> → PageResponse<T>
     */
    public static <T> PageResponse<T> from(Page<T> page) {
        return PageResponse.<T>builder()
                .content(page.getContent())
                .page(page.getNumber())
                .size(page.getSize())
                .totalElements(page.getTotalElements())
                .totalPages(page.getTotalPages())
                .hasNext(page.hasNext())
                .hasPrevious(page.hasPrevious())
                .build();
    }

    /**
     * Page<S> + 매퍼 → PageResponse<T>
     * - 엔티티를 DTO로 변환해야 하는 경우 유용
     */
    public static <S, T> PageResponse<T> of(Page<S> page, Function<S, T> mapper) {
        List<T> mapped = page.getContent().stream().map(mapper).toList();
        return PageResponse.<T>builder()
                .content(mapped)
                .page(page.getNumber())
                .size(page.getSize())
                .totalElements(page.getTotalElements())
                .totalPages(page.getTotalPages())
                .hasNext(page.hasNext())
                .hasPrevious(page.hasPrevious())
                .build();
    }
}
