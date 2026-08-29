# AI Document Manager

**AI Document Manager** is an intelligent knowledge management desktop application built with **JavaFX 23**, **LangChain4j**, and **OpenAI / Hugging Face** models. It lets you chat with, search, and retrieve contextual answers from your own document collections using **Retrieval-Augmented Generation (RAG)**.

Documents are embedded **locally** with a bundled quantized ONNX model, so indexing and similarity search need no network and no API key. Only the chat step calls a hosted LLM.

---

## Features

### Core Functionality

- **AI Chat Interface** - Conversational AI over your documents via LangChain4j, using OpenAI or Hugging Face models
- **Document Management** - Upload and process TXT, PDF, DOCX, MD, Java, XML, JSON, YAML, and .properties files
- **Local Embeddings** - Text is embedded on your machine with `all-MiniLM-L6-v2` (quantized ONNX); no data leaves the machine during indexing
- **Intelligent Search** - Full-text search across the knowledge base with filtering and result export
- **Settings Panel** - Provider and API key configuration, RAG tuning (chunk size, overlap, retrieval count, similarity threshold), and preferences
- **Statistics & Monitoring** - Knowledge base statistics, real-time analytics, and system information
- **Theming** - Light, Dark, and System Default themes
- **Error Handling** - User-friendly error messages with specific guidance for auth, model, and quota failures
- **Persistent Configuration** - Settings and window state saved between sessions

### Demo Mode

Without an API key the application still starts and remains fully navigable. Chat falls back to a built-in mock assistant that explains how to configure a key, while document upload, indexing, and search continue to work normally.

---

## Project Architecture

The Maven project sits at the repository root, so `mvn` commands run from there.

Packages are named after what they own, and dependencies point one way: `ui`
depends on `ai`, `document`, `storage`, `config`, `domain`, and `common`, and
nothing below `ui` imports JavaFX. That is what makes the search rules, the CSV
export, and the settings normalization testable without a scene graph. There is
one known exception, noted in the architecture doc.

| Package | Owns |
| --- | --- |
| `config` | Settings persistence and migrations |
| `domain` | `DocumentEntry`, `ChatMessage`, `KnowledgeBase` |
| `document` | Text extraction from TXT, PDF, and DOCX; chunking; upload validation |
| `ai` | Retrieval-augmented chat: models, embeddings, prompts, statistics |
| `storage` | The on-disk index that survives a restart |
| `ui` | The shell and one package per tab: chat, documents, search, settings |
| `common` | Logging, validation, byte formatting |

FXML and the stylesheet live under the package that loads them, so each view is
found by name relative to its controller.

See [docs/architecture.md](docs/architecture.md) for the full package layout,
the resource layout, and where the tests sit.

---

## Tech Stack

### Core Technologies

- **Language:** Java 21 LTS (`maven-compiler-plugin` targets release 21)
- **Frontend:** JavaFX 23.0.1 with FXML
- **AI Framework:** LangChain4j 0.35.0
- **LLM Providers:** OpenAI (default `gpt-4o-mini`) and Hugging Face (default `meta-llama/Llama-3.1-8B-Instruct`), both reached through an OpenAI-compatible client
- **Embeddings:** `langchain4j-embeddings-all-minilm-l6-v2-q`, running locally
- **Vector Storage:** `InMemoryEmbeddingStore`, rebuilt on each launch
- **Build Tool:** Apache Maven

### Key Dependencies

- **Document Processing:** Apache PDFBox 3.0.3 (PDF), Apache POI 5.2.5 (DOCX)
- **JSON Processing:** Jackson Databind 2.17.2
- **Utilities:** Apache Commons IO 2.16.1
- **Logging:** Log4j2 2.22.1
- **Testing:** JUnit 5 (Jupiter 5.10.2)

---

## Setup & Installation

### Prerequisites

- **Java 21 LTS** or higher (required)
- **Apache Maven 3.6+** (required)
- **Internet connection** - needed to download dependencies, and for chat once configured. Embedding and search work offline
- **API key** (optional) - an OpenAI and/or Hugging Face key enables real chat responses. Without one the app runs in demo mode

### Quick Start

1. **Clone the project**

   ```bash
   git clone https://github.com/ecxof/AI-Document-Manager.git
   cd AI-Document-Manager
   ```

2. **Verify the Java version**

   ```bash
   java -version
   # Should show Java 21 or higher
   ```

3. **Build and test**

   ```bash
   mvn clean test
   ```

4. **Run AI Document Manager**

   ```bash
   mvn javafx:run
   ```

   Alternatively, run it from your IDE using the `com.example.aidocumentmanager.Launcher`
   class. `Launcher` is a thin wrapper whose `main()` calls `MainApp.main()`; it exists
   so the app can be started from an IDE without adding JavaFX module-path VM arguments.

5. **Initial configuration**

   - Open the **Settings** tab
   - Choose your provider (**OpenAI** or **Hugging Face**)
   - Enter your API key and select a model
   - Adjust RAG parameters if desired, then save

### Alternative Run Methods

```bash
# Run without clean
mvn javafx:run

# Run with full rebuild
mvn clean javafx:run

# Build and run separately
mvn compile && mvn javafx:run
```

---

## Configuration & Data Location

On first launch the application creates `~/.aidocumentmanager/`, holding
`config.properties`, copies of uploaded documents, logs, and the persisted
index.

**API keys are stored in plain text** in `config.properties`. Treat that file as
a secret, and do not commit it. The repository's `.gitignore` already excludes
`config.properties` and `.aidocumentmanager/`.

See [docs/configuration.md](docs/configuration.md) for every setting, its
default, and what the similarity threshold actually compares against.

---

## Application Usage

### Getting Started

1. **Settings** - Choose provider, enter the API key, pick a model, and tune RAG parameters (chunk size, overlap, retrieval count, similarity threshold)
2. **Documents** - Upload documents; view processing status and knowledge base statistics
3. **Chat** - Ask questions about your documents and get answers grounded in retrieved excerpts
4. **Search** - Search across documents with filters, and export results to a file

### How Retrieval Works

1. An uploaded document is split with a recursive splitter using the configured chunk size and overlap
2. Each chunk is embedded locally and stored in the in-memory vector store
3. A question is embedded, then matched against the store using the configured maximum results and minimum similarity score
4. Matching excerpts are prepended to the prompt with an instruction to answer only from that context, and to say so when the answer is not present
5. If nothing clears the threshold, the question is sent without context rather than failing

---

## Architecture Highlights

### Design Patterns

- **MVC** - Clean separation between FXML views, controllers, and the domain layer
- **Singleton** - `AIService`, `ConfigurationManager`, `DialogService`, `LoggerUtil`, `ThemeManager`
- **Observer** - JavaFX property listeners drive window-state persistence and theming
- **Strategy** - Per-extension text extraction in `DocumentTextExtractor`

### Key Technical Decisions

- **JavaFX 23** - Modern, native desktop application framework
- **LangChain4j** - Multi-provider AI integration with RAG support
- **Local embedding model** - Indexing works offline and costs nothing per document
- **In-memory vector store** - Fast retrieval, at the cost of being rebuilt each launch
- **Maven** - Reliable dependency management

---

## Known Limitations

- **The vector store is not persistent.** `InMemoryEmbeddingStore` is rebuilt on every launch, so documents must be re-indexed each session
- **Removing a document does not remove its vectors.** `InMemoryEmbeddingStore` has no delete-by-id, so a removed document's chunks can still be retrieved until restart. An external vector database would resolve this
- **API keys are stored in plain text** in `~/.aidocumentmanager/config.properties`
- **Test coverage is partial.** Unit tests cover the model and utility layers; the controllers and `AIService` are not covered

---

## Future Enhancements

### Planned Improvements

- **External Vector Databases** - Integration with FAISS, Chroma, or Qdrant for persistence and deletion
- **Theme System** - Custom themes beyond Light/Dark/System
- **Plugin Architecture** - Extensible system for custom document processors
- **Multi-language Support** - Internationalization (i18n)

### Technical Roadmap

- **Enhanced RAG Pipeline** - Query transformation and result re-ranking
- **Distributed Processing** - Support for large document collections
- **API Integration** - REST API for external systems
- **Cloud Deployment** - Docker containerization

---

## Project Status

All core coursework requirements are implemented and working:

- Java 21 LTS build
- MVC architecture across five views and controllers
- RAG pipeline with local embeddings and configurable retrieval
- Document management for nine file formats
- Search with filtering and export
- Light/Dark/System theming
- Error handling and input validation
- Persistent configuration and window state

See [Known Limitations](#known-limitations) for what is not production-ready.

---

## Development Team

**Project:** AI Document Manager - Intelligent Knowledge Management System
**Course:** Advanced Programming (ITS66704)
**Institution:** Taylor's University
**Academic Year:** 2025/2026

**Technologies used:**

- Java 21 LTS
- JavaFX desktop application development
- AI integration with LangChain4j
- Retrieval-Augmented Generation (RAG)
- Maven build system
- Software architecture and design patterns
