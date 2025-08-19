package com.guineafigma.domain.logosong.client;

import com.guineafigma.domain.logosong.dto.request.SunoGenerateRequest;
import com.guineafigma.domain.logosong.dto.response.SunoGenerateResponse;
import com.guineafigma.domain.logosong.dto.response.SunoGenerateRecordInfoResponse;
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
            String url = sunoApiUrl + "/api/v1/generate";
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
        HttpHeaders headers = createHeaders();
        HttpEntity<Void> entity = new HttpEntity<>(headers);

        // 우선 v1 엔드포인트 시도 후, 실패 시 레거시 엔드포인트 폴백
        String[] candidates = new String[] {
                sunoApiUrl + "/api/v1/get?ids=" + taskId,
                sunoApiUrl + "/api/get?ids=" + taskId
        };

        for (String url : candidates) {
            try {
                log.debug("Suno API 상태 확인 요청: url={}, taskId={}", url, taskId);
                ResponseEntity<SunoStatusResponse[]> response = restTemplate.exchange(url, HttpMethod.GET, entity, SunoStatusResponse[].class);

                if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null && response.getBody().length > 0) {
                    SunoStatusResponse statusResponse = response.getBody()[0];
                    log.debug("Suno API 상태 확인 성공: taskId={}, status={}", taskId, statusResponse.getStatus());
                    return statusResponse;
                }
            } catch (HttpClientErrorException e) {
                log.error("Suno API 상태 확인 클라이언트 오류: url={}, taskId={}, status={}, body={}", url, taskId, e.getStatusCode(), e.getResponseBodyAsString());
                if (e.getStatusCode() == HttpStatus.NOT_FOUND) {
                    // 다음 후보 URL 시도
                    continue;
                }
                throw new BusinessException(ErrorCode.SUNO_API_ERROR);
            } catch (HttpServerErrorException e) {
                log.error("Suno API 상태 확인 서버 오류: url={}, taskId={}, status={}, body={}", url, taskId, e.getStatusCode(), e.getResponseBodyAsString());
                // 서버 오류는 일시적일 수 있으므로 상위 재시도 로직에 맡김
                throw new BusinessException(ErrorCode.SUNO_API_ERROR);
            } catch (Exception e) {
                log.error("Suno API 상태 확인 중 예외 발생: url={}, taskId={}", url, taskId, e);
                throw new BusinessException(ErrorCode.SUNO_API_ERROR);
            }
        }

        // 모든 후보가 404로 실패한 경우: 아직 인덱싱/전파 전일 수 있으므로 NOT_FOUND를 반환
        throw new BusinessException(ErrorCode.SUNO_TASK_NOT_FOUND);
    }

    public SunoGenerateRecordInfoResponse getGenerateRecordInfo(String taskId) {
        try {
            String url = sunoApiUrl + "/api/v1/generate/record-info?taskId=" + taskId;
            HttpHeaders headers = createHeaders();
            HttpEntity<Void> entity = new HttpEntity<>(headers);

            log.debug("Suno API generate record-info 요청: url={}, taskId={}", url, taskId);
            ResponseEntity<SunoGenerateRecordInfoResponse> response =
                    restTemplate.exchange(url, HttpMethod.GET, entity, SunoGenerateRecordInfoResponse.class);

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                return response.getBody();
            }
            throw new BusinessException(ErrorCode.SUNO_API_ERROR);
        } catch (HttpClientErrorException e) {
            log.error("Suno API record-info 클라이언트 오류: status={}, body={}", e.getStatusCode(), e.getResponseBodyAsString());
            throw new BusinessException(ErrorCode.SUNO_API_ERROR);
        } catch (Exception e) {
            log.error("Suno API record-info 호출 중 예외 발생", e);
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