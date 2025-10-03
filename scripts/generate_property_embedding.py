#!/usr/bin/env python3
"""Generate property embeddings using a SentenceTransformer model.

This script reads a JSON payload from STDIN that mirrors the structure emitted by
`PythonPropertyEmbeddingGenerator` and prints a JSON response containing an
`embedding` array. The implementation uses the `sentence-transformers` library
so that real multilingual transformer embeddings can be produced instead of the
previous heuristic Java-only vector.

Example usage::

    echo '{"title": "Sunny apartment", "description": "Two bedroom"}' \
        | python3 scripts/generate_property_embedding.py
"""

from __future__ import annotations

import json
import os
import sys
from dataclasses import dataclass
from typing import Iterable, List, Optional

try:
    from sentence_transformers import SentenceTransformer
except ImportError as exc:  # pragma: no cover - dependency missing at runtime
    sys.stderr.write(
        "sentence-transformers is required to run the embedding generator.\n"
        "Install it with: pip install sentence-transformers\n"
    )
    raise SystemExit(2) from exc

MODEL_NAME = os.environ.get(
    "PROPERTY_EMBEDDING_MODEL",
    "sentence-transformers/paraphrase-multilingual-MiniLM-L12-v2",
)


@dataclass
class PropertyPayload:
    title: Optional[str] = None
    description: Optional[str] = None
    buildingName: Optional[str] = None
    propertyType: Optional[str] = None
    apartmentCategory: Optional[str] = None
    address: Optional[dict] = None
    services: Optional[Iterable[dict]] = None
    furnishings: Optional[Iterable[dict]] = None

    def to_corpus(self) -> str:
        parts: List[str] = []
        self._extend(parts, self.title)
        self._extend(parts, self.description)
        self._extend(parts, self.buildingName)
        self._extend(parts, self.propertyType)
        self._extend(parts, self.apartmentCategory)

        if self.address:
            for key in ("houseNumber", "street", "addressFull", "ward", "district", "province"):
                self._extend(parts, self.address.get(key))

        if self.services:
            for service in self.services:
                for key in ("serviceName", "note", "chargeBasis"):
                    self._extend(parts, service.get(key))

        if self.furnishings:
            for furnishing in self.furnishings:
                self._extend(parts, furnishing.get("name"))

        corpus = "\n".join(part for part in parts if part)
        return corpus.strip()

    @staticmethod
    def _extend(parts: List[str], value: Optional[str]) -> None:
        if not value:
            return
        trimmed = str(value).strip()
        if trimmed:
            parts.append(trimmed)


def load_payload() -> PropertyPayload:
    raw = sys.stdin.read()
    if not raw.strip():
        raise SystemExit("Empty input payload")
    data = json.loads(raw)
    return PropertyPayload(
        title=data.get("title"),
        description=data.get("description"),
        buildingName=data.get("buildingName"),
        propertyType=data.get("propertyType"),
        apartmentCategory=data.get("apartmentCategory"),
        address=data.get("address"),
        services=data.get("services"),
        furnishings=data.get("furnishings"),
    )


def load_model() -> SentenceTransformer:
    return SentenceTransformer(MODEL_NAME)


def main() -> None:
    payload = load_payload()
    corpus = payload.to_corpus()
    if not corpus:
        print(json.dumps({"embedding": []}))
        return

    model = load_model()
    vector = model.encode(corpus, normalize_embeddings=True)
    print(json.dumps({"embedding": vector.tolist()}))


if __name__ == "__main__":
    main()
