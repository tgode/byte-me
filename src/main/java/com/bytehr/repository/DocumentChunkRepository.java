package com.bytehr.repository;

import com.bytehr.model.DocumentChunk;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface DocumentChunkRepository extends JpaRepository<DocumentChunk, UUID> {

    @Modifying
    @Query("DELETE FROM DocumentChunk dc WHERE dc.document.id = :documentId")
    void deleteByDocumentId(@Param("documentId") UUID documentId);

    long countByDocumentId(UUID documentId);
}
