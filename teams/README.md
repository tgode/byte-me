# ByteHR Teams App Package

This directory contains the Microsoft Teams application package for ByteHR AI.

## Files

| File | Required | Description |
|---|---|---|
| `manifest.json` | ✅ | Teams app manifest (template — replace `${{TEAMS_APP_ID}}`) |
| `color.png` | ✅ | 192×192 px full-color app icon |
| `outline.png` | ✅ | 32×32 px transparent outline icon |
| `ByteHR.zip` | Generated | Final package to upload to Teams |

## Icon Specifications

### color.png
- Size: **192×192 pixels**
- Format: PNG
- Background: Any color (recommended: `#0078D4` — Microsoft blue)
- Content: ByteHR logo or "HR" text on branded background

### outline.png
- Size: **32×32 pixels**
- Format: PNG
- Background: **Transparent**
- Foreground: **White** (#FFFFFF)
- Content: Simple outline of the ByteHR logo

> You can create placeholder icons quickly with ImageMagick:
> ```bash
> # color.png placeholder (blue background, white text)
> convert -size 192x192 xc:#0078D4 -fill white -gravity center \
>   -font DejaVu-Sans-Bold -pointsize 48 -annotate 0 "HR" teams/color.png
>
> # outline.png placeholder (transparent background, white icon)
> convert -size 32x32 xc:transparent -fill white -gravity center \
>   -font DejaVu-Sans-Bold -pointsize 14 -annotate 0 "HR" teams/outline.png
> ```

## Generate Package

```bash
python3 scripts/generate_teams_package.py --app-id <your-teams-bot-app-id>
```

This produces `teams/ByteHR.zip` ready for upload to Teams.
