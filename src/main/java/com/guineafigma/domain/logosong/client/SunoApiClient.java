package com.guineafigma.domain.logosong.client;

import com.guineafigma.domain.logosong.dto.request.SunoGenerateRequest;
import com.guineafigma.domain.logosong.dto.response.SunoGenerateResponse;
import com.guineafigma.domain.logosong.dto.response.SunoStatusResponse;
import com.guineafigma.global.exception.BusinessException;
import com.guineafigma.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestTemplate;

@Slf4j
@Component
@RequiredArgsConstructor
public class SunoApiClient {

    private final RestTemplate restTemplate;

    @Value("${suno.api.key}")
    private String sunoApiKey;

    @Value("${suno.api.url:https://api.sunoapi.org}")
    private String sunoApiUrl;

    @Retryable(
            value = {HttpServerErrorException.class},
            maxAttempts = 3,
            backoff = @Backoff(delay = 2000, multiplier = 2)
    )
    public SunoGenerateResponse generateMusic(SunoGenerateRequest request) {
        try {
            String url = sunoApiUrl + "/api/generate";
            HttpHeaders headers = createHeaders();
            HttpEntity<SunoGenerateRequest> entity = new HttpEntity<>(request, headers);

            log.info("Suno API 음악 생성 요청: {}", request.getTitle());
            ResponseEntity<SunoGenerateResponse> response = restTemplate.postForEntity(url, entity, SunoGenerateResponse.class);

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                log.info("Suno API 음악 생성 요청 성공: taskId={}", response.getBody().getId());
                return response.getBody();
            } else {
                log.error("Suno API 응답 오류: status={}", response.getStatusCode());
                throw new BusinessException(ErrorCode.SUNO_API_ERROR);
            }
        } catch (HttpClientErrorException e) {
            log.error("Suno API 클라이언트 오류: status={}, body={}", e.getStatusCode(), e.getResponseBodyAsString());
            if (e.getStatusCode() == HttpStatus.UNAUTHORIZED) {
                throw new BusinessException(ErrorCode.SUNO_API_UNAUTHORIZED);
            } else if (e.getStatusCode() == HttpStatus.BAD_REQUEST) {
                throw new BusinessException(ErrorCode.SUNO_API_BAD_REQUEST);
            } else {
                throw new BusinessException(ErrorCode.SUNO_API_ERROR);
            }
        } catch (HttpServerErrorException e) {
            log.error("Suno API 서버 오류: status={}, body={}", e.getStatusCode(), e.getResponseBodyAsString());
            throw new BusinessException(ErrorCode.SUNO_API_ERROR);
        } catch (Exception e) {
            log.error("Suno API 호출 중 예외 발생", e);
            throw new BusinessException(ErrorCode.SUNO_API_ERROR);
        }
    }

    @Retryable(
            value = {HttpServerErrorException.class},
            maxAttempts = 3,
            backoff = @Backoff(delay = 1000, multiplier = 1.5)
    )
    public SunoStatusResponse getGenerationStatus(String taskId) {
        try {
            String url = sunoApiUrl + "/api/get?ids=" + taskId;
            HttpHeaders headers = createHeaders();
            HttpEntity<Void> entity = new HttpEntity<>(headers);

            log.debug("Suno API 상태 확인 요청: taskId={}", taskId);
            ResponseEntity<SunoStatusResponse[]> response = restTemplate.exchange(url, HttpMethod.GET, entity, SunoStatusResponse[].class);

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null && response.getBody().length > 0) {
                SunoStatusResponse statusResponse = response.getBody()[0];
                log.debug("Suno API 상태 확인 성공: taskId={}, status={}", taskId, statusResponse.getStatus());
                return statusResponse;
            } else {
                log.error("Suno API 상태 확인 실패: taskId={}, status={}", taskId, response.getStatusCode());
                throw new BusinessException(ErrorCode.SUNO_API_ERROR);
            }
        } catch (HttpClientErrorException e) {
            log.error("Suno API 상태 확인 클라이언트 오류: taskId={}, status={}, body={}", taskId, e.getStatusCode(), e.getResponseBodyAsString());
            if (e.getStatusCode() == HttpStatus.NOT_FOUND) {
                throw new BusinessException(ErrorCode.SUNO_TASK_NOT_FOUND);
            } else {
                throw new BusinessException(ErrorCode.SUNO_API_ERROR);
            }
        } catch (HttpServerErrorException e) {
            log.error("Suno API 상태 확인 서버 오류: taskId={}, status={}, body={}", taskId, e.getStatusCode(), e.getResponseBodyAsString());
            throw new BusinessException(ErrorCode.SUNO_API_ERROR);
        } catch (Exception e) {
            log.error("Suno API 상태 확인 중 예외 발생: taskId={}", taskId, e);
            throw new BusinessException(ErrorCode.SUNO_API_ERROR);
        }
    }

    public Double getRemainingCredits() {
        try {
            String url = sunoApiUrl + "/api/get-credits";
            HttpHeaders headers = createHeaders();
            HttpEntity<Void> entity = new HttpEntity<>(headers);

            ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.GET, entity, String.class);
            
            if (response.getStatusCode().is2xxSuccessful()) {
                // 크레딧 정보 파싱 로직 추가 필요
                log.info("Suno API 크레딧 조회 성공");
                return 100.0; // 임시값
            } else {
                log.warn("Suno API 크레딧 조회 실패: status={}", response.getStatusCode());
                return null;
            }
        } catch (Exception e) {
            log.error("Suno API 크레딧 조회 중 예외 발생", e);
            return null;
        }
    }

    private HttpHeaders createHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Authorization", "Bearer " + sunoApiKey);
        headers.set("User-Agent", "GuineaFigma-LogoSong-Service/1.0");
        return headers;
    }
}