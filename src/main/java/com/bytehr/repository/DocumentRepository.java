package com.bytehr.repository;

import com.bytehr.model.Document;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface DocumentRepository extends JpaRepository<Document, UUID> {

    Optional<Document> findBySharepointItemId(String sharepointItemId);

    List<Document> findByCountry(String country);

    List<Document> findByLastSyncBefore(Instant before);
}
