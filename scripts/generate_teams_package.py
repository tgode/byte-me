#!/usr/bin/env python3
"""
generate_teams_package.py
Generates the ByteHR.zip Teams application package from the teams/ directory.
Run this script after replacing ${{TEAMS_APP_ID}} in manifest.json.

Usage:
    python3 scripts/generate_teams_package.py --app-id <your-teams-app-id>
"""

import argparse
import json
import os
import zipfile
from pathlib import Path

TEAMS_DIR = Path(__file__).parent.parent / "teams"
OUTPUT_ZIP = TEAMS_DIR / "ByteHR.zip"


def generate_package(app_id: str) -> None:
    # Update manifest
    manifest_path = TEAMS_DIR / "manifest.json"
    with open(manifest_path, "r") as f:
        content = f.read()

    content = content.replace("${{TEAMS_APP_ID}}", app_id)
    manifest = json.loads(content)

    # Write updated manifest to temp file
    updated_manifest = TEAMS_DIR / "manifest_updated.json"
    with open(updated_manifest, "w") as f:
        json.dump(manifest, f, indent=2)

    # Create zip
    with zipfile.ZipFile(OUTPUT_ZIP, "w", zipfile.ZIP_DEFLATED) as zf:
        zf.write(updated_manifest, "manifest.json")

        for icon in ["color.png", "outline.png"]:
            icon_path = TEAMS_DIR / icon
            if icon_path.exists():
                zf.write(icon_path, icon)
            else:
                print(f"WARNING: {icon} not found. Add it to the teams/ directory before deploying.")

    updated_manifest.unlink()
    print(f"Teams package generated: {OUTPUT_ZIP}")


if __name__ == "__main__":
    parser = argparse.ArgumentParser()
    parser.add_argument("--app-id", required=True, help="Teams Bot App ID (Azure Bot Registration)")
    args = parser.parse_args()
    generate_package(args.app_id)
