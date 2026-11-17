-- pgvector adds the "vector" column type and distance operators (<=> = cosine distance)
CREATE EXTENSION IF NOT EXISTS vector;

-- One row per user: the meaning of their profile text as 768 numbers (nomic-embed-text).
-- Personal data: never returned by any API (only similarity scores), deleted with the account.
CREATE TABLE profile_embeddings (
    user_id      UUID PRIMARY KEY,
    embedding    vector(768) NOT NULL,
    content_hash CHAR(64)    NOT NULL,   -- SHA-256 of the text we embedded: same text => skip the model call
    model        VARCHAR(64) NOT NULL,   -- if we ever switch models, old rows can be found and re-embedded
    updated_at   TIMESTAMPTZ NOT NULL
);

-- HNSW = a graph index for "find the nearest vectors" without comparing against every row.
-- Not needed for re-ranking a feed (we look rows up by user_id), but ready for "people like you" searches.
CREATE INDEX profile_embeddings_hnsw ON profile_embeddings USING hnsw (embedding vector_cosine_ops);
