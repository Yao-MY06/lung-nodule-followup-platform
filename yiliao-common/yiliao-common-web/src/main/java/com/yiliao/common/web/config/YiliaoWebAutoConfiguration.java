package com.yiliao.common.web.config;

import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateDeserializer;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateTimeDeserializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateSerializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateTimeSerializer;
import com.yiliao.common.web.handler.GlobalExceptionHandler;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.jackson.Jackson2ObjectMapperBuilderCustomizer;
import org.springframework.context.annotation.Bean;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;

/**
 * Web 自动配置：全局异常处理 + Jackson 日期格式（specs/global/10 §1，yyyy-MM-dd HH:mm:ss，GMT+8）。
 */
@AutoConfiguration
public class YiliaoWebAutoConfiguration {

    private static final DateTimeFormatter DATETIME = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    @Bean
    public GlobalExceptionHandler globalExceptionHandler() {
        return new GlobalExceptionHandler();
    }

    @Bean
    public Jackson2ObjectMapperBuilderCustomizer yiliaoJacksonCustomizer() {
        return builder -> builder
                .serializersByType(Map.of(
                        LocalDateTime.class, new LocalDateTimeSerializer(DATETIME),
                        LocalDate.class, new LocalDateSerializer(DATE)))
                .deserializersByType(Map.of(
                        LocalDateTime.class, new LocalDateTimeDeserializer(DATETIME),
                        LocalDate.class, new LocalDateDeserializer(DATE)));
    }
}
