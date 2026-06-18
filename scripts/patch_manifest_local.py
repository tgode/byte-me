import json, zipfile
from pathlib import Path

ROOT = Path(__file__).parent.parent
manifest_path = ROOT / "teams" / "manifest.local.json"
zip_path = ROOT / "teams" / "ByteHR-local.zip"

URL = "https://1zntvf7f-4200.euw.devtunnels.ms/"
DOMAIN = "1zntvf7f-4200.euw.devtunnels.ms"

with open(manifest_path, encoding="utf-8") as f:
    data = json.load(f)

data["staticTabs"][0]["contentUrl"] = URL
data["staticTabs"][0]["websiteUrl"] = URL
data["validDomains"] = [DOMAIN]

with open(manifest_path, "w", encoding="utf-8") as f:
    json.dump(data, f, indent=2, ensure_ascii=False)

print("manifest.local.json updated:")
print("  contentUrl :", data["staticTabs"][0]["contentUrl"])
print("  validDomains:", data["validDomains"])

# Rebuild zip
icons_dir = ROOT / "teams"
with zipfile.ZipFile(zip_path, "w", zipfile.ZIP_DEFLATED) as zf:
    zf.write(manifest_path, "manifest.json")
    for icon in ["color.png", "outline.png"]:
        p = icons_dir / icon
        if p.exists():
            zf.write(p, icon)

print("ByteHR-local.zip regenerated:", zip_path)
