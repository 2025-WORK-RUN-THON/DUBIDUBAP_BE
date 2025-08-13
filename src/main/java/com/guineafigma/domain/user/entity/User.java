package com.guineafigma.domain.user.entity;

import com.guineafigma.common.entity.BaseEntity;
import com.guineafigma.domain.user.enums.Levels;
import com.guineafigma.domain.user.enums.Role;
import com.guineafigma.common.enums.SocialType;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.*;

@Entity
@Table(name = "users")
@Builder
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class User extends BaseEntity {


    @NotNull
    private String email;

    @Enumerated(EnumType.STRING)
    @NotNull
    private SocialType socialType;

    @Enumerated(EnumType.STRING)
    @NotNull
    private Role role;

    @Enumerated(EnumType.STRING)
    private Levels level;
    
}
