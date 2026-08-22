package com.yiliao.ai.controller;

import com.yiliao.ai.entity.KnowledgeDoc;
import com.yiliao.ai.mapper.KnowledgeDocMapper;
import com.yiliao.ai.rag.KnowledgeBaseService;
import com.yiliao.common.core.result.Result;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.constraints.NotBlank;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 知识库管理（specs/modules/ai.md §3）：指南导入（管理员）。
 */
@RestController
@RequestMapping("/api/ai/knowledge")
public class KnowledgeController {

    private final KnowledgeBaseService knowledgeBase;
    private final KnowledgeDocMapper docMapper;

    public KnowledgeController(KnowledgeBaseService knowledgeBase, KnowledgeDocMapper docMapper) {
        this.knowledgeBase = knowledgeBase;
        this.docMapper = docMapper;
    }

    @Operation(summary = "指南文档切片入库（管理员）")
    @PostMapping("/import")
    public Result<Integer> importDoc(@jakarta.validation.Valid @RequestBody ImportRequest request) {
        int chunks = knowledgeBase.importDoc(request.docName(), request.content());
        KnowledgeDoc doc = new KnowledgeDoc();
        doc.setDocName(request.docName());
        doc.setVersion(request.version());
        doc.setChunkCount(chunks);
        doc.setStatus(1);
        docMapper.insert(doc);
        return Result.ok(chunks);
    }

    public record ImportRequest(@NotBlank String docName, String version, @NotBlank String content) {
    }
}
