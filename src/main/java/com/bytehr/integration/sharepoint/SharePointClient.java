package com.bytehr.integration.sharepoint;

import com.azure.identity.ClientSecretCredential;
import com.azure.identity.ClientSecretCredentialBuilder;
import com.bytehr.integration.sharepoint.dto.SharePointFile;
import com.microsoft.graph.models.DriveItem;
import com.microsoft.graph.models.DriveItemCollectionResponse;
import com.microsoft.graph.serviceclient.GraphServiceClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@Component
@Slf4j
public class SharePointClient {

    private static final Set<String> SUPPORTED_EXTENSIONS = Set.of("pdf", "docx", "xlsx", "pptx");

    private final GraphServiceClient graphClient;
    private final String siteId;
    private final String driveId;
    private final boolean syncEnabled;

    public SharePointClient(
            @Value("${sharepoint.tenant-id}") String tenantId,
            @Value("${sharepoint.client-id}") String clientId,
            @Value("${sharepoint.client-secret}") String clientSecret,
            @Value("${sharepoint.site-id}") String siteId,
            @Value("${sharepoint.drive-id}") String driveId,
            @Value("${sharepoint.sync-enabled}") boolean syncEnabled) {
        this.siteId = siteId;
        this.driveId = driveId;
        this.syncEnabled = syncEnabled;

        if (syncEnabled && !tenantId.isBlank() && !clientId.isBlank() && !clientSecret.isBlank()) {
            ClientSecretCredential credential = new ClientSecretCredentialBuilder()
                    .tenantId(tenantId)
                    .clientId(clientId)
                    .clientSecret(clientSecret)
                    .build();

            this.graphClient = new GraphServiceClient(credential, "https://graph.microsoft.com/.default");
        } else {
            log.warn("SharePoint sync is disabled or credentials are not configured.");
            this.graphClient = null;
        }
    }

    /**
     * Lists all supported HR documents from the configured SharePoint drive.
     */
    public List<SharePointFile> listDocuments() {
        if (graphClient == null) {
            log.warn("SharePoint client not initialized. Returning empty document list.");
            return List.of();
        }

        List<SharePointFile> files = new ArrayList<>();
        try {
            DriveItemCollectionResponse response = graphClient
                    .drives()
                    .byDriveId(driveId)
                    .items()
                    .byDriveItemId("root")
                    .children()
                    .get();

            collectSupportedFiles(response, files);
        } catch (Exception e) {
            log.error("Failed to list SharePoint documents", e);
        }
        return files;
    }

    private void collectSupportedFiles(DriveItemCollectionResponse response, List<SharePointFile> files) {
        if (response == null || response.getValue() == null) return;

        for (DriveItem item : response.getValue()) {
            if (item.getFile() != null) {
                String extension = getExtension(item.getName());
                if (SUPPORTED_EXTENSIONS.contains(extension)) {
                    Object downloadUrlObj = item.getAdditionalData() != null
                            ? item.getAdditionalData().get("@microsoft.graph.downloadUrl")
                            : null;
                    String downloadUrl = downloadUrlObj != null ? downloadUrlObj.toString() : null;

                    files.add(SharePointFile.builder()
                            .id(item.getId())
                            .name(item.getName())
                            .webUrl(item.getWebUrl())
                            .downloadUrl(downloadUrl)
                            .size(item.getSize())
                            .lastModified(item.getLastModifiedDateTime() != null
                                    ? item.getLastModifiedDateTime().toInstant()
                                    : Instant.now())
                            .mimeType(item.getFile().getMimeType())
                            .fileExtension(extension)
                            .build());
                }
            } else if (item.getFolder() != null) {
                try {
                    DriveItemCollectionResponse children = graphClient
                            .drives()
                            .byDriveId(driveId)
                            .items()
                            .byDriveItemId(item.getId())
                            .children()
                            .get();
                    collectSupportedFiles(children, files);
                } catch (Exception e) {
                    log.warn("Failed to list contents of folder: {}", item.getName(), e);
                }
            }
        }
    }

    /**
     * Downloads a document as a byte array from SharePoint.
     */
    public byte[] downloadDocument(String itemId) {
        if (graphClient == null) {
            throw new IllegalStateException("SharePoint client not initialized");
        }
        try (InputStream stream = graphClient
                .drives()
                .byDriveId(driveId)
                .items()
                .byDriveItemId(itemId)
                .content()
                .get()) {
            return stream != null ? stream.readAllBytes() : new byte[0];
        } catch (Exception e) {
            log.error("Failed to download SharePoint item: {}", itemId, e);
            throw new RuntimeException("Failed to download document: " + itemId, e);
        }
    }

    private String getExtension(String filename) {
        if (filename == null || !filename.contains(".")) return "";
        return filename.substring(filename.lastIndexOf('.') + 1).toLowerCase();
    }
}
