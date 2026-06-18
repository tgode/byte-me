package com.bytehr.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "documents")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Document {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, length = 500)
    private String name;

    @Column(name = "source_path", nullable = false, length = 2000)
    private String sourcePath;

    @Column(name = "sharepoint_item_id", nullable = false, unique = true, length = 500)
    private String sharepointItemId;

    @Column(length = 10)
    private String language;

    @Column(length = 10)
    private String country;

    @Column(name = "file_type", length = 20)
    private String fileType;

    @Column(name = "file_size")
    private Long fileSize;

    @Column(name = "last_modified")
    private Instant lastModified;

    @Column(name = "last_sync")
    private Instant lastSync;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @OneToMany(mappedBy = "document", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<DocumentChunk> chunks = new ArrayList<>();

    @PrePersist
    public void prePersist() {
        createdAt = Instant.now();
    }
}
