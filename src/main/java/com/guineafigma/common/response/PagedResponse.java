package com.guineafigma.common.response;

import com.guineafigma.common.dto.BasePaginationDto;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "페이지네이션 응답 데이터")
public class PagedResponse<T> {
    
    @ArraySchema(schema = @Schema(description = "조회된 데이터 목록"))
    private List<T> content;
    
    @Schema(description = "페이지네이션 정보")
    private BasePaginationDto pagination;
    
    public static <T> PagedResponse<T> of(List<T> content, BasePaginationDto pagination) {
        return PagedResponse.<T>builder()
                .content(content)
                .pagination(pagination)
                .build();
    }
    
    public static <T> PagedResponse<T> of(List<T> content, int limit, int currentPage, int totalPage) {
        return PagedResponse.<T>builder()
                .content(content)
                .pagination(BasePaginationDto.of(limit, currentPage, totalPage))
                .build();
    }
}