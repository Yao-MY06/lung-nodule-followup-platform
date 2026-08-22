package com.yiliao.followup.config;

import com.yiliao.common.core.constant.YiliaoConstants;
import com.yiliao.common.data.fill.UserContextProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@Configuration
public class FollowupBeanConfig {

    /** 操作人填充：取网关透传 X-User-Id（specs/global/20 §1）。 */
    @Bean
    public UserContextProvider userContextProvider() {
        return () -> {
            if (RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes attributes) {
                String userId = attributes.getRequest().getHeader(YiliaoConstants.HEADER_USER_ID);
                if (userId != null && !userId.isBlank()) {
                    return Long.valueOf(userId);
                }
            }
            return null;
        };
    }
}
