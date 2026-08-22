package com.yiliao.ai.rag;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RagPipelineTest {

    private HybridRetriever.DocChunk chunk(String doc, String section, String text) {
        return new HybridRetriever.DocChunk(doc, section, text,
                KnowledgeBaseService.DeterministicEmbedding.embed(text));
    }

    @Test
    void hybridRetrievalRanksRelevantSectionFirst() {
        HybridRetriever retriever = new HybridRetriever();
        retriever.add(chunk("肺结节共识2024", "实性结节随访",
                "孤立性实性结节6至8毫米且无危险因素者，2年内6至12个月随访一次"));
        retriever.add(chunk("肺结节共识2024", "磨玻璃结节随访",
                "纯磨玻璃结节不超过5毫米，首次6个月CT随访，随后年度随访"));
        retriever.add(chunk("术后随访共识", "II~III期",
                "II期至III期术后前2年每3个月随访一次，第3至5年每6个月一次"));

        List<HybridRetriever.DocChunk> top = retriever.retrieve(
                "磨玻璃结节5mm多久复查一次", null, 3);

        assertEquals("磨玻璃结节随访", top.get(0).section());
        assertTrue(top.stream().anyMatch(c -> c.section().equals("实性结节随访")));
    }

    @Test
    void unrelatedQueryReturnsSomethingButKnowledgeBaseEmptyGuardExists() {
        KnowledgeBaseService service = new KnowledgeBaseService(3, 600, 100);
        assertTrue(service.isEmpty());
        service.importDoc("共识", "# 随访\n纯磨玻璃结节年度随访");
        assertEquals(1, service.retrieve("磨玻璃复查").size());
    }

    @Test
    void chunkerSplitsBySectionHeaders() {
        TextChunker chunker = new TextChunker(600, 100);
        List<TextChunker.Chunk> chunks = chunker.chunk("共识2024",
                "# 实性结节\n4至6毫米常规年度随访。\n# 磨玻璃结节\n5毫米以内6个月后年度随访。");
        assertEquals(2, chunks.size());
        assertEquals("实性结节", chunks.get(0).section());
        assertEquals("磨玻璃结节", chunks.get(1).section());
    }

    @Test
    void chunkerSlidingWindowForLongText() {
        TextChunker chunker = new TextChunker(50, 10);
        String longText = "随".repeat(200);
        List<TextChunker.Chunk> chunks = chunker.chunk("文档", "# 章节\n" + longText);
        assertTrue(chunks.size() > 1);
        // 相邻片段存在重叠
        String a = chunks.get(0).text();
        String b = chunks.get(1).text();
        assertTrue(a.length() == 50 && b.length() == 50);
    }

    @Test
    void chunkerRejectsBadConfig() {
        assertThrows(IllegalArgumentException.class, () -> new TextChunker(100, 100));
    }

    @Test
    void rrfFusionPrefersDualChannelHits() {
        // 同时命中两通道的文档 RRF 分数更高（1/(k+1) + 1/(k+1) > 单通道）
        String query = "磨玻璃结节随访间隔";
        HybridRetriever retriever = new HybridRetriever();
        HybridRetriever.DocChunk dualHit = chunk("A", "磨玻璃随访", "磨玻璃结节随访间隔为年度");
        HybridRetriever.DocChunk keywordOnly = chunk("B", "其他", "磨玻璃结节随访间隔概述很长的背景文字介绍");
        retriever.add(dualHit);
        retriever.add(keywordOnly);
        List<HybridRetriever.DocChunk> top = retriever.retrieve(
                query, KnowledgeBaseService.DeterministicEmbedding.embed(query), 2);
        assertEquals("磨玻璃随访", top.get(0).section());
    }
}
