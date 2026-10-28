CREATE TABLE IF NOT EXISTS vector_ingestion_manifest (
    source_type varchar(32) NOT NULL,
    source_id varchar(64) NOT NULL,
    document_version varchar(64) NOT NULL,
    chunk_count integer NOT NULL CHECK (chunk_count > 0),
    updated_at timestamp with time zone NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (source_type, source_id)
);
