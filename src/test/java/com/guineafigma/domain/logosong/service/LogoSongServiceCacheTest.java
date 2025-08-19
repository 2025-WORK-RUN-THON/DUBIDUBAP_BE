package com.guineafigma.domain.logosong.service;

import com.guineafigma.domain.logosong.dto.request.LogoSongCreateRequest;
import com.guineafigma.domain.logosong.dto.response.LogoSongResponse;
import com.guineafigma.domain.logosong.repository.LogoSongRepository;
import com.guineafigma.utils.TestDataBuilder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@SpringBootTest
@ActiveProfiles("dev")
@Transactional
@DisplayName("LogoSongService 캐시 동작 테스트")
class LogoSongServiceCacheTest {

    @Autowired
    private LogoSongService logoSongService;

    @SpyBean
    private LogoSongRepository logoSongRepository;

    private Long logoSongId;

    @BeforeEach
    void setUp() {
        LogoSongCreateRequest req = TestDataBuilder.createValidLogoSongRequest();
        LogoSongResponse created = logoSongService.createLogoSong(req);
        logoSongId = created.getId();
        // 초기화: 호출 카운트 명확히 하기 위해
        Mockito.clearInvocations(logoSongRepository);
    }

    @Test
    @DisplayName("단건 조회 캐시 - 두 번째 호출부터 캐시 적중")
    void getLogoSong_CachedOnSecondCall() {
        // when
        LogoSongResponse first = logoSongService.getLogoSong(logoSongId);
        LogoSongResponse second = logoSongService.getLogoSong(logoSongId);

        // then
        assertThat(first.getId()).isEqualTo(second.getId());
        verify(logoSongRepository, times(1)).findById(logoSongId);
    }

    @Test
    @DisplayName("목록 조회 캐시 - 동일 페이지 파라미터는 두 번째 호출부터 캐시 적중")
    void getAllLogoSongs_ListCached() {
        // given: 데이터 더 생성
        for (int i = 0; i < 3; i++) {
            logoSongService.createLogoSong(TestDataBuilder.createValidLogoSongRequest());
        }
        Mockito.clearInvocations(logoSongRepository);

        var pageable = PageRequest.of(0, 10, Sort.by("createdAt").descending());

        // when
        logoSongService.getAllLogoSongs(pageable);
        logoSongService.getAllLogoSongs(pageable);

        // then: findByIsPublicTrue가 호출되어야 함 (공개만 조회)
        verify(logoSongRepository, times(1)).findByIsPublicTrue(pageable);
    }

    @Test
    @DisplayName("좋아요 토글 시 캐시 무효화 - 이후 단건 조회는 DB 재조회")
    void toggleLike_EvictsCaches() {
        // given: 단건 캐시 적중 상태를 만들기
        logoSongService.getLogoSong(logoSongId);
        logoSongService.getLogoSong(logoSongId);
        verify(logoSongRepository, times(1)).findById(logoSongId);
        Mockito.clearInvocations(logoSongRepository);

        // when: 좋아요 토글 → 캐시 무효화
        logoSongService.toggleLike(logoSongId, 123L);

        // then: toggleLike 내부에서 1회, 이후 조회에서 1회 → 총 2회
        logoSongService.getLogoSong(logoSongId);
        verify(logoSongRepository, times(2)).findById(logoSongId);
    }

    @Test
    @DisplayName("공개 여부 변경 시 캐시 무효화 - 이후 목록 조회는 DB 재조회")
    void updateVisibility_EvictsCaches() {
        // given: 공개 로고송 생성 및 설정
        LogoSongCreateRequest req = TestDataBuilder.createValidLogoSongRequest();
        req.setServiceName("캐시 무효화 테스트용");
        LogoSongResponse created = logoSongService.createLogoSong(req);
        Long testId = created.getId();
        
        // 공개로 설정
        logoSongService.updateVisibility(testId, true);
        
        // 목록 캐시 적중 상태를 만들기
        var pageable = PageRequest.of(0, 10, Sort.by("createdAt").descending());
        logoSongService.getAllLogoSongs(pageable);
        logoSongService.getAllLogoSongs(pageable);
        verify(logoSongRepository, times(1)).findByIsPublicTrue(pageable);
        Mockito.clearInvocations(logoSongRepository);

        // when: 공개 여부 변경 → 캐시 무효화
        logoSongService.updateVisibility(testId, false);

        // then: 이후 목록 조회는 DB 재조회
        logoSongService.getAllLogoSongs(pageable);
        verify(logoSongRepository, times(1)).findByIsPublicTrue(pageable);
    }

    @Test
    @DisplayName("공개 로고송만 목록에 노출 - 비공개 로고송은 제외")
    void getAllLogoSongs_OnlyPublicVisible() {
        // given: 공개/비공개 로고송 생성
        LogoSongCreateRequest publicReq = TestDataBuilder.createValidLogoSongRequest();
        publicReq.setServiceName("공개 테스트 서비스");
        LogoSongCreateRequest privateReq = TestDataBuilder.createValidLogoSongRequest();
        privateReq.setServiceName("비공개 테스트 서비스");
        
        LogoSongResponse publicLogoSong = logoSongService.createLogoSong(publicReq);
        LogoSongResponse privateLogoSong = logoSongService.createLogoSong(privateReq);
        
        // 공개 로고송 설정 (먼저)
        logoSongService.updateVisibility(publicLogoSong.getId(), true);
        // 비공개 로고송 설정
        logoSongService.updateVisibility(privateLogoSong.getId(), false);
        
        Mockito.clearInvocations(logoSongRepository);

        // when: 전체 목록 조회 (공개만)
        var pageable = PageRequest.of(0, 10);
        var result = logoSongService.getAllLogoSongs(pageable);

        // then: 공개 로고송만 조회됨
        assertThat(result.getContent()).hasSizeGreaterThanOrEqualTo(1);
        // 공개 로고송이 목록에 포함되어 있는지 확인
        boolean hasPublicLogoSong = result.getContent().stream()
                .anyMatch(logoSong -> logoSong.getServiceName().equals("공개 테스트 서비스"));
        assertThat(hasPublicLogoSong).isTrue();
        verify(logoSongRepository, times(1)).findByIsPublicTrue(pageable);
    }

    @Test
    @DisplayName("내 로고송은 공개 여부와 관계없이 조회 가능")
    void getMyLogoSongs_AllVisibleRegardlessOfPublic() {
        // given: 공개/비공개 로고송 생성
        LogoSongCreateRequest publicReq = TestDataBuilder.createValidLogoSongRequest();
        publicReq.setServiceName("공개 테스트 서비스");
        LogoSongCreateRequest privateReq = TestDataBuilder.createValidLogoSongRequest();
        privateReq.setServiceName("비공개 테스트 서비스");

        LogoSongResponse publicLogoSong = logoSongService.createLogoSong(publicReq);
        LogoSongResponse privateLogoSong = logoSongService.createLogoSong(privateReq);

        // 공개/비공개 설정
        logoSongService.updateVisibility(publicLogoSong.getId(), true);
        logoSongService.updateVisibility(privateLogoSong.getId(), false);

        Mockito.clearInvocations(logoSongRepository);

        // when: 내 로고송 목록 조회 (정확한 소유자 ID 사용)
        var pageable = PageRequest.of(0, 10);
        Long ownerId = publicLogoSong.getUserId();
        var result = logoSongService.getMyLogoSongs(ownerId, pageable);

        // then: 공개/비공개 모두 조회됨
        assertThat(result.getContent()).hasSizeGreaterThanOrEqualTo(2);
        boolean hasPublicLogoSong = result.getContent().stream()
                .anyMatch(logoSong -> logoSong.getServiceName().equals("공개 테스트 서비스"));
        boolean hasPrivateLogoSong = result.getContent().stream()
                .anyMatch(logoSong -> logoSong.getServiceName().equals("비공개 테스트 서비스"));
        assertThat(hasPublicLogoSong).isTrue();
        assertThat(hasPrivateLogoSong).isTrue();
        verify(logoSongRepository, times(1)).findByUser_Id(ownerId, pageable);
    }
}


