package com.guineafigma.global.config.properties;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@Getter
@Setter
@ConfigurationProperties("jwt")
//public class JwtProperties {
//    private String secretKey="defaultSecretKeyWhichIsSufficientlyLongForHMAC";
//    private Expiration expiration = new Expiration();
//
//    @Getter
//    @Setter
//    public static class Expiration{
//        private Long access = 14400000L;  // 기본값 (4시간)
//        private Long refresh = 60480000L;  // 기본값 (7일)
//    }
//}
public class JwtProperties {
    private String secretKey="";
    private Expiration expiration;

    @Getter
    @Setter
    public static class Expiration{
        private Long access;  // 기본값 (4시간)
        private Long refresh;  // 기본값 (7일)
    }
}
