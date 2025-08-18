package com.guineafigma.global.config;

import com.guineafigma.global.exception.ErrorCode;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.examples.Example;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.media.Content;
import io.swagger.v3.oas.models.media.MediaType;
import io.swagger.v3.oas.models.responses.ApiResponse;
import io.swagger.v3.oas.models.responses.ApiResponses;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import lombok.Builder;
import lombok.Getter;
import org.springdoc.core.customizers.OperationCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.mvc.method.RequestMappingInfo;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Configuration
@SecurityScheme(
        name = "JWT", // 아래에서 사용할 이름
        type = SecuritySchemeType.HTTP,
        scheme = "bearer",
        bearerFormat = "JWT"
)
public class SwaggerConfig {

    @Autowired
    private ApplicationContext applicationContext;

    @Bean
    public OpenAPI openAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("하비뒤밥 (Dubidubap) API")
                        .description("하비뒤밥 - AI 기반 로고송 제작 플랫폼\n\n" +
                                "🎵 **주요 기능**:\n" +
                                "- 브랜드 정보 기반 로고송 가사 생성 (OpenAI)\n" +
                                "- AI 음악 생성 및 업로드 (Suno AI)\n" +
                                "- 사용자 커미니티 및 상호작용 기능\n" +
                                "- S3 미디어 관리 및 다운로드\n\n" +
                                "🔒 **인증**: JWT Bearer Token 기반\n" +
                                "🌐 **서버**: Spring Boot 3.x + MySQL/H2")
                        .version("v1.0.0"))
                .addSecurityItem(new SecurityRequirement().addList("JWT"));
    }

    @Bean
    public OperationCustomizer operationCustomizer() {
        return (Operation operation, HandlerMethod handlerMethod) -> {
            // 실제 API 경로 정보 추출
            String actualPath = extractActualPath(handlerMethod);
            
            // 단일 에러 코드 어노테이션 처리
            ApiErrorExample apiErrorExample = handlerMethod.getMethodAnnotation(ApiErrorExample.class);
            if (apiErrorExample != null) {
                generateErrorCodeResponseExample(operation, new ErrorCode[]{apiErrorExample.value()}, actualPath);
            }

            // 복수 에러 코드 어노테이션 처리
            ApiErrorExamples apiErrorExamples = handlerMethod.getMethodAnnotation(ApiErrorExamples.class);
            if (apiErrorExamples != null) {
                generateErrorCodeResponseExample(operation, apiErrorExamples.value(), actualPath);
            }

            return operation;
        };
    }

    // HandlerMethod에서 실제 API 경로 추출
    private String extractActualPath(HandlerMethod handlerMethod) {
        try {
            RequestMappingHandlerMapping mapping = applicationContext.getBean(RequestMappingHandlerMapping.class);
            Map<RequestMappingInfo, HandlerMethod> handlerMethods = mapping.getHandlerMethods();
            
            for (Map.Entry<RequestMappingInfo, HandlerMethod> entry : handlerMethods.entrySet()) {
                if (entry.getValue().equals(handlerMethod)) {
                    RequestMappingInfo info = entry.getKey();
                    
                    // PathPatternsCondition 확인
                    var pathPatternsCondition = info.getPathPatternsCondition();
                    if (pathPatternsCondition != null && !pathPatternsCondition.getPatterns().isEmpty()) {
                        return pathPatternsCondition.getPatterns().iterator().next().getPatternString();
                    }
                    
                    // PatternsCondition 확인
                    var patternsCondition = info.getPatternsCondition();
                    if (patternsCondition != null && !patternsCondition.getPatterns().isEmpty()) {
                        return patternsCondition.getPatterns().iterator().next();
                    }
                }
            }
        } catch (Exception e) {
            // 경로 추출 실패시 기본값 사용
        }
        
        return "/api/example";
    }

    // 에러 코드들을 기반으로 Swagger 응답 예제를 생성
    private void generateErrorCodeResponseExample(Operation operation, ErrorCode[] errorCodes, String actualPath) {
        ApiResponses responses = operation.getResponses();

        // HTTP 상태 코드별로 에러 코드들을 그룹화
        Map<Integer, List<ExampleHolder>> statusWithExampleHolders = Arrays.stream(errorCodes)
                .map(errorCode -> ExampleHolder.builder()
                        .example(createErrorExample(errorCode, actualPath))
                        .name(errorCode.name())
                        .httpStatus(errorCode.getStatus().value())
                        .build())
                .collect(Collectors.groupingBy(ExampleHolder::getHttpStatus));

        // 상태 코드별로 ApiResponse에 예제들 추가
        addExamplesToResponses(responses, statusWithExampleHolders);
    }

    // ErrorCode를 기반으로 Example 객체 생성
    private Example createErrorExample(ErrorCode errorCode, String actualPath) {
        // 에러 응답 객체 생성
        Map<String, Object> errorResponse = new LinkedHashMap<>();
        errorResponse.put("timestamp", "2025-06-30T12:00:00.000000");
        errorResponse.put("status", errorCode.getStatus().value());
        errorResponse.put("code", errorCode.getCode());
        errorResponse.put("message", errorCode.getMessage());
        errorResponse.put("path", actualPath);

        Example example = new Example();
        example.description(errorCode.getMessage());
        example.setValue(errorResponse);
        
        return example;
    }

    // 상태 코드별로 그룹화된 예제들을 ApiResponses에 추가
    private void addExamplesToResponses(ApiResponses responses, Map<Integer, List<ExampleHolder>> statusWithExampleHolders) {
        statusWithExampleHolders.forEach((httpStatus, exampleHolders) -> {
            // 해당 상태 코드에 대한 ApiResponse가 이미 존재하는지 확인
            String statusKey = httpStatus.toString();
            ApiResponse apiResponse = responses.get(statusKey);
            
            if (apiResponse == null) {
                apiResponse = new ApiResponse();
                apiResponse.setDescription("에러 응답");
                apiResponse.setContent(new Content());
            }

            // Content와 MediaType 설정
            Content content = apiResponse.getContent();
            MediaType mediaType = content.get("application/json");
            
            if (mediaType == null) {
                mediaType = new MediaType();
                content.addMediaType("application/json", mediaType);
            }

            // Examples 맵 설정
            Map<String, Example> examples = mediaType.getExamples();
            if (examples == null) {
                examples = new HashMap<>();
                mediaType.setExamples(examples);
            }

            // 각 에러 코드별 예제 추가
            for (ExampleHolder exampleHolder : exampleHolders) {
                examples.put(exampleHolder.getName(), exampleHolder.getExample());
            }

            // ApiResponse를 responses에 추가
            responses.addApiResponse(statusKey, apiResponse);
        });
    }

    // 예제 정보를 담는 내부 클래스
    @Getter
    @Builder
    private static class ExampleHolder {
        private final Example example;
        private final String name;
        private final int httpStatus;
    }

    
    // 단일 에러 코드 예제를 위한 어노테이션
    @Target(ElementType.METHOD)
    @Retention(RetentionPolicy.RUNTIME)
    public @interface ApiErrorExample {
        ErrorCode value();
    }

    // 복수 에러 코드 예제를 위한 어노테이션
    @Target(ElementType.METHOD)
    @Retention(RetentionPolicy.RUNTIME)
    public @interface ApiErrorExamples {
        ErrorCode[] value();
    }

    // 성공 응답 예제를 위한 어노테이션
    @Target(ElementType.METHOD)
    @Retention(RetentionPolicy.RUNTIME)
    public @interface ApiSuccessResponse {
        String message() default "요청이 성공적으로 처리되었습니다.";
        Class<?> dataType() default Object.class;
        String dataExample() default "";
        boolean isArray() default false;
    }
}
