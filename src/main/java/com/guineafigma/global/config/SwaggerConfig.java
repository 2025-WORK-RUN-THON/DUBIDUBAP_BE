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
import io.swagger.v3.oas.models.media.ObjectSchema;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.responses.ApiResponse;
import io.swagger.v3.oas.models.responses.ApiResponses;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.parameters.Parameter;
import lombok.Builder;
import lombok.Getter;
import org.springdoc.core.customizers.OperationCustomizer;
import org.springdoc.core.models.GroupedOpenApi;
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
                        .title("두비두밥 (Dubidubap) API")
                        .description("AI 기반 로고송 제작 플랫폼 API\n\n" +
                                "그룹:\n- all: 전체 API\n- auth: 인증\n- logosongs: 로고송\n- system: 시스템 상태\n")
                        .version("v1.0.0"))
                .addSecurityItem(new SecurityRequirement().addList("JWT"));
    }

    @Bean
    public GroupedOpenApi defaultGroup() {
        return GroupedOpenApi.builder()
                .group("all")
                .pathsToMatch("/api/v1/**")
                .addOperationCustomizer(operationCustomizer())
                .build();
    }

    @Bean
    public GroupedOpenApi authGroup() {
        return GroupedOpenApi.builder()
                .group("auth")
                .pathsToMatch("/api/v1/auth/**")
                .addOperationCustomizer(operationCustomizer())
                .build();
    }

    @Bean
    public GroupedOpenApi logoSongsGroup() {
        return GroupedOpenApi.builder()
                .group("logosongs")
                .pathsToMatch("/api/v1/logosongs/**")
                .addOperationCustomizer(operationCustomizer())
                .build();
    }

    @Bean
    public GroupedOpenApi systemGroup() {
        return GroupedOpenApi.builder()
                .group("system")
                .pathsToMatch("/api/v1/system/**")
                .addOperationCustomizer(operationCustomizer())
                .build();
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

            // 성공 응답 예제 처리
            ApiSuccessResponse apiSuccessResponse = handlerMethod.getMethodAnnotation(ApiSuccessResponse.class);
            if (apiSuccessResponse != null) {
                generateSuccessResponseExample(operation, apiSuccessResponse, actualPath);
            }

            // 페이지네이션 파라미터 한국어 문서 보강
            enhancePaginationParameters(operation);

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
        errorResponse.put("timestamp", "2025-08-19T12:00:00.000000");
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
            if (mediaType.getSchema() == null) {
                mediaType.setSchema(new ObjectSchema());
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

    // 페이지네이션 파라미터 설명/예제 보강 (page, size, sort)
    private void enhancePaginationParameters(Operation operation) {
        if (operation.getParameters() == null || operation.getParameters().isEmpty()) {
            return;
        }

        for (Parameter p : operation.getParameters()) {
            if (p == null || p.getName() == null) continue;
            String name = p.getName();
            if ("page".equals(name)) {
                p.setRequired(false);
                p.setDescription("0부터 시작하는 페이지 번호 (예: 0)");
                p.setExample(0);
                Schema<?> schema = p.getSchema() != null ? p.getSchema() : new Schema<>().type("integer");
                schema.setMinimum(java.math.BigDecimal.ZERO);
                p.setSchema(schema);
            } else if ("size".equals(name)) {
                p.setRequired(false);
                p.setDescription("페이지 당 항목 수 (1~100, 기본값 10)");
                p.setExample(10);
                Schema<?> schema = p.getSchema() != null ? p.getSchema() : new Schema<>().type("integer");
                schema.setMinimum(java.math.BigDecimal.ONE);
                schema.setMaximum(new java.math.BigDecimal(100));
                p.setSchema(schema);
            } else if ("sort".equals(name)) {
                p.setRequired(false);
                p.setDescription("정렬 기준, 쉼표로 방향 지정 (예: createdAt,desc | likeCount,asc)");
                p.setExample("createdAt,desc");
                Schema<?> schema = p.getSchema() != null ? p.getSchema() : new Schema<>().type("string");
                p.setSchema(schema);
            }
        }
    }

    // 성공 응답 예제 생성 (200)
    private void generateSuccessResponseExample(Operation operation, ApiSuccessResponse successMeta, String actualPath) {
        ApiResponses responses = operation.getResponses();

        String successKey = String.valueOf(successMeta.httpStatus());
        ApiResponse apiResponse = responses.get(successKey);
        if (apiResponse == null) {
            apiResponse = new ApiResponse();
            apiResponse.setDescription(successMeta.message());
            apiResponse.setContent(new Content());
        }

        Content content = apiResponse.getContent();
        MediaType mediaType = content.get("application/json");
        if (mediaType == null) {
            mediaType = new MediaType();
            content.addMediaType("application/json", mediaType);
        }
        if (mediaType.getSchema() == null) {
            mediaType.setSchema(new ObjectSchema());
        }

        Map<String, Example> examples = mediaType.getExamples();
        if (examples == null) {
            examples = new HashMap<>();
            mediaType.setExamples(examples);
        }

        Map<String, Object> successResponse = new LinkedHashMap<>();
        successResponse.put("timestamp", "2025-08-19T12:00:00.000000");
        successResponse.put("status", successMeta.httpStatus());
        successResponse.put("code", "SUCCESS");
        successResponse.put("message", successMeta.message());
        successResponse.put("path", actualPath);

        // dataExample 문자열이 주어지면 JSON으로 파싱 시도 후 실패 시 문자열로 삽입
        if (successMeta.dataExample() != null && !successMeta.dataExample().isEmpty()) {
            Object dataValue = successMeta.dataExample();
            successResponse.put("data", dataValue);
        } else {
            successResponse.put("data", new LinkedHashMap<>());
        }

        Example example = new Example();
        example.description(successMeta.message());
        example.setValue(successResponse);

        examples.put("SUCCESS", example);

        responses.addApiResponse(successKey, apiResponse);
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
        int httpStatus() default 200;
    }
}
