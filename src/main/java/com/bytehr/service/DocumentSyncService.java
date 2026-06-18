package com.bytehr.service;

public interface DocumentSyncService {

    /**
     * Synchronizes documents from SharePoint: downloads new or modified documents
     * and triggers the processing pipeline for each.
     */
    void synchronize();
}
