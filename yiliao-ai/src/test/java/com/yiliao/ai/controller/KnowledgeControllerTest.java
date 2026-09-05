package com.yiliao.ai.controller;

import com.yiliao.ai.entity.KnowledgeDoc;
import com.yiliao.ai.mapper.KnowledgeDocMapper;
import com.yiliao.ai.rag.KnowledgeBaseService;
import com.yiliao.common.core.constant.YiliaoConstants;
import com.yiliao.common.core.error.CommonErrorCode;
import com.yiliao.common.core.exception.BizException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class KnowledgeControllerTest {

    @Mock
    private KnowledgeBaseService knowledgeBase;
    @Mock
    private KnowledgeDocMapper docMapper;

    @AfterEach
    void tearDown() {
        RequestContextHolder.resetRequestAttributes();
    }

    @Test
    void importDocRejectsWhenRequestContextIsMissing() {
        KnowledgeController controller = new KnowledgeController(knowledgeBase, docMapper);

        assertThrows(BizException.class, () -> controller.importDoc(request()));
        verify(knowledgeBase, never()).importDoc(request().docName(), request().content());
        verify(docMapper, never()).insert(org.mockito.ArgumentMatchers.any(KnowledgeDoc.class));
    }

    @Test
    void importDocRejectsNonAdminRole() {
        setRoles("DOCTOR,NURSE");
        KnowledgeController controller = new KnowledgeController(knowledgeBase, docMapper);

        BizException exception = assertThrows(BizException.class, () -> controller.importDoc(request()));

        assertEquals(CommonErrorCode.FORBIDDEN, exception.getErrorCode());
        verify(knowledgeBase, never()).importDoc(request().docName(), request().content());
        verify(docMapper, never()).insert(org.mockito.ArgumentMatchers.any(KnowledgeDoc.class));
    }

    @Test
    void importDocAllowsAdminRole() {
        setRoles("DOCTOR,ADMIN");
        when(knowledgeBase.importDoc("指南", "内容")).thenReturn(2);
        KnowledgeController controller = new KnowledgeController(knowledgeBase, docMapper);

        var result = controller.importDoc(request());

        assertEquals(0, result.code());
        assertEquals(2, result.data());
        verify(knowledgeBase).importDoc("指南", "内容");
        verify(docMapper).insert(org.mockito.ArgumentMatchers.any(KnowledgeDoc.class));
    }

    private static KnowledgeController.ImportRequest request() {
        return new KnowledgeController.ImportRequest("指南", "v1", "内容");
    }

    private static void setRoles(String roles) {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader(YiliaoConstants.HEADER_USER_ROLES, roles);
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));
    }
}
