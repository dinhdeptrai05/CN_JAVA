import { readFileSync, writeFileSync } from 'node:fs';
import { resolve } from 'node:path';

const root = resolve(import.meta.dirname, '..');
const source = readFileSync(resolve(root, 'docs/UniSchedule.dbml'), 'utf8');
const output = resolve(root, 'docs/UniSchedule_Database_Diagram.svg');

const groups = [
  { name: 'DANH TÍNH', color: '#2563eb', tables: ['departments', 'roles', 'users', 'user_roles'] },
  { name: 'ĐÀO TẠO', color: '#7c3aed', tables: ['semesters', 'courses', 'course_sections', 'lecturer_assignments', 'student_enrollments'] },
  { name: 'TÀI NGUYÊN', color: '#059669', tables: ['time_slots', 'classrooms', 'equipment', 'classroom_equipment'] },
  { name: 'LỊCH & XỬ LÝ', color: '#d97706', tables: ['schedules', 'change_requests', 'notifications', 'audit_logs', 'maintenance_records'] },
];
const tableMap = new Map();
for (const match of source.matchAll(/^Table (\w+) \{\r?\n([\s\S]*?)^\}/gm)) {
  const fields = [];
  for (const line of match[2].split(/\r?\n/)) {
    if (line.trim() === 'indexes {') continue;
    const field = line.match(/^  ([a-z][\w]*) ([^\s]+)(?: \[(.*?)\])?$/);
    if (field) fields.push({ name: field[1], type: field[2], options: field[3] ?? '' });
  }
  tableMap.set(match[1], { name: match[1], fields });
}
if (tableMap.size !== 18) throw new Error(`Expected 18 SQL tables, found ${tableMap.size}`);

const refs = [...source.matchAll(/^Ref: (\w+)\.(?:\(([^)]+)\)|(\w+)) > (\w+)\.(?:\(([^)]+)\)|(\w+))/gm)].map(m => ({
  fromTable: m[1], fromFields: (m[2] ?? m[3]).split(',').map(s => s.trim()),
  toTable: m[4], toFields: (m[5] ?? m[6]).split(',').map(s => s.trim()),
}));
if (refs.length !== 30) throw new Error(`Expected 30 foreign keys, found ${refs.length}`);
for (const ref of refs) {
  const child = tableMap.get(ref.fromTable), parent = tableMap.get(ref.toTable);
  if (!child || !parent) throw new Error(`Unknown table in ${JSON.stringify(ref)}`);
  for (const name of ref.fromFields) {
    const field = child.fields.find(f => f.name === name);
    if (!field) throw new Error(`Missing ${ref.fromTable}.${name}`);
    field.foreign = true;
  }
  for (const name of ref.toFields) if (!parent.fields.some(f => f.name === name)) throw new Error(`Missing ${ref.toTable}.${name}`);
}
for (const name of ['user_id', 'role_id']) tableMap.get('user_roles').fields.find(f => f.name === name).compositePrimary = true;

const esc = s => String(s).replaceAll('&', '&amp;').replaceAll('<', '&lt;').replaceAll('>', '&gt;').replaceAll('"', '&quot;');
// Compact, scattered table boxes and a fine grid echo the user's database-diagram reference.
const cardW = 390, rowH = 22, headerH = 34, footerH = 20;
const positions = {
  roles: [70, 180], departments: [70, 430], semesters: [70, 730], time_slots: [70, 1100],
  users: [690, 180], user_roles: [690, 700], courses: [690, 960], classrooms: [690, 1320],
  notifications: [1310, 180], audit_logs: [1310, 640], course_sections: [1310, 1010], equipment: [1310, 1450],
  lecturer_assignments: [1930, 190], student_enrollments: [1930, 510], classroom_equipment: [1930, 850], maintenance_records: [1930, 1260],
  schedules: [2550, 310], change_requests: [2550, 990],
};
for (const [name, [x, y]] of Object.entries(positions)) {
  const t = tableMap.get(name);
  if (!t) throw new Error(`Missing positioned table ${name}`);
  t.x = x; t.y = y; t.h = headerH + t.fields.length * rowH + footerH;
}
const width = 3020, height = 1800;
const svg = [];
const endpointMarks = [];
svg.push(`<svg xmlns="http://www.w3.org/2000/svg" width="${width}" height="${height}" viewBox="0 0 ${width} ${height}" role="img" aria-label="UniSchedule database diagram in the style of the supplied reference, with 18 compact tables and 30 foreign keys">`);
svg.push(`<defs><pattern id="fineGrid" width="10" height="10" patternUnits="userSpaceOnUse"><path d="M 10 0 L 0 0 0 10" fill="none" stroke="#e8edf2" stroke-width=".65"/></pattern><pattern id="largeGrid" width="50" height="50" patternUnits="userSpaceOnUse"><path d="M 50 0 L 0 0 0 50" fill="none" stroke="#d6dee7" stroke-width=".8"/></pattern><filter id="shadow" x="-20%" y="-20%" width="140%" height="150%"><feDropShadow dx="0" dy="2" stdDeviation="2" flood-color="#8aa0b2" flood-opacity=".25"/></filter></defs>`);
svg.push(`<style>text{font-family:Arial,'DejaVu Sans',sans-serif;fill:#243342} .title{font-size:25px;font-weight:700} .sub{font-size:14px;fill:#5c6b78} .table{font-size:16px;font-weight:700;fill:#183951} .field{font-size:12.5px} .type{font-size:11.5px;fill:#647583} .badge{font-size:9px;font-weight:700;fill:#fff} .footer{font-size:10px;fill:#6b7b86} .conn{fill:none;stroke:#58646e;stroke-width:1.6;stroke-dasharray:9 6;stroke-linecap:square;stroke-linejoin:miter;opacity:.82}</style>`);
svg.push(`<rect width="${width}" height="${height}" fill="#fff"/><rect width="${width}" height="${height}" fill="url(#fineGrid)"/><rect width="${width}" height="${height}" fill="url(#largeGrid)"/>`);
svg.push(`<text x="70" y="55" class="title">UniSchedule — Database Diagram</text>`);
svg.push(`<text x="70" y="80" class="sub">Lược đồ vật lý theo ảnh mẫu · MySQL · 18 bảng · 30 khóa ngoại · PK / FK / UQ</text>`);
svg.push(`<line x1="70" y1="105" x2="2940" y2="105" stroke="#aabac7" stroke-width="1"/>`);

// Right-angle dashed links are drawn before table cards, like an ERD canvas.
for (let i = 0; i < refs.length; i++) {
  const ref = refs[i], from = tableMap.get(ref.fromTable), to = tableMap.get(ref.toTable);
  const fromIdx = from.fields.findIndex(f => f.name === ref.fromFields[0]);
  const toIdx = to.fields.findIndex(f => f.name === ref.toFields[0]);
  const fy = from.y + headerH + fromIdx * rowH + rowH / 2;
  const ty = to.y + headerH + toIdx * rowH + rowH / 2;
  let sx, tx, bend, fromSign, toSign;
  if (from.x > to.x) {
    sx = from.x; tx = to.x + cardW; fromSign = -1; toSign = 1;
    bend = (sx + tx) / 2 + ((i % 5) - 2) * 10;
  } else if (from.x < to.x) {
    sx = from.x + cardW; tx = to.x; fromSign = 1; toSign = -1;
    bend = (sx + tx) / 2 + ((i % 5) - 2) * 10;
  } else {
    sx = from.x + cardW; tx = to.x + cardW; fromSign = 1; toSign = 1;
    bend = sx + 28 + (i % 6) * 12;
  }
  svg.push(`<path class="conn" d="M ${sx} ${fy} H ${bend} V ${ty} H ${tx}"/>`);
  const ftip = sx + fromSign * 9, pbar = tx + toSign * 8;
  endpointMarks.push(`<path d="M ${sx} ${fy - 5} L ${ftip} ${fy} L ${sx} ${fy + 5} M ${pbar} ${ty - 6} V ${ty + 6}" fill="none" stroke="#4d5b66" stroke-width="1.5"/>`);
}

for (const t of tableMap.values()) {
  const x = t.x, y = t.y;
  svg.push(`<g filter="url(#shadow)"><rect x="${x}" y="${y}" width="${cardW}" height="${t.h}" rx="5" fill="#fff" stroke="#8ca6b9" stroke-width="1.2"/>`);
  svg.push(`<path d="M ${x + 5} ${y} H ${x + cardW - 5} Q ${x + cardW} ${y} ${x + cardW} ${y + 5} V ${y + headerH} H ${x} V ${y + 5} Q ${x} ${y} ${x + 5} ${y}" fill="#a9cbe0" stroke="#7ba3be" stroke-width="1"/>`);
  svg.push(`<rect x="${x + 8}" y="${y + 8}" width="14" height="14" rx="2" fill="#e6f4fc" stroke="#6c91a9"/><path d="M ${x + 10} ${y + 13} H ${x + 20} M ${x + 10} ${y + 17} H ${x + 20}" stroke="#6c91a9" stroke-width="1"/>`);
  svg.push(`<text x="${x + 29}" y="${y + 23}" class="table">${esc(t.name)}</text><path d="M ${x + cardW - 17} ${y + 13} l 5 0 -2.5 4 z" fill="#54748a"/>`);
  for (let i = 0; i < t.fields.length; i++) {
    const f = t.fields[i], ry = y + headerH + i * rowH, cy = ry + 15;
    if (i) svg.push(`<line x1="${x + 1}" y1="${ry}" x2="${x + cardW - 1}" y2="${ry}" stroke="#eff2f5" stroke-width=".8"/>`);
    const marks = [];
    if (f.options.includes('pk') || f.compositePrimary) marks.push('P');
    if (f.foreign) marks.push('F');
    if (f.options.includes('unique')) marks.push('U');
    const mark = marks.join('');
    if (mark) {
      const color = mark.includes('P') ? '#d7a439' : mark.includes('F') ? '#67a9c7' : '#78a790';
      svg.push(`<rect x="${x + 5}" y="${ry + 4}" width="${mark.length > 1 ? 23 : 16}" height="14" rx="3" fill="${color}"/><text x="${x + (mark.length > 1 ? 16.5 : 13)}" y="${ry + 14}" class="badge" text-anchor="middle">${mark}</text>`);
    } else svg.push(`<circle cx="${x + 13}" cy="${ry + 11}" r="2.6" fill="#b5c2cc"/>`);
    svg.push(`<text x="${x + 36}" y="${cy}" class="field">${esc(f.name)}</text>`);
    svg.push(`<text x="${x + 257}" y="${cy}" class="type">${esc(f.type)}</text>`);
  }
  const footerY = y + headerH + t.fields.length * rowH;
  svg.push(`<rect x="${x + 1}" y="${footerY}" width="${cardW - 2}" height="${footerH - 1}" fill="#e7ecef"/><text x="${x + 11}" y="${footerY + 14}" class="footer">Indexes</text><path d="M ${x + cardW - 18} ${footerY + 6} l 5 4 -5 4 z" fill="#7f919e"/></g>`);
}
svg.push(...endpointMarks);
svg.push(`<text x="70" y="${height - 24}" class="sub">Nguồn: SQL 01–05 của UniSchedule. Các kiểu BIGINT trong DBML là UNSIGNED trong SQL gốc; chi tiết CHECK / ON DELETE xem file DDL.</text>`);
svg.push('</svg>');
writeFileSync(output, svg.join('\n'), 'utf8');
console.log(JSON.stringify({ output, tables: tableMap.size, foreignKeys: refs.length, width, height }));
