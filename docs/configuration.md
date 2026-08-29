# Configuration and data location

On first launch the application creates a directory in your home folder:

```
~/.aidocumentmanager/
├── config.properties               # Provider, API keys, RAG settings, window state
├── documents/                      # Copies of uploaded documents
├── logs/                           # Application logs
└── index/
    └── knowledge-index.json        # Persisted documents and embeddings
```

The index is what lets an uploaded document survive a restart. It records the
embedding dimension it was written with, so vectors produced by a different
model are never searched against; a corrupt or mismatched index is discarded and
rebuilt rather than failing startup.

**API keys are stored in plain text** in `config.properties`. Treat that file as
a secret, and do not commit it. The repository's `.gitignore` already excludes
`config.properties` and `.aidocumentmanager/`.

Exported settings files are safe to share: `SettingsExporter` replaces both API
keys with `[REDACTED]`, and a test asserts no key survives anywhere in the
export.

## Settings

All of these are editable from the Settings tab and saved to
`config.properties`. `ConfigurationManager` owns the defaults and the migrations
that update an existing installation when a default changes.

### Provider

| Setting | Default | Notes |
| --- | --- | --- |
| Provider | `openai` | `openai` or `huggingface` |
| OpenAI model | `gpt-4o-mini` | A config still naming `gpt-3.5-turbo` is treated as unset |
| Hugging Face model | `meta-llama/Llama-3.1-8B-Instruct` | A config still naming `gpt2` is treated as unset |

Both providers are reached through the OpenAI client; Hugging Face exposes an
OpenAI-compatible router endpoint. With no API key the application still starts
and stays navigable: chat falls back to a mock assistant, while upload,
indexing, and search work normally.

### Retrieval

| Setting | Default | Notes |
| --- | --- | --- |
| Chunk size | 500 characters | Applies to documents indexed from then on |
| Chunk overlap | 100 characters | Must be smaller than the chunk size |
| Max retrieval results | 3 | Excerpts injected into the prompt |
| Similarity threshold | 0.5 | See below |

The threshold is **not** compared against raw cosine similarity. langchain4j
rescales with `(cosine + 1) / 2`, so 0.5 means "cosine 0" and 0.7 means "cosine
0.4". On-topic questions score around 0.70 to 0.83 against real prose, so a 0.7
threshold sits right on the cliff: rephrase the question and every chunk of a
correctly indexed document drops out. The slider is bounded at 0.9 for that
reason, and retrieval falls back to the closest chunks when the threshold
excludes everything, rather than answering with no context at all.

### Application

| Setting | Default |
| --- | --- |
| Theme | System Default |
| Max chat history | 10 |
| Logging enabled | true |
| Auto-save | true |

Window width, height, and maximized state are saved automatically as you resize.
Size is only recorded while the window is not maximized, so un-maximizing
restores the size you had before.
