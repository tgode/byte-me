# Teams App Installation Guide

This guide walks through installing ByteHR AI into Microsoft Teams as a custom app.

---

## Prerequisites

- ByteHR API running and publicly accessible (via ngrok or cloud deployment)
- Azure Bot registered and messaging endpoint configured
- Teams app package (`teams/ByteHR.zip`) generated

---

## 1. Generate the App Package

```bash
# Replace <your-bot-app-id> with your Azure Bot App ID
python3 scripts/generate_teams_package.py --app-id <your-bot-app-id>
```

This creates `teams/ByteHR.zip` containing:
- `manifest.json`
- `color.png` (192×192 px color icon)
- `outline.png` (32×32 px transparent outline icon)

### Icon Requirements

| File | Size | Format | Notes |
|---|---|---|---|
| `color.png` | 192×192 px | PNG | Full color, used in Teams app list |
| `outline.png` | 32×32 px | PNG | White + transparent background |

> Add placeholder icons to `teams/` before generating the package.
> You can use any image editor or the Microsoft Teams Icon Generator.

---

## 2. Upload to Microsoft Teams

### Option A: Teams Admin Center (Recommended for Organizations)

1. Go to [Teams Admin Center](https://admin.teams.microsoft.com)
2. Navigate to **Teams apps** → **Manage apps** → **Upload new app**
3. Upload `ByteHR.zip`
4. The app will be available to all users after approval

### Option B: Upload Directly in Teams (Developer / Testing)

1. Open Microsoft Teams
2. Click **Apps** in the left sidebar
3. Click **Manage your apps** (bottom left)
4. Click **Upload an app** → **Upload a custom app**
5. Select `teams/ByteHR.zip`
6. Click **Add**

> You must be a Teams administrator or have "Upload custom apps" permission enabled.
> Enable it in Teams Admin Center → **Teams apps** → **Setup policies** → **Upload custom apps: On**

---

## 3. Use ByteHR AI

1. After installation, click **ByteHR AI** in your Teams apps
2. The bot chat will open
3. Type any HR question, e.g.:
   - "How many vacation days do I have?"
   - "Sa ditë pushimi kam?" (Albanian)
   - "Koliko dana godišnjeg odmora imam?" (Serbian)
4. The assistant responds with an answer and source citations

---

## 4. Pin ByteHR AI to the Sidebar

1. Right-click **ByteHR AI** in the Teams app list
2. Select **Pin**
3. ByteHR AI will appear in the Teams sidebar for quick access

---

## MVP Acceptance Checklist

- [ ] ByteHR AI bot installed in Teams
- [ ] Employee can send a message and receive an HR response
- [ ] Response includes source document citations
- [ ] Follow-up questions maintain conversation context
- [ ] Albanian questions receive Albanian responses
- [ ] Serbian questions receive Serbian responses
- [ ] Questions outside HR scope are rejected gracefully
- [ ] Documents sync automatically from SharePoint
- [ ] Average response time under 10 seconds
