package com.guineafigma.domain.user.service;

import com.guineafigma.common.util.EnumValidator;
import com.guineafigma.domain.media.dto.request.MediaUploadRequest;
import com.guineafigma.domain.media.service.MediaService;
import com.guineafigma.domain.user.dto.requestDTO.UserLevelUpdateRequestDTO;
import com.guineafigma.domain.user.dto.response.UserLicenseImageResponseDTO;
import com.guineafigma.global.exception.BusinessException;
import com.guineafigma.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.guineafigma.domain.user.repository.UserRepository;
import com.guineafigma.domain.user.entity.User;
import com.guineafigma.domain.user.enums.Levels;
import org.springframework.web.multipart.MultipartFile;

@Service
@RequiredArgsConstructor
@Transactional
public class UserServiceImpl implements UserService {
    private final UserRepository memberRepository;
    private final MediaService imageService;
    String additionalPath = "qualifications";

    @Override
    public User findMemberByEmail(String email) {
        return memberRepository.findByEmail(email).orElseThrow(()-> new BusinessException(ErrorCode.EMAIL_NOT_FOUND));
    }

    @Override
    public User findById(Long id) {
        return  memberRepository.findById(id).orElseThrow(()-> new BusinessException(ErrorCode.MEMBER_NOT_FOUND));
    }

    @Override
    public User saveMember(User member) {
        return memberRepository.save(member);
    }



    public void updateLevel(Long userId, UserLevelUpdateRequestDTO requestDTO) {
        Levels level = EnumValidator.validateEnum(Levels.class, requestDTO.getLevel().name());


        User member = memberRepository.findById(userId).orElseThrow(()-> new BusinessException(ErrorCode.MEMBER_NOT_FOUND));
        member.setLevel(level);
    }

    @Override
    public UserLicenseImageResponseDTO uploadLicense(MultipartFile image, Long userId) {
        String uploadPath = "users/" + userId + "/license/";

        MediaUploadRequest request = MediaUploadRequest.builder()
                .file(image)
                .uploadPath(uploadPath)
                .build();

        String fileUrl = imageService.uploadMedia(request).getFileUrl();


        return new UserLicenseImageResponseDTO(fileUrl);
    }

}
