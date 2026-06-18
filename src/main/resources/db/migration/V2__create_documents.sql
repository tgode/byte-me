-- HR Documents synced from SharePoint
CREATE TABLE IF NOT EXISTS documents (
    id                  UUID        NOT NULL DEFAULT gen_random_uuid() PRIMARY KEY,
    name                VARCHAR(500) NOT NULL,
    source_path         VARCHAR(2000) NOT NULL,
    sharepoint_item_id  VARCHAR(500) NOT NULL,
    language            VARCHAR(10),
    country             VARCHAR(10),
    file_type           VARCHAR(20),
    file_size           BIGINT,
    last_modified       TIMESTAMP WITH TIME ZONE,
    last_sync           TIMESTAMP WITH TIME ZONE,
    created_at          TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_documents_sharepoint_item_id UNIQUE (sharepoint_item_id)
);

CREATE INDEX IF NOT EXISTS idx_documents_sharepoint_item_id ON documents (sharepoint_item_id);
CREATE INDEX IF NOT EXISTS idx_documents_country             ON documents (country);
CREATE INDEX IF NOT EXISTS idx_documents_language            ON documents (language);
