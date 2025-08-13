package com.guineafigma.domain.user.service;

import com.guineafigma.domain.user.dto.response.UserLicenseImageResponseDTO;
import com.guineafigma.domain.user.entity.User;
import com.guineafigma.domain.user.dto.requestDTO.UserLevelUpdateRequestDTO;
import org.springframework.web.multipart.MultipartFile;

public interface UserService {
    User findMemberByEmail(String email);
    User findById(Long id);
    User saveMember(User member);
    void updateLevel(Long userId, UserLevelUpdateRequestDTO requestDTO);
    UserLicenseImageResponseDTO uploadLicense(MultipartFile image, Long userId);
}
