package com.yiliao.ai.rag;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 混合检索 + RRF 融合（specs/modules/ai.md §4.4）：
 * 关键词通道（中文 bigram 重叠打分）+ 向量通道（余弦相似度）→ RRF(k=60) 融合取 TopK。
 * 纯 Java 可测；Embedding 默认确定性 bigram 哈希伪向量（离线可演示），P4b 换真模型。
 * Rerank 精排为 P4b 扩展点：reRank() 预留钩子。
 */
public class HybridRetriever {

    public static final int RRF_K = 60;

    /** 检索单元：文本 + 元数据 + 可选向量。 */
    public record DocChunk(String docName, String section, String text, double[] embedding) {
    }

    private final Map<String, DocChunk> store = new LinkedHashMap<>();

    public void add(DocChunk chunk) {
        store.put(chunk.docName() + "#" + chunk.section() + "#" + store.size(), chunk);
    }

    public int size() {
        return store.size();
    }

    public List<DocChunk> all() {
        return new ArrayList<>(store.values());
    }

    public List<DocChunk> retrieve(String query, double[] queryEmbedding, int topK) {
        // 通道一：关键词 bigram 打分
        List<Map.Entry<String, Double>> keyword = new ArrayList<>();
        for (Map.Entry<String, DocChunk> e : store.entrySet()) {
            keyword.add(Map.entry(e.getKey(), keywordScore(query, e.getValue().text())));
        }
        keyword.sort((a, b) -> Double.compare(b.getValue(), a.getValue()));

        // 通道二：向量余弦
        List<Map.Entry<String, Double>> vector = new ArrayList<>();
        if (queryEmbedding != null) {
            for (Map.Entry<String, DocChunk> e : store.entrySet()) {
                vector.add(Map.entry(e.getKey(),
                        cosine(queryEmbedding, e.getValue().embedding())));
            }
            vector.sort((a, b) -> Double.compare(b.getValue(), a.getValue()));
        }

        // RRF 融合
        Map<String, Double> fused = new HashMap<>();
        for (int rank = 0; rank < keyword.size(); rank++) {
            fused.merge(keyword.get(rank).getKey(), 1.0 / (RRF_K + rank + 1), Double::sum);
        }
        for (int rank = 0; rank < vector.size(); rank++) {
            fused.merge(vector.get(rank).getKey(), 1.0 / (RRF_K + rank + 1), Double::sum);
        }
        return fused.entrySet().stream()
                .sorted((a, b) -> Double.compare(b.getValue(), a.getValue()))
                .limit(topK)
                .map(e -> store.get(e.getKey()))
                .toList();
    }

    /** Rerank 钩子（P4b 接 bge-reranker；不可用时保持 RRF 序）。 */
    public List<DocChunk> reRank(String query, List<DocChunk> candidates) {
        return candidates; // 默认透传
    }

    /** 中文 bigram 集合。 */
    static Map<String, Integer> bigrams(String text) {
        Map<String, Integer> counts = new HashMap<>();
        String normalized = text.replaceAll("\\s+", "");
        for (int i = 0; i + 2 <= normalized.length(); i++) {
            counts.merge(normalized.substring(i, i + 2), 1, Integer::sum);
        }
        return counts;
    }

    static double keywordScore(String query, String text) {
        if (query == null || query.isBlank()) {
            return 0;
        }
        Map<String, Integer> queryGrams = bigrams(query);
        Map<String, Integer> textGrams = bigrams(text);
        double overlap = 0;
        for (Map.Entry<String, Integer> entry : queryGrams.entrySet()) {
            overlap += Math.min(entry.getValue(), textGrams.getOrDefault(entry.getKey(), 0));
        }
        return overlap / queryGrams.size();
    }

    static double cosine(double[] a, double[] b) {
        if (a == null || b == null || a.length != b.length) {
            return 0;
        }
        double dot = 0;
        double normA = 0;
        double normB = 0;
        for (int i = 0; i < a.length; i++) {
            dot += a[i] * b[i];
            normA += a[i] * a[i];
            normB += b[i] * b[i];
        }
        if (normA == 0 || normB == 0) {
            return 0;
        }
        return dot / (Math.sqrt(normA) * Math.sqrt(normB));
    }
}
