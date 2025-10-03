"# ZenRoomBackend" 

## Property embedding generator

The scheduled `PropertyEmbeddingJob` now shells out to `scripts/generate_property_embedding.py`,
which relies on the [`sentence-transformers`](https://www.sbert.net/) Python package to create
real transformer-based embeddings. Install the dependency and ensure the script is available on
the application host:

```
pip install sentence-transformers
```

You can override the Python command, script path, and timeout via the following application
properties:

```
embedding.python.command=python3
embedding.python.script=./scripts/generate_property_embedding.py
embedding.python.timeout-seconds=120
```

Set the `PROPERTY_EMBEDDING_MODEL` environment variable if you want to use a different
SentenceTransformer model (defaults to `sentence-transformers/paraphrase-multilingual-MiniLM-L12-v2`).
