package com.guineafigma.domain.logosong.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class LogoSongLikeId implements Serializable {
    private Long userId;
    private Long logosongId;
}