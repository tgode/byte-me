package com.bytehr.integration.sharepoint;

import com.azure.identity.ClientSecretCredential;
import com.azure.identity.ClientSecretCredentialBuilder;
import com.bytehr.integration.sharepoint.dto.SharePointFile;
import com.microsoft.graph.authentication.TokenCredentialAuthProvider;
import com.microsoft.graph.models.DriveItem;
import com.microsoft.graph.requests.DriveItemCollectionPage;
import com.microsoft.graph.requests.GraphServiceClient;
import lombok.extern.slf4j.Slf4j;
import okhttp3.Request;
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

    private final GraphServiceClient<Request> graphClient;
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

            TokenCredentialAuthProvider authProvider = new TokenCredentialAuthProvider(
                    List.of("https://graph.microsoft.com/.default"), credential);

            this.graphClient = GraphServiceClient.builder()
                    .authenticationProvider(authProvider)
                    .buildClient();
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
            DriveItemCollectionPage items = graphClient
                    .sites(siteId)
                    .drives(driveId)
                    .root()
                    .children()
                    .buildRequest()
                    .get();

            collectSupportedFiles(items, files);
        } catch (Exception e) {
            log.error("Failed to list SharePoint documents", e);
        }
        return files;
    }

    private void collectSupportedFiles(DriveItemCollectionPage page, List<SharePointFile> files) {
        if (page == null) return;

        for (DriveItem item : page.getCurrentPage()) {
            if (item.file != null) {
                String extension = getExtension(item.name);
                if (SUPPORTED_EXTENSIONS.contains(extension)) {
                    files.add(SharePointFile.builder()
                            .id(item.id)
                            .name(item.name)
                            .webUrl(item.webUrl)
                            .downloadUrl(item.additionalDataManager().get("@microsoft.graph.downloadUrl") != null
                                    ? item.additionalDataManager().get("@microsoft.graph.downloadUrl").getAsString()
                                    : null)
                            .size(item.size)
                            .lastModified(item.lastModifiedDateTime != null
                                    ? item.lastModifiedDateTime.toInstant()
                                    : Instant.now())
                            .mimeType(item.file.mimeType)
                            .fileExtension(extension)
                            .build());
                }
            } else if (item.folder != null) {
                // Recurse into subfolders
                try {
                    DriveItemCollectionPage children = graphClient
                            .sites(siteId)
                            .drives(driveId)
                            .items(item.id)
                            .children()
                            .buildRequest()
                            .get();
                    collectSupportedFiles(children, files);
                } catch (Exception e) {
                    log.warn("Failed to list contents of folder: {}", item.name, e);
                }
            }
        }

        if (page.getNextPage() != null) {
            collectSupportedFiles(page.getNextPage().buildRequest().get(), files);
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
                .sites(siteId)
                .drives(driveId)
                .items(itemId)
                .content()
                .buildRequest()
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
