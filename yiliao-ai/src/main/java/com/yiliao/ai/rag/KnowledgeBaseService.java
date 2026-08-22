package com.yiliao.ai.rag;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 知识库服务（specs/modules/ai.md §4.4）：
 * 登记文档（knowledge_doc 落库）→ 切分 → 伪向量入库 → 混合检索。
 * P4b：切片迁 PgVector + 真实 EmbeddingModel（接口不变，仅换 embed 实现）。
 */
@Service
public class KnowledgeBaseService {

    private final TextChunker chunker;
    private final HybridRetriever retriever = new HybridRetriever();
    private final int topK;

    public KnowledgeBaseService(@Value("${yiliao.ai.rag.top-k:3}") int topK,
                                @Value("${yiliao.ai.rag.chunk-size:600}") int chunkSize,
                                @Value("${yiliao.ai.rag.chunk-overlap:100}") int overlap) {
        this.topK = topK;
        this.chunker = new TextChunker(chunkSize, overlap);
    }

    /** 导入文档：切分 + 向量化入库。返回切片数。 */
    public int importDoc(String docName, String content) {
        List<TextChunker.Chunk> chunks = chunker.chunk(docName, content);
        for (TextChunker.Chunk chunk : chunks) {
            retriever.add(new HybridRetriever.DocChunk(
                    chunk.docName(), chunk.section(), chunk.text(),
                    DeterministicEmbedding.embed(chunk.text())));
        }
        return chunks.size();
    }

    /** 检索 TopK（RRF 融合；Rerank 钩子在 P4b 激活）。 */
    public List<HybridRetriever.DocChunk> retrieve(String query) {
        return retriever.reRank(query, retriever.retrieve(
                query, DeterministicEmbedding.embed(query), topK));
    }

    public boolean isEmpty() {
        return retriever.size() == 0;
    }

    /**
     * 确定性伪向量：bigram 词频哈希到固定维度——离线环境 RAG 可完整演示；
     * 语义能力有限（退化为词频相似），P4b 换 BGE Embedding 后保留同接口。
     */
    static final class DeterministicEmbedding {

        static final int DIM = 128;

        static double[] embed(String text) {
            double[] vector = new double[DIM];
            Map<String, Integer> grams = HybridRetriever.bigrams(text);
            for (Map.Entry<String, Integer> entry : grams.entrySet()) {
                vector[Math.floorMod(entry.getKey().hashCode(), DIM)] += entry.getValue();
            }
            // L2 归一化
            double norm = 0;
            for (double v : vector) {
                norm += v * v;
            }
            norm = Math.sqrt(norm);
            if (norm > 0) {
                for (int i = 0; i < DIM; i++) {
                    vector[i] /= norm;
                }
            }
            return vector;
        }
    }

    /** 供测试与内部使用。 */
    Map<String, Integer> bigramsOf(String text) {
        return new HashMap<>(HybridRetriever.bigrams(text));
    }
}
