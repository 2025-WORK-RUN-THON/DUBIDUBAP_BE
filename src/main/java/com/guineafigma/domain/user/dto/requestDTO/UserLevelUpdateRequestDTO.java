package com.guineafigma.domain.user.dto.requestDTO;

import com.guineafigma.domain.user.enums.Levels;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;

@Getter
public class UserLevelUpdateRequestDTO {
    @Schema(description = "Levels", example = "OPEN_WATER_DIVER", nullable = false)
    private Levels level;
}
