package com.example.aidocumentmanager.storage;

import com.example.aidocumentmanager.domain.DocumentEntry;
import com.example.aidocumentmanager.common.LoggerUtil;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.ObjectMapper;

import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.store.embedding.inmemory.InMemoryEmbeddingStore;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Persists the document index - the documents themselves, their embeddings, and
 * the mapping between them - so an uploaded document survives a restart instead
 * of having to be uploaded again.
 *
 * <p>
 * Everything lives in a single JSON file, written atomically. The three pieces
 * only make sense together: embeddings without their documents would answer
 * questions from a knowledge base the Admin panel shows as empty, and documents
 * without their embeddings would list files the assistant cannot see. Writing
 * one file means a crash mid-save can never leave that mismatch on disk.
 */
public class DocumentIndexStore {

    private static final LoggerUtil logger = LoggerUtil.getInstance();

    private static final String INDEX_FILE = "knowledge-index.json";

    /** Bump when the on-disk shape changes; older files are then discarded. */
    static final int FORMAT_VERSION = 1;

    /**
     * Vectors are only comparable to vectors from the same model, so the model
     * is stamped into the file and a mismatch discards the cache.
     */
    static final String EMBEDDING_MODEL_ID = "all-minilm-l6-v2-q";

    private final Path indexFile;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public DocumentIndexStore(Path indexDirectory) {
        this.indexFile = indexDirectory.resolve(INDEX_FILE);
    }

    Path getIndexFile() {
        return indexFile;
    }

    /**
     * What was restored from disk. A snapshot is always internally consistent:
     * either everything loaded, or it is empty.
     */
    public record Snapshot(
            List<DocumentEntry> documents,
            Map<String, List<String>> embeddingIds,
            InMemoryEmbeddingStore<TextSegment> embeddingStore) {

        public static Snapshot empty() {
            return new Snapshot(List.of(), Map.of(), new InMemoryEmbeddingStore<>());
        }

        public boolean isEmpty() {
            return documents.isEmpty();
        }
    }

    /**
     * Read the index back. Never throws: a missing, unreadable, corrupt or
     * outdated file just means starting with an empty index, which costs the
     * user a re-upload but never a broken application.
     */
    public synchronized Snapshot load(int expectedEmbeddingDimension) {
        if (!Files.exists(indexFile)) {
            logger.info("No persisted document index at " + indexFile + " - starting with an empty index.");
            return Snapshot.empty();
        }

        try {
            IndexFile file = objectMapper.readValue(Files.readString(indexFile), IndexFile.class);

            if (file.formatVersion != FORMAT_VERSION) {
                logger.warn("Discarding document index written in format version " + file.formatVersion
                        + " (this build reads version " + FORMAT_VERSION + "). Documents must be uploaded again.");
                return Snapshot.empty();
            }

            if (!EMBEDDING_MODEL_ID.equals(file.embeddingModel)
                    || file.embeddingDimension != expectedEmbeddingDimension) {
                logger.warn("Discarding document index embedded with " + file.embeddingModel + "/"
                        + file.embeddingDimension + " - this build uses " + EMBEDDING_MODEL_ID + "/"
                        + expectedEmbeddingDimension + ". Vectors from different models are not comparable.");
                return Snapshot.empty();
            }

            if (file.embeddingStore == null || file.embeddingStore.isBlank()) {
                logger.warn("Persisted index has no embeddings. Documents must be uploaded again.");
                return Snapshot.empty();
            }

            InMemoryEmbeddingStore<TextSegment> store = InMemoryEmbeddingStore.fromJson(file.embeddingStore);

            List<DocumentEntry> documents = new ArrayList<>();
            for (DocumentRecord record : file.documents) {
                documents.add(record.toDocumentEntry());
            }

            Map<String, List<String>> embeddingIds = new LinkedHashMap<>();
            file.embeddingIds.forEach((documentId, ids) -> embeddingIds.put(documentId, new ArrayList<>(ids)));

            if (documents.isEmpty()) {
                logger.info("Persisted index at " + indexFile + " holds no documents.");
            } else {
                logger.info("Restored " + documents.size() + " documents from the persisted index.");
            }
            return new Snapshot(documents, embeddingIds, store);

        } catch (Exception e) {
            logger.error("Failed to read the document index at " + indexFile
                    + " - starting with an empty index. Documents must be uploaded again.", e);
            return Snapshot.empty();
        }
    }

    /**
     * Write the index out atomically. Failures are logged rather than thrown:
     * losing persistence should not take an upload down with it, since the
     * in-memory index the user is working with is still correct.
     */
    public synchronized void save(Collection<DocumentEntry> documents, Map<String, List<String>> embeddingIds,
            InMemoryEmbeddingStore<TextSegment> embeddingStore, int embeddingDimension) {

        try {
            IndexFile file = new IndexFile();
            file.formatVersion = FORMAT_VERSION;
            file.embeddingModel = EMBEDDING_MODEL_ID;
            file.embeddingDimension = embeddingDimension;
            file.embeddingStore = embeddingStore.serializeToJson();

            for (DocumentEntry document : documents) {
                file.documents.add(DocumentRecord.from(document));
            }
            embeddingIds.forEach((documentId, ids) -> file.embeddingIds.put(documentId, new ArrayList<>(ids)));

            writeAtomically(objectMapper.writeValueAsString(file));

            logger.info("Persisted document index (" + file.documents.size() + " documents) to " + indexFile);

        } catch (Exception e) {
            logger.error("Failed to persist the document index to " + indexFile
                    + ". The current session is unaffected, but documents may need re-uploading after a restart.", e);
        }
    }

    /** Remove the persisted index entirely. */
    public synchronized void clear() {
        try {
            Files.deleteIfExists(indexFile);
        } catch (IOException e) {
            logger.error("Failed to delete the document index at " + indexFile, e);
        }
    }

    /**
     * Write through a temporary file so an interrupted save leaves the previous
     * index intact rather than a half-written one.
     */
    private void writeAtomically(String json) throws IOException {
        Path directory = indexFile.getParent();
        if (directory != null) {
            Files.createDirectories(directory);
        }

        Path temporary = indexFile.resolveSibling(INDEX_FILE + ".tmp");
        Files.writeString(temporary, json);

        try {
            Files.move(temporary, indexFile, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        } catch (Exception atomicMoveUnsupported) {
            // Some Windows filesystems reject ATOMIC_MOVE; a plain replace is
            // still far better than writing over the live file in place.
            Files.move(temporary, indexFile, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    // =========================================================================
    // On-disk shape
    // =========================================================================

    @JsonIgnoreProperties(ignoreUnknown = true)
    static class IndexFile {
        public int formatVersion;
        public String embeddingModel;
        public int embeddingDimension;
        public List<DocumentRecord> documents = new ArrayList<>();
        public Map<String, List<String>> embeddingIds = new LinkedHashMap<>();
        /** The embedding store JSON, nested so the whole index is one file. */
        public String embeddingStore;
    }

    /**
     * A DocumentEntry flattened for storage. Timestamps are written as ISO-8601
     * strings and the type as its enum name, so the file stays readable and
     * stable without pulling in extra Jackson modules.
     */
    @JsonIgnoreProperties(ignoreUnknown = true)
    static class DocumentRecord {
        public String id;
        public String fileName;
        public String filePath;
        public String title;
        public String content;
        public String type;
        public long fileSize;
        public String uploadedAt;
        public String lastModified;
        public String hash;
        public boolean indexed;
        public List<String> tags;
        public String category;
        public int chunkCount;

        static DocumentRecord from(DocumentEntry document) {
            DocumentRecord record = new DocumentRecord();
            record.id = document.getId();
            record.fileName = document.getFileName();
            record.filePath = document.getFilePath();
            record.title = document.getTitle();
            record.content = document.getContent();
            record.type = document.getType() != null ? document.getType().name() : null;
            record.fileSize = document.getFileSize();
            record.uploadedAt = document.getUploadedAt() != null ? document.getUploadedAt().toString() : null;
            record.lastModified = document.getLastModified() != null ? document.getLastModified().toString() : null;
            record.hash = document.getHash();
            record.indexed = document.isIndexed();
            record.tags = document.getTags() != null ? new ArrayList<>(document.getTags()) : null;
            record.category = document.getCategory();
            record.chunkCount = document.getChunkCount();
            return record;
        }

        DocumentEntry toDocumentEntry() {
            DocumentEntry document = new DocumentEntry();
            document.setId(id);
            // setFileName also derives the type, so set it first and let an
            // explicitly stored type win afterwards.
            document.setFileName(fileName);
            document.setFilePath(filePath);
            document.setTitle(title);
            document.setContent(content);
            document.setType(parseType(type, document.getType()));
            document.setFileSize(fileSize);
            document.setUploadedAt(parseTimestamp(uploadedAt));
            document.setLastModified(parseTimestamp(lastModified));
            document.setHash(hash);
            document.setIndexed(indexed);
            document.setTags(tags);
            document.setCategory(category);
            document.setChunkCount(chunkCount);
            return document;
        }

        private static DocumentEntry.DocumentType parseType(String name, DocumentEntry.DocumentType fallback) {
            if (name == null) {
                return fallback;
            }
            try {
                return DocumentEntry.DocumentType.valueOf(name);
            } catch (IllegalArgumentException unknownType) {
                return fallback;
            }
        }

        private static LocalDateTime parseTimestamp(String value) {
            if (value == null) {
                return LocalDateTime.now();
            }
            try {
                return LocalDateTime.parse(value);
            } catch (DateTimeParseException unparseable) {
                return LocalDateTime.now();
            }
        }
    }
}
