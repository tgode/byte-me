# ByteHR AI — Microsoft Teams Deployment Audit

**Date:** 2026-06-18  
**Auditor:** GitHub Copilot  
**Scope:** Teams manifest, icons, package generation, deployment readiness

---

## Audit Results Summary

| # | Check | Result | Notes |
|---|---|---|---|
| 1 | manifest.json schema version | ✅ PASS | `1.16` — current supported version |
| 2 | Teams Personal App configuration | ✅ PASS (after fix) | Bot scope `personal`, 3 command hints |
| 3 | Package generation script | ✅ PASS (after fix) | UUID validation, error-on-missing-icons |
| 4 | color.png exists and is valid | ✅ PASS (after fix) | 192×192 px, valid PNG |
| 5 | outline.png exists and is valid | ✅ PASS (after fix) | 32×32 px, valid RGBA PNG |
| 6 | All required manifest fields | ✅ PASS | All 10 required fields present |
| 7 | Bot registration documented | ✅ PASS | docs/deployment/README.md §2a |
| 8 | Teams Custom App Upload ready | ✅ PASS | `ByteHR.zip` generates correctly |

---

## Issues Found and Fixed

### Fix-T1 — Missing Icon Files

**Symptom:** `teams/color.png` and `teams/outline.png` did not exist.  
**Impact:** Teams package could not be uploaded — Teams rejects packages with missing icons.  
**Fix:** Generated valid placeholder PNG files using Python stdlib (`struct` + `zlib`):

| File | Dimensions | Format | Content |
|---|---|---|---|
| `color.png` | 192×192 px | RGBA PNG | Microsoft blue (#0078D4) background with white centre block |
| `outline.png` | 32×32 px | RGBA PNG | Transparent background with white centre block |

> **Production note:** Replace these placeholders with the final ByteHR branded icons
> before end-user deployment. Specifications: `color.png` 192×192 full-colour PNG;
> `outline.png` 32×32 white-on-transparent PNG.

---

### Fix-T2 — webApplicationInfo Block Removed

**Symptom:** `manifest.json` contained a `webApplicationInfo` block with placeholder URLs
(`api://bytehr.example.com/...`).  
**Impact:** Teams manifest validation fails when `webApplicationInfo.resource` is not a
registered Azure AD application URL. This block is only required for SSO scenarios, which
are out of scope for the ByteHR MVP.  
**Fix:** Removed the `webApplicationInfo` block entirely. It can be re-added if SSO is
implemented in a future version.

---

### Fix-T3 — Em-dash Characters in name/description

**Symptom:** `name.full` and `description.full` used em-dash (`—`, U+2014).  
**Impact:** Some Teams manifest validators and older Teams client versions reject non-ASCII
characters in these fields.  
**Fix:** Replaced em-dashes with standard ASCII hyphens (`-`).

---

### Fix-T4 — Package Script: Warnings Upgraded to Errors; UUID Validation Added

**Symptom:** The original `generate_teams_package.py` only printed a `WARNING` when icon
files were missing, still exiting with code 0. An invalid `--app-id` was accepted silently.  
**Impact:** Developers could accidentally generate and upload an invalid package (no icons,
wrong app-id format).  
**Fix:**
- Script now **errors and exits 1** when icons are missing (unless `--force` is passed)
- Script validates `--app-id` is a proper UUID before proceeding
- JSON parse error after substitution is caught and reported
- Output now includes the Teams upload path reminder

---

### Fix-T5 — Added commandLists to Bot Configuration

**Symptom:** The bot had no `commandLists` defined.  
**Impact:** Teams shows no suggested commands in the compose box, reducing discoverability.  
**Fix:** Added 3 example commands (`Help`, `Vacation`, `Benefits`) to guide employees.

---

## Manifest Validation — Full Field Report

```
Field                  Status    Value
─────────────────────────────────────────────────────────────
$schema                ✅        https://developer.microsoft.com/.../v1.16/...
manifestVersion        ✅        1.16
version                ✅        1.0.0
id                     ✅        ${{TEAMS_APP_ID}} (replaced at package time)
packageName            ✅        com.bytehr.teams
developer.name         ✅        ByteHR Team
developer.websiteUrl   ✅        https://bytehr.example.com
developer.privacyUrl   ✅        https://bytehr.example.com/privacy
developer.termsOfUseUrl ✅       https://bytehr.example.com/terms
name.short             ✅        ByteHR AI (9/30 chars)
name.full              ✅        ByteHR AI - HR Assistant (24/100 chars)
description.short      ✅        AI-powered HR... (38/80 chars)
description.full       ✅        166/4000 chars
icons.color            ✅        color.png (192x192 px ✅)
icons.outline          ✅        outline.png (32x32 px ✅)
accentColor            ✅        #0078D4
bots[0].botId          ✅        ${{TEAMS_APP_ID}} (replaced at package time)
bots[0].scopes         ✅        ["personal"]
bots[0].commandLists   ✅        3 commands
permissions            ✅        ["identity", "messageTeamMembers"]
validDomains           ✅        [] (bot-only app, no web content)
webApplicationInfo     ✅        Removed (SSO not required for MVP)
```

---

## Icon File Verification

| File | Size | Dimensions | PNG Valid | Color Type |
|---|---|---|---|---|
| `color.png` | 544 bytes | 192×192 px | ✅ | RGBA (6) |
| `outline.png` | 95 bytes | 32×32 px | ✅ | RGBA (6) |

---

## Package Generation — Test Results

| Test | Command | Result |
|---|---|---|
| Missing `--app-id` | `python3 scripts/generate_teams_package.py` | ✅ Exits 1, usage shown |
| Invalid UUID | `--app-id not-a-uuid` | ✅ Exits 1, clear error |
| Valid UUID, icons present | `--app-id <valid-uuid>` | ✅ `ByteHR.zip` generated (1.3 KB) |
| Verify ZIP contents | `zipfile.infolist()` | ✅ `manifest.json`, `color.png`, `outline.png` |
| Template substitution | Inspect manifest in ZIP | ✅ `${{TEAMS_APP_ID}}` replaced, no placeholders remain |

---

## Teams Package Contents

The generated `teams/ByteHR.zip` contains exactly:

```
ByteHR.zip
├── manifest.json   (1,717 bytes — schema v1.16, bot configured)
├── color.png       (544 bytes — 192×192 px placeholder icon)
└── outline.png     (95 bytes — 32×32 px transparent placeholder icon)
```

---

## Bot / App Registration Requirements

### Azure Bot Registration (required before deployment)

1. Go to [Azure Portal](https://portal.azure.com) → **Azure Bot** → **Create**
2. Choose **Multi Tenant**
3. Create a new **App Registration** (or use an existing one)
4. Note the **Application (client) ID** → this is your `TEAMS_APP_ID`
5. Generate a **Client Secret** → this is your `TEAMS_APP_PASSWORD`
6. Under **Channels** → add **Microsoft Teams**
7. Set **Messaging Endpoint**:
   - Local testing: `https://<ngrok-subdomain>.ngrok-free.app/api/messages`
   - Production: `https://<your-domain>/api/messages`
8. Update `.env` with the App ID and password

### Generate Teams Package

```bash
python3 scripts/generate_teams_package.py --app-id <your-azure-bot-app-id>
```

### Upload to Teams (Custom App Upload)

**Option A — Direct upload (developer/tester):**
1. Open Microsoft Teams
2. Click **Apps** → **Manage your apps** (bottom-left)
3. Click **Upload an app** → **Upload a custom app**
4. Select `teams/ByteHR.zip`
5. Click **Add**

**Option B — Organisation-wide deployment:**
1. Go to [Teams Admin Center](https://admin.teams.microsoft.com)
2. **Teams apps** → **Manage apps** → **Upload new app**
3. Upload `teams/ByteHR.zip`
4. Optionally configure an **App Setup Policy** to auto-pin ByteHR for all users

### Required Teams Admin Permission

Custom App Upload must be enabled:
- Teams Admin Center → **Teams apps** → **Setup policies** → Edit Global policy
- Set **Upload custom apps** = **On**

---

## Teams Custom App Upload — Checklist

- [ ] Azure Bot registered with Microsoft Teams channel enabled
- [ ] Messaging endpoint configured (ngrok for local / domain for production)
- [ ] `.env` updated with `TEAMS_APP_ID` and `TEAMS_APP_PASSWORD`
- [ ] `python3 scripts/generate_teams_package.py --app-id <uuid>` runs without errors
- [ ] `teams/ByteHR.zip` contains `manifest.json`, `color.png`, `outline.png`
- [ ] Teams Admin has enabled Custom App Upload
- [ ] Package uploaded via Teams → Apps → Upload a custom app
- [ ] Bot responds to a test message in Teams personal chat
- [ ] Source citations appear in the bot response
- [ ] Follow-up question works (conversation context maintained)

---

## Production Icon Replacement

The current icons are programmatically generated placeholders. Before end-user deployment,
replace them with branded icons meeting Teams specifications:

```
color.png
  Size:       192×192 pixels
  Format:     PNG (RGB or RGBA)
  Background: Any solid colour (recommended: #0078D4)
  Content:    ByteHR logo, brand mark, or "HR" styled text

outline.png
  Size:       32×32 pixels
  Format:     PNG with alpha channel (RGBA)
  Background: Fully transparent (alpha = 0)
  Foreground: White (#FFFFFF, alpha = 255)
  Content:    Simple single-colour outline of the ByteHR mark
```

Quick placeholder generation (ImageMagick, if available):
```bash
convert -size 192x192 xc:#0078D4 \
  -fill white -gravity center -font DejaVu-Sans-Bold -pointsize 56 \
  -annotate 0 "HR" teams/color.png

convert -size 32x32 xc:transparent \
  -fill white -gravity center -font DejaVu-Sans-Bold -pointsize 14 \
  -annotate 0 "HR" teams/outline.png
```
