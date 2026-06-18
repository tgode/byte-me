const fs   = require('fs');
const path = require('path');
const { Client } = require('pg');
const http = require('http');

const PG_CONFIG = { host:'localhost', port:5432, database:'bytehr', user:'bytehr', password:'bytehr' };
const OLLAMA    = 'http://localhost:11434';
const MODEL     = 'nomic-embed-text:latest';
const CHUNK_SZ  = 1000;
const OVERLAP   = 200;

function fetchJson(url, body) {
  return new Promise((resolve, reject) => {
    const parsed = new URL(url);
    const data = JSON.stringify(body);
    const req = http.request({
      hostname: parsed.hostname, port: parsed.port, path: parsed.pathname,
      method: 'POST', headers: { 'Content-Type':'application/json', 'Content-Length': Buffer.byteLength(data) }
    }, res => {
      let buf = ''; res.on('data', d => buf += d);
      res.on('end', () => { try { resolve(JSON.parse(buf)); } catch { reject(new Error(buf.slice(0,300))); } });
    });
    req.on('error', reject);
    req.setTimeout(60000, () => { req.destroy(); reject(new Error('Ollama timeout')); });
    req.write(data); req.end();
  });
}

function chunk(text) {
  const chunks = [];
  let start = 0;
  while (start < text.length) {
    const end = Math.min(start + CHUNK_SZ, text.length);
    chunks.push(text.slice(start, end));
    if (end === text.length) break;
    start += CHUNK_SZ - OVERLAP;
  }
  return chunks.filter(c => c.trim().length > 20);
}

const SOURCES = [
  { dir: '/home/tgode/env/byte-me/sample-data/hr-documents/albania', country: 'AL' },
  { dir: '/home/tgode/env/byte-me/sample-data/hr-documents/serbia',  country: 'RS' },
  { dir: '/home/tgode/env/byte-me/sample-data/hr-documents/global',  country: null }
];

async function main() {
  const client = new Client(PG_CONFIG);
  await client.connect();
  console.log('Connected to PostgreSQL');

  let totalDocs = 0, totalChunks = 0;

  for (const { dir, country } of SOURCES) {
    const files = fs.readdirSync(dir).filter(f => f.endsWith('.md'));
    for (const file of files) {
      const content = fs.readFileSync(path.join(dir, file), 'utf8');
      const name    = path.basename(file, '.md');
      const itemId  = `seed-${name}-${country || 'global'}`;
      process.stdout.write(`  [${country || 'GLOBAL'}] ${file} ... `);

      // Upsert document
      const upsertRes = await client.query(`
        INSERT INTO documents (id, name, source_path, sharepoint_item_id, country, file_type, last_sync, created_at)
        VALUES (gen_random_uuid(), $1, $2, $3, $4, 'md', NOW(), NOW())
        ON CONFLICT (sharepoint_item_id) DO UPDATE SET last_sync = NOW()
        RETURNING id
      `, [name, `sample-data/${file}`, itemId, country]);
      const docId = upsertRes.rows[0].id;

      await client.query('DELETE FROM document_chunks WHERE document_id=$1', [docId]);

      const chunks = chunk(content);
      for (let i = 0; i < chunks.length; i++) {
        const resp = await fetchJson(`${OLLAMA}/api/embed`, { model: MODEL, input: chunks[i] });
        const vec = resp.embeddings[0];
        const vecStr = '[' + vec.join(',') + ']';
        await client.query(`
          INSERT INTO document_chunks (id, document_id, content, chunk_index, embedding)
          VALUES (gen_random_uuid(), $1, $2, $3, $4::vector)
        `, [docId, chunks[i], i, vecStr]);
      }
      totalDocs++;
      totalChunks += chunks.length;
      console.log(`✔  ${chunks.length} chunks`);
    }
  }

  await client.end();
  console.log(`\n✅ Done! ${totalDocs} documents, ${totalChunks} chunks loaded.`);
}

main().catch(e => { console.error('ERROR:', e.message, e.stack); process.exit(1); });
