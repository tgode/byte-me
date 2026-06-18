#!/usr/bin/env python3
"""
generate_teams_package.py
Generates the ByteHR.zip Teams application package from the teams/ directory.

Usage:
    python3 scripts/generate_teams_package.py --app-id <your-teams-app-id>
    python3 scripts/generate_teams_package.py --app-id <your-teams-app-id> --force

Options:
    --app-id   Azure Bot App Registration Application ID (UUID format)
    --force    Allow packaging even if icon files are missing (placeholder package only)
"""

import argparse
import json
import os
import re
import sys
import zipfile
from pathlib import Path

TEAMS_DIR = Path(__file__).parent.parent / "teams"
OUTPUT_ZIP = TEAMS_DIR / "ByteHR.zip"
UUID_RE = re.compile(
    r"^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$",
    re.IGNORECASE,
)


def validate_app_id(app_id: str) -> None:
    if not UUID_RE.match(app_id):
        print(f"ERROR: --app-id must be a valid UUID. Got: {app_id}", file=sys.stderr)
        sys.exit(1)


def check_icons(force: bool) -> list[str]:
    missing = []
    for icon in ["color.png", "outline.png"]:
        if not (TEAMS_DIR / icon).exists():
            missing.append(icon)
    if missing and not force:
        print(
            f"ERROR: Missing icon file(s): {', '.join(missing)}\n"
            "Add the icons to the teams/ directory before packaging.\n"
            "  color.png   — 192x192 px, full-colour PNG\n"
            "  outline.png — 32x32 px, white on transparent PNG\n"
            "Use --force to generate a test package without icons.",
            file=sys.stderr,
        )
        sys.exit(1)
    return missing


def generate_package(app_id: str, force: bool) -> None:
    validate_app_id(app_id)
    missing_icons = check_icons(force)

    manifest_path = TEAMS_DIR / "manifest.json"
    if not manifest_path.exists():
        print("ERROR: teams/manifest.json not found.", file=sys.stderr)
        sys.exit(1)

    content = manifest_path.read_text(encoding="utf-8")
    content = content.replace("${{TEAMS_APP_ID}}", app_id)

    try:
        manifest = json.loads(content)
    except json.JSONDecodeError as e:
        print(f"ERROR: manifest.json is not valid JSON after substitution: {e}", file=sys.stderr)
        sys.exit(1)

    # Write temp manifest with substituted values
    updated_manifest = TEAMS_DIR / "manifest_updated.json"
    updated_manifest.write_text(json.dumps(manifest, indent=2, ensure_ascii=False), encoding="utf-8")

    with zipfile.ZipFile(OUTPUT_ZIP, "w", zipfile.ZIP_DEFLATED) as zf:
        zf.write(updated_manifest, "manifest.json")
        for icon in ["color.png", "outline.png"]:
            icon_path = TEAMS_DIR / icon
            if icon_path.exists():
                zf.write(icon_path, icon)
            else:
                print(f"WARNING: {icon} omitted from package (--force mode).")

    updated_manifest.unlink()

    size_kb = OUTPUT_ZIP.stat().st_size // 1024
    if missing_icons:
        print(f"WARNING: Package generated without icons: {OUTPUT_ZIP} ({size_kb} KB)")
        print("This package is for testing only. Add icon files before deploying to production.")
    else:
        print(f"Teams package generated: {OUTPUT_ZIP} ({size_kb} KB)")
        print(f"Upload this file via: Teams > Apps > Manage your apps > Upload a custom app")


if __name__ == "__main__":
    parser = argparse.ArgumentParser(description="Generate ByteHR Teams app package")
    parser.add_argument(
        "--app-id",
        required=True,
        help="Azure Bot App Registration Application ID (UUID)",
    )
    parser.add_argument(
        "--force",
        action="store_true",
        default=False,
        help="Generate package even if icon files are missing",
    )
    args = parser.parse_args()
    generate_package(args.app_id, args.force)

