package com.guineafigma;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@ActiveProfiles("dev")
@TestPropertySource(properties = {
    "spring.datasource.url=jdbc:h2:mem:testdb;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE",
    "spring.datasource.driver-class-name=org.h2.Driver",
    "spring.datasource.username=sa",
    "spring.datasource.password=",
    "spring.jpa.hibernate.ddl-auto=create-drop",
    "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect",
    "spring.cloud.aws.stack.auto=false",
    "spring.cloud.aws.region.auto=false",
    "cloud.aws.region.static=ap-northeast-2",
    "cloud.aws.s3.bucket=test-bucket",
    "cloud.aws.credentials.access-key=test-key",
    "cloud.aws.credentials.secret-key=test-secret",
    "jwt.secret-key=test-jwt-secret-key-for-testing-purposes-only"
})
class GuineafigmaApplicationTests {

    @Test
    void contextLoads() {
    }

} 