package com.yiliao.common.data.config;

import com.baomidou.mybatisplus.annotation.DbType;
import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.PaginationInnerInterceptor;
import com.yiliao.common.data.fill.UserContextProvider;
import com.yiliao.common.data.fill.YiliaoMetaObjectHandler;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;

/**
 * MyBatis-Plus 自动配置（specs/global/20 §1）：分页插件 + 字段填充。
 * UserContextProvider 未提供服务实现时降级为不填操作人（MQ/定时任务上下文）。
 */
@AutoConfiguration
@ConditionalOnClass(MybatisPlusInterceptor.class)
public class YiliaoDataAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public MybatisPlusInterceptor yiliaoMybatisPlusInterceptor() {
        MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor();
        interceptor.addInnerInterceptor(new PaginationInnerInterceptor(DbType.MYSQL));
        return interceptor;
    }

    @Bean
    @ConditionalOnMissingBean
    public YiliaoMetaObjectHandler yiliaoMetaObjectHandler(
            org.springframework.beans.factory.ObjectProvider<UserContextProvider> provider) {
        return new YiliaoMetaObjectHandler(provider.getIfAvailable());
    }
}
