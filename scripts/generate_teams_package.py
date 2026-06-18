#!/usr/bin/env python3
"""
generate_teams_package.py
Generates the ByteHR.zip Teams application package from the teams/ directory.

Usage:
    # Production / ngrok build:
    python3 scripts/generate_teams_package.py --app-id <your-teams-app-id>

    # Local development build (uses manifest.local.json -> localhost:4200):
    python3 scripts/
    generate_teams_package.py --local

Options:
    --app-id   Azure Bot App Registration ID (UUID). Optional when --local is used.
    --local    Package for local dev (uses manifest.local.json, no ngrok needed).
    --force    Allow packaging even if icon files are missing.
"""

import argparse
import json
import re
import sys
import zipfile
from pathlib import Path

TEAMS_DIR = Path(__file__).parent.parent / "teams"
OUTPUT_ZIP = TEAMS_DIR / "ByteHR.zip"
LOCAL_OUTPUT_ZIP = TEAMS_DIR / "ByteHR-local.zip"
UUID_RE = re.compile(
    r"^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$",
    re.IGNORECASE,
)
LOCAL_DEV_APP_ID = "00000000-0000-0000-0000-000000000001"


def validate_app_id(app_id: str) -> None:
    if not UUID_RE.match(app_id):
        print(f"ERROR: --app-id must be a valid UUID. Got: {app_id}", file=sys.stderr)
        sys.exit(1)


def check_icons(force: bool) -> list:
    missing = []
    for icon in ["color.png", "outline.png"]:
        if not (TEAMS_DIR / icon).exists():
            missing.append(icon)
    if missing and not force:
        print(
            f"ERROR: Missing icon file(s): {', '.join(missing)}\n"
            "Add the icons to the teams/ directory before packaging.\n"
            "  color.png   -- 192x192 px, full-colour PNG\n"
            "  outline.png -- 32x32 px, white on transparent PNG\n"
            "Use --force to generate a test package without icons.",
            file=sys.stderr,
        )
        sys.exit(1)
    return missing


def generate_package(app_id: str, force: bool, local: bool) -> None:
    validate_app_id(app_id)
    missing_icons = check_icons(force)

    if local:
        manifest_path = TEAMS_DIR / "manifest.local.json"
        output_zip = LOCAL_OUTPUT_ZIP
        if not manifest_path.exists():
            print("ERROR: teams/manifest.local.json not found.", file=sys.stderr)
            sys.exit(1)
    else:
        manifest_path = TEAMS_DIR / "manifest.json"
        output_zip = OUTPUT_ZIP
        if not manifest_path.exists():
            print("ERROR: teams/manifest.json not found.", file=sys.stderr)
            sys.exit(1)

    content = manifest_path.read_text(encoding="utf-8")
    content = content.replace("${{TEAMS_APP_ID}}", app_id)

    try:
        manifest = json.loads(content)
    except json.JSONDecodeError as e:
        print(f"ERROR: manifest JSON is not valid after substitution: {e}", file=sys.stderr)
        sys.exit(1)

    updated_manifest = TEAMS_DIR / "manifest_updated.json"
    updated_manifest.write_text(json.dumps(manifest, indent=2, ensure_ascii=False), encoding="utf-8")

    with zipfile.ZipFile(output_zip, "w", zipfile.ZIP_DEFLATED) as zf:
        zf.write(updated_manifest, "manifest.json")
        for icon in ["color.png", "outline.png"]:
            icon_path = TEAMS_DIR / icon
            if icon_path.exists():
                zf.write(icon_path, icon)
            else:
                print(f"WARNING: {icon} omitted from package (--force mode).")

    updated_manifest.unlink()

    size_kb = output_zip.stat().st_size // 1024
    if missing_icons:
        print(f"WARNING: Package generated without icons: {output_zip} ({size_kb} KB)")
        print("This package is for testing only. Add icon files before deploying.")
    else:
        print(f"Teams package generated: {output_zip} ({size_kb} KB)")

    if local:
        print()
        print("=== LOCAL DEV STEPS ===")
        print("1. Start Angular dev server:  cd frontend && npm start")
        print("2. Go to https://dev.teams.microsoft.com/apps")
        print(f"3. Click 'Import app' and upload:  {output_zip}")
        print("4. Open the app -> click 'Preview in Teams'")
        print("   The HR Assistant tab will load http://localhost:4200 inside Teams.")
    else:
        print("Upload via: Teams > Apps > Manage your apps > Upload a custom app")


if __name__ == "__main__":
    parser = argparse.ArgumentParser(description="Generate ByteHR Teams app package")
    parser.add_argument(
        "--app-id",
        default=None,
        help="Azure Bot App Registration Application ID (UUID). "
             "Optional when --local is used (defaults to dev placeholder UUID).",
    )
    parser.add_argument(
        "--local",
        action="store_true",
        default=False,
        help="Build local-dev package using manifest.local.json (localhost:4200). "
             "No ngrok/Azure Bot required.",
    )
    parser.add_argument(
        "--force",
        action="store_true",
        default=False,
        help="Generate package even if icon files are missing",
    )
    args = parser.parse_args()

    if not args.local and args.app_id is None:
        parser.error("--app-id is required unless --local is specified.")

    app_id = args.app_id if args.app_id else LOCAL_DEV_APP_ID
    generate_package(app_id, args.force, args.local)
