package com.example.aidocumentmanager.ai;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Counters behind the Settings panel's statistics.
 *
 * <p>
 * The Settings panel casts these values to Integer, Double, and Long, so the
 * boxed type of each key is part of the contract, not an implementation detail.
 */
class ServiceStatistics {

    private final Map<String, Object> values = new ConcurrentHashMap<>();

    ServiceStatistics() {
        values.put("totalQueries", 0);
        values.put("totalDocuments", 0);
        values.put("totalChunks", 0);
        values.put("averageResponseTime", 0.0);
        values.put("lastQueryTime", 0L);
    }

    /** Adopt the counts of an index reloaded from disk. */
    void restored(int documents, int chunks) {
        values.put("totalDocuments", documents);
        values.put("totalChunks", chunks);
    }

    void documentAdded(int chunkCount) {
        values.put("totalDocuments", (Integer) values.get("totalDocuments") + 1);
        values.put("totalChunks", (Integer) values.get("totalChunks") + chunkCount);
    }

    /**
     * Mirrors documentAdded, which counts the document's own chunk count - that
     * is a paragraph estimate rather than an embedding count when embedding was
     * unavailable, and the two must cancel out.
     */
    void documentRemoved(int chunkCount) {
        values.put("totalDocuments", Math.max(0, (Integer) values.get("totalDocuments") - 1));
        values.put("totalChunks", Math.max(0, (Integer) values.get("totalChunks") - chunkCount));
    }

    /**
     * Fold one completed query into the running average.
     *
     * @return how long that query took, in milliseconds
     */
    long queryCompleted(long startTime) {
        int totalQueries = (Integer) values.get("totalQueries") + 1;
        long responseTime = System.currentTimeMillis() - startTime;
        double avgResponseTime = (Double) values.get("averageResponseTime");
        avgResponseTime = (avgResponseTime * (totalQueries - 1) + responseTime) / totalQueries;

        values.put("totalQueries", totalQueries);
        values.put("averageResponseTime", avgResponseTime);
        values.put("lastQueryTime", responseTime);

        return responseTime;
    }

    Map<String, Object> snapshot() {
        return new HashMap<>(values);
    }
}
