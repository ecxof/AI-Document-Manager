package com.example.aidocumentmanager.ai;

import com.example.aidocumentmanager.common.LoggerUtil;
import com.example.aidocumentmanager.config.ConfigurationManager;
import com.example.aidocumentmanager.domain.DocumentEntry;
import com.example.aidocumentmanager.storage.DocumentIndexStore;

import dev.langchain4j.data.document.Document;
import dev.langchain4j.data.document.DocumentSplitter;
import dev.langchain4j.data.document.splitter.DocumentSplitters;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.embedding.onnx.allminilml6v2q.AllMiniLmL6V2QuantizedEmbeddingModel;
import dev.langchain4j.store.embedding.EmbeddingMatch;
import dev.langchain4j.store.embedding.EmbeddingSearchRequest;
import dev.langchain4j.store.embedding.EmbeddingSearchResult;
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.store.embedding.inmemory.InMemoryEmbeddingStore;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * The vector side of the RAG pipeline: the local embedding model, the store its
 * vectors live in, and the on-disk index that survives a restart.
 *
 * <p>
 * Every method here tolerates the model having failed to load. When it does,
 * the application still runs, just without retrieval.
 */
public class EmbeddingIndex {

    private static final LoggerUtil logger = LoggerUtil.getInstance();

    private EmbeddingModel embeddingModel;
    // Concrete type: persisting the index needs the store to serialize itself.
    private InMemoryEmbeddingStore<TextSegment> embeddingStore;
    private DocumentSplitter documentSplitter;
    private DocumentIndexStore indexStore;
    private int embeddingDimension;

    // Embedding store ids per document, so a deleted document's chunks can be
    // dropped from the vector store too - the store deletes by embedding id and
    // knows nothing about our documents.
    private final Map<String, List<String>> documentEmbeddingIds = new ConcurrentHashMap<>();

    EmbeddingIndex() {
        try {
            logger.info("Initializing local embedding model (All-MiniLM-L6-v2)...");
            this.embeddingModel = new AllMiniLmL6V2QuantizedEmbeddingModel();
            this.embeddingStore = new InMemoryEmbeddingStore<>();
            this.documentSplitter = createDocumentSplitter();
            this.indexStore = new DocumentIndexStore(ConfigurationManager.getInstance().getIndexPath());
            // Probed once here rather than per save: it is a property of the
            // model, and stamping it into the index keeps vectors from a
            // different model from ever being searched against.
            this.embeddingDimension = embeddingModel.embed("dimension probe").content().dimension();
            logger.info("Embedding model initialized successfully.");
        } catch (Exception e) {
            logger.error("Failed to initialize embedding model, so RAG will be disabled.", e);
            this.embeddingModel = null;
            this.embeddingStore = null;
            this.indexStore = null;
        }
    }

    /** Whether the embedding model loaded, which is what enables retrieval. */
    boolean isModelAvailable() {
        return embeddingModel != null;
    }

    /** Whether both the model and a store are present, so chunks can be added. */
    boolean isUsable() {
        return embeddingModel != null && embeddingStore != null;
    }

    /**
     * Reload the documents and embeddings written by the previous session, so an
     * uploaded document does not have to be uploaded again after a restart.
     *
     * @return what was on disk, or an empty snapshot when there was nothing to
     *         restore or the model is unavailable
     */
    DocumentIndexStore.Snapshot restore() {
        if (indexStore == null || embeddingModel == null) {
            return DocumentIndexStore.Snapshot.empty();
        }

        DocumentIndexStore.Snapshot snapshot = indexStore.load(embeddingDimension);
        if (snapshot.isEmpty()) {
            return snapshot;
        }

        this.embeddingStore = snapshot.embeddingStore();
        documentEmbeddingIds.putAll(snapshot.embeddingIds());

        return snapshot;
    }

    /** Write the current documents and embeddings to disk. */
    void persist(Collection<DocumentEntry> documents) {
        if (indexStore == null || embeddingStore == null || embeddingModel == null) {
            return;
        }

        indexStore.save(documents, documentEmbeddingIds, embeddingStore, embeddingDimension);
    }

    /**
     * Build a document splitter using the chunk size / overlap configured in the
     * Settings panel, so the RAG sliders actually take effect. Falls back to sane
     * defaults if the configured values are out of range.
     */
    private static DocumentSplitter createDocumentSplitter() {
        ConfigurationManager config = ConfigurationManager.getInstance();
        int chunkSize = config.getChunkSize();
        int overlap = config.getChunkOverlap();

        // Guard against invalid combinations that would break the splitter.
        if (chunkSize < 100) {
            chunkSize = 500;
        }
        if (overlap < 0 || overlap >= chunkSize) {
            overlap = Math.min(50, chunkSize / 10);
        }

        logger.info("Configuring document splitter: chunkSize=" + chunkSize + ", overlap=" + overlap);
        return DocumentSplitters.recursive(chunkSize, overlap);
    }

    /**
     * Pick up any change to the configured chunk size / overlap. Documents that
     * are already embedded keep their original chunking.
     */
    void refreshSplitter() {
        if (embeddingModel != null) {
            this.documentSplitter = createDocumentSplitter();
        }
    }

    /**
     * Split document content into chunks and store embeddings in the vector store.
     *
     * @return number of chunks created
     */
    int embedDocument(DocumentEntry docEntry) {
        try {
            Document langchainDoc = Document.from(docEntry.getContent());
            List<TextSegment> segments = documentSplitter.split(langchainDoc);

            if (segments.isEmpty()) {
                logger.warn("No segments produced for document: " + docEntry.getFileName());
                return 0;
            }

            logger.info("Embedding " + segments.size() + " chunks for: " + docEntry.getFileName());

            List<Embedding> embeddings = embeddingModel.embedAll(segments).content();
            List<String> embeddingIds = embeddingStore.addAll(embeddings, segments);
            documentEmbeddingIds.put(docEntry.getId(), new ArrayList<>(embeddingIds));

            logger.info("Successfully embedded " + segments.size() + " chunks for: " + docEntry.getFileName());
            return segments.size();

        } catch (Exception e) {
            logger.error("Failed to embed document: " + docEntry.getFileName(), e);
            return 1; // Fallback count
        }
    }

    /**
     * Drop this document's chunks from the vector store.
     *
     * @return number of chunks removed
     */
    int removeDocument(DocumentEntry document) {
        return removeEmbeddings(embeddingStore, documentEmbeddingIds, document);
    }

    /**
     * Embed the question and return the chunks closest to it.
     *
     * @return the matching chunks, or an empty list when retrieval is unavailable
     */
    List<EmbeddingMatch<TextSegment>> retrieve(String question, int maxResults, double minScore) {
        if (!isUsable()) {
            return List.of();
        }

        Embedding queryEmbedding = embeddingModel.embed(question).content();
        return retrieveWithFallback(embeddingStore, queryEmbedding, maxResults, minScore);
    }

    /**
     * Search the store for the chunks closest to the query, retrying without the
     * similarity threshold when it filters out everything.
     *
     * <p>
     * Note on {@code minScore}: langchain4j does not compare against raw cosine
     * similarity. It rescales with {@code (cosineSimilarity + 1) / 2}, so 0.5
     * means "cosine 0.0" and 0.7 means "cosine 0.4". On-topic questions score
     * around 0.70-0.83 against real prose, so a 0.7 threshold sits right on the
     * cliff: rephrase the question and every chunk of a correctly indexed
     * document drops out, leaving the model to answer with no context at all.
     *
     * <p>
     * Falling back to the closest chunks is the safer failure mode. The prompt
     * tells the model to say when the answer is not in the context, so an
     * off-topic excerpt costs little, while dropping the context guarantees an
     * "I don't have that information" answer about a document we did index.
     */
    public static List<EmbeddingMatch<TextSegment>> retrieveWithFallback(EmbeddingStore<TextSegment> store,
            Embedding queryEmbedding, int maxResults, double minScore) {

        List<EmbeddingMatch<TextSegment>> matches = search(store, queryEmbedding, maxResults, minScore);

        if (matches.isEmpty() && minScore > 0.0) {
            logger.info("No chunks scored at or above the similarity threshold (" + minScore
                    + "). Falling back to the closest " + maxResults + " chunks.");
            matches = search(store, queryEmbedding, maxResults, 0.0);
        }

        return matches;
    }

    private static List<EmbeddingMatch<TextSegment>> search(EmbeddingStore<TextSegment> store,
            Embedding queryEmbedding, int maxResults, double minScore) {

        EmbeddingSearchRequest searchRequest = EmbeddingSearchRequest.builder()
                .queryEmbedding(queryEmbedding)
                .maxResults(maxResults)
                .minScore(minScore)
                .build();

        EmbeddingSearchResult<TextSegment> searchResult = store.search(searchRequest);
        return searchResult.matches();
    }

    /**
     * Drop a document's chunks from the vector store. Without this the chunks
     * stay searchable after the document is deleted, so the assistant keeps
     * answering from a document the user removed.
     *
     * @return number of chunks removed
     */
    public static int removeEmbeddings(EmbeddingStore<TextSegment> store,
            Map<String, List<String>> embeddingIdsByDocument, DocumentEntry document) {

        List<String> embeddingIds = embeddingIdsByDocument.remove(document.getId());

        if (embeddingIds == null || embeddingIds.isEmpty()) {
            // Never embedded - the model failed to load, or embedding threw and
            // the document was indexed by paragraph count only.
            return 0;
        }

        if (store == null) {
            return 0;
        }

        try {
            store.removeAll(embeddingIds);
            return embeddingIds.size();
        } catch (Exception e) {
            logger.error("Failed to remove embeddings for: " + document.getFileName(), e);
            // Put them back so a later delete, or a store swap, can retry.
            embeddingIdsByDocument.put(document.getId(), embeddingIds);
            return 0;
        }
    }
}
