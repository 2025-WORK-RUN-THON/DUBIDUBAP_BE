package com.guineafigma.common.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "페이지네이션 정보")
public class BasePaginationDto {
    
    @Schema(description = "페이지당 조회 개수", example = "10")
    private int limit;
    
    @Schema(description = "현재 페이지 번호", example = "1")
    private int currentPage;
    
    @Schema(description = "전체 페이지 수", example = "5")
    private int totalPage;
    
    public static BasePaginationDto of(int limit, int currentPage, int totalPage) {
        return BasePaginationDto.builder()
                .limit(limit)
                .currentPage(currentPage)
                .totalPage(totalPage)
                .build();
    }
}