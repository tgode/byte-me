package com.bytehr.service;

public interface DocumentSyncService {

    /**
     * Synchronizes documents from the configured source (local or SharePoint).
     *
     * @return number of documents processed (new or updated) in this run
     */
    int synchronize();
}
