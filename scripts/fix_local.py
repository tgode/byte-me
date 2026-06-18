import json, zipfile
from pathlib import Path

ROOT = Path(r'c:\Users\kejkostdhi\byte-me')

# 1. Fix main.ts — bootstrap immediately, init Teams SDK in background
main_ts = ROOT / 'frontend' / 'src' / 'main.ts'
main_ts.write_text(
    'import { bootstrapApplication } from \'@angular/platform-browser\';\n'
    'import { appConfig } from \'./app/app.config\';\n'
    'import { AppComponent } from \'./app/app.component\';\n'
    'import { app } from \'@microsoft/teams-js\';\n'
    '\n'
    '// Bootstrap Angular immediately — never block on Teams SDK\n'
    'bootstrapApplication(AppComponent, appConfig).catch(err => console.error(err));\n'
    '\n'
    '// Init Teams SDK in the background\n'
    'app.initialize().then(() => {\n'
    '  app.notifyAppLoaded();\n'
    '  app.notifySuccess();\n'
    '}).catch(() => {});\n',
    encoding='utf-8'
)
print('main.ts fixed')

# 2. Update manifest with --allow-anonymous tunnel URL
URL = 'https://psd8xfj9-4200.euw.devtunnels.ms/'
DOMAIN = 'psd8xfj9-4200.euw.devtunnels.ms'
mp = ROOT / 'teams' / 'manifest.local.json'
data = json.loads(mp.read_text(encoding='utf-8'))
data['staticTabs'][0]['contentUrl'] = URL
data['staticTabs'][0]['websiteUrl'] = URL
data['validDomains'] = [DOMAIN]
mp.write_text(json.dumps(data, indent=2), encoding='utf-8')
print('manifest updated:', URL)

# 3. Regenerate zip
zp = ROOT / 'teams' / 'ByteHR-local.zip'
with zipfile.ZipFile(zp, 'w', zipfile.ZIP_DEFLATED) as z:
    z.write(mp, 'manifest.json')
    for icon in ['color.png', 'outline.png']:
        p = ROOT / 'teams' / icon
        if p.exists():
            z.write(p, icon)
print('ByteHR-local.zip regenerated')
