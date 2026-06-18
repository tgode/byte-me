# SharePoint Setup Guide

This guide explains how to connect ByteHR AI to your SharePoint document library.

---

## 1. Create Azure App Registration

1. Go to [Azure Portal](https://portal.azure.com) → **Azure Active Directory** → **App registrations** → **New registration**
2. Name: `ByteHR-SharePoint`
3. Supported account types: **Accounts in this organizational directory only**
4. Click **Register**
5. Note the **Application (client) ID** and **Directory (tenant) ID**

---

## 2. Add API Permissions

1. Go to your new app → **API permissions** → **Add a permission** → **Microsoft Graph**
2. Select **Application permissions** (not delegated)
3. Add:
   - `Sites.Read.All`
   - `Files.Read.All`
4. Click **Grant admin consent**

---

## 3. Create Client Secret

1. Go to **Certificates & secrets** → **New client secret**
2. Set an expiry (e.g. 24 months)
3. Copy the **secret value** (shown only once)

---

## 4. Find SharePoint IDs

### Site ID

```bash
# Replace with your tenant and site name
curl -H "Authorization: Bearer <token>" \
  "https://graph.microsoft.com/v1.0/sites/your-tenant.sharepoint.com:/sites/HRDocuments"
```

Copy the `id` field from the response.

### Drive ID

```bash
curl -H "Authorization: Bearer <token>" \
  "https://graph.microsoft.com/v1.0/sites/<site-id>/drives"
```

Copy the `id` of the document library drive (usually named "Documents").

---

## 5. Configure .env

```env
SHAREPOINT_TENANT_ID=xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx
SHAREPOINT_CLIENT_ID=xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx
SHAREPOINT_CLIENT_SECRET=your~secret~value
SHAREPOINT_SITE_ID=your-tenant.sharepoint.com,xxxxxxxx-xxxx,xxxxxxxx-xxxx
SHAREPOINT_DRIVE_ID=b!xxxxxxxxxxxxxxxxxxxxxxxxxx
SHAREPOINT_SYNC_ENABLED=true
```

---

## 6. Upload HR Documents

Upload your HR policy documents (PDF, DOCX, XLSX, PPTX) to the configured SharePoint drive.

Trigger the first sync:
```bash
curl -X POST http://localhost:8080/api/sync
```

---

## Supported Document Formats

| Format | Extension |
|---|---|
| PDF | .pdf |
| Word | .docx |
| Excel | .xlsx |
| PowerPoint | .pptx |
