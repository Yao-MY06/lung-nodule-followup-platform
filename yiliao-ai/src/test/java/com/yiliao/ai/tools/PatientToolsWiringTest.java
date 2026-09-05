package com.yiliao.ai.tools;

import com.yiliao.api.followup.FollowupApi;
import com.yiliao.api.nodule.NoduleApi;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * 构造器装配冒烟测试（无 DB / 无 LLM / 无 Nacos 依赖）。
 * <p>
 * T4 教训（2026-09-05）：PatientTools 引入双构造器后未标 @Autowired，
 * Spring 组件扫描回退无参构造 → NoSuchMethodException → yiliao-ai 启动失败，
 * 而全模块单测均为纯 Mockito（直接 new / @Mock），对上下文装配失败完全不可见
 * （"单测全绿但启动失败"盲区）。本用例以最小 Spring 上下文把"bean 能否装配"
 * 纳入 mvn test 可见范围：修复回退（删掉 @Autowired）时本测试必须失败。
 */
class PatientToolsWiringTest {

    private AnnotationConfigApplicationContext ctx;

    @AfterEach
    void tearDown() {
        if (ctx != null) {
            ctx.close();
        }
    }

    @Test
    void patientToolsBeanResolvableViaConstructorInjection() {
        ctx = new AnnotationConfigApplicationContext();
        ctx.registerBean(FollowupApi.class, () -> Mockito.mock(FollowupApi.class));
        ctx.registerBean(NoduleApi.class, () -> Mockito.mock(NoduleApi.class));
        ctx.register(PatientTools.class);
        ctx.refresh();

        PatientTools bean = ctx.getBean(PatientTools.class);
        assertNotNull(bean, "PatientTools 必须能经构造器注入装配为 bean（T4 启动缺陷回归门禁）");
    }
}
