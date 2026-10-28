CREATE TABLE IF NOT EXISTS vector_collection_metadata (
    collection_name varchar(63) PRIMARY KEY,
    embedding_provider varchar(64) NOT NULL,
    embedding_model varchar(128) NOT NULL,
    embedding_dimensions integer NOT NULL CHECK (embedding_dimensions > 0),
    configuration_version varchar(64) NOT NULL,
    created_at timestamp with time zone NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at timestamp with time zone NOT NULL DEFAULT CURRENT_TIMESTAMP
);
