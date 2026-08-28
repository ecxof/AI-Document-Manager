# Architecture

A single-module Maven project. The Maven project sits at the repository root, so
`mvn` commands run from there.

## Layering

Packages are named after what they own, and dependencies point one way:

```
ui  ->  ai, document, storage, config, domain, common
ai  ->  document, storage, config, domain, common
```

Two rules keep that honest:

- **Nothing below `ui` imports JavaFX.** Everything outside `ui` can be exercised
  without a scene graph, which is why the search rules, the CSV writer, and the
  settings normalization have tests at all.
- **`domain` and `common` depend on nothing of ours.** They are the leaves.

These are not conventions, they are checked: `PackageLayeringTest` walks the
sources and fails the build if anything outside `ui` imports JavaFX, if anything
below `ui` imports `ui`, or if `domain` or `common` grow a dependency on another
package of ours.

`ConfigurationManager` used to break the second rule, reaching up into
`DialogService` to report a failed config read. It now exposes a
`FailureListener` that `MainApp` registers before the first `getInstance()`,
so the dialog still appears at the same moment while the config layer no longer
knows dialogs exist. Failures are logged either way; the listener only decides
whether the user is also told, and nothing is registered by default so tests and
headless runs never try to raise a dialog.

## Packages

```
com.example.aidocumentmanager
├── Launcher              IDE-friendly wrapper around MainApp
├── MainApp               JavaFX Application entry point
│
├── config
│   └── ConfigurationManager    settings persistence and migrations
│
├── domain                      the things the application is about
│   ├── DocumentEntry           a document and its metadata
│   ├── ChatMessage             one turn of a conversation
│   └── KnowledgeBase           the document collection and its statistics
│
├── document                    getting text out of files
│   ├── SupportedFileTypes      which extensions are accepted
│   ├── DocumentTextExtractor   TXT, PDF (PDFBox), and DOCX (POI) extraction
│   ├── TextChunker             overlapping chunk splitting
│   ├── DocumentFiles           hashing, DocumentEntry creation, data directory
│   └── FileValidator           upload preconditions and detailed feedback
│
├── ai                          retrieval-augmented chat
│   ├── AIService               the facade the UI talks to (singleton)
│   ├── Assistant               the chat backend
│   ├── MockAssistant           demo-mode stand-in when no API key is set
│   ├── ChatModelFactory        provider selection and model construction
│   ├── EmbeddingIndex          embedding model, vector store, persistence
│   ├── RagPromptBuilder        excerpt and prompt assembly
│   ├── ChatHistory             the conversation
│   └── ServiceStatistics       counters behind the Settings panel
│
├── storage
│   └── DocumentIndexStore      the on-disk index that survives a restart
│
├── ui
│   ├── MainController          the shell: tabs, menus, view cache
│   ├── DialogService           error and information dialogs
│   ├── ThemeManager            light/dark/system theming
│   ├── WindowStateBinder       window size and maximized state
│   ├── chat/                   ChatController
│   ├── documents/              DocumentsController
│   ├── search/                 SearchController, DocumentSearch,
│   │                           DocumentFilter, SearchResultsCsvWriter
│   └── settings/               SettingsController, SettingsForm,
│                               SettingsExporter, SystemInfo
│
└── common
    ├── LoggerUtil              logging facade
    ├── ValidationUtil          input validation
    └── ByteFormat              byte counts for display
```

## Resources

FXML and the stylesheet live under the package that loads them, so a view is
found by name relative to its controller rather than by an absolute classpath
path:

```
src/main/resources/
├── com/example/aidocumentmanager/ui/
│   ├── MainView.fxml   app.css
│   ├── chat/ChatView.fxml
│   ├── documents/DocumentsView.fxml
│   ├── search/SearchView.fxml
│   └── settings/SettingsView.fxml
└── log4j2.xml            must stay at the classpath root for Log4j2 to find it
```

Those lookups are strings, invisible to the compiler, and only the chat tab is
loaded at startup. `ViewResourcesTest` loads every view the way the application
does, so a wrong path or a stale `fx:controller` fails the build rather than the
user's next click.

## Tests

`src/test/java` mirrors these packages. A few tests must stay in the package
they mirror because they reach into it:

- `AIServiceRestartTest` and `ConfigurationMigrationTest` reflect into the
  private `instance` field to simulate a restart.
- `DocumentIndexStoreTest` uses the package-private `DocumentIndexStore.getIndexFile()`.

Tests run against a sandboxed `user.home` set by the surefire configuration in
the pom, so they never touch a real installation's settings or index.

## Design patterns

- **MVC** - FXML views, controllers, and a domain layer that knows nothing about either
- **Singleton** - `AIService`, `ConfigurationManager`, `DialogService`, `LoggerUtil`, `ThemeManager`
- **Observer** - JavaFX property listeners drive window-state persistence and theming
- **Strategy** - per-extension text extraction in `DocumentTextExtractor`
