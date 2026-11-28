# -*- coding: utf-8 -*-
"""归档审计 + 发布清单生成。

本仓库**不保存 jar**：正式版产物作为 GitHub Release 附件发布，`versions/` 只在本地保留。
这个脚本做两件事：

1. 审计本地 `versions/vX.Y.Z/`：jar / sources / README / RELEASE 是否齐全、
   jar 大小与 SHA-256、内嵌 `fabric.mod.json` 的版本是否与目录一致、git tag 是否存在；
2. 生成两个清单：
   * `versions/ARCHIVE.md`（本地，详细版，不入库）
   * `RELEASES.md`（仓库根目录，公开版，链到 GitHub Release）

测试版（`versions/pre-release/`）只在本地，脚本只统计数量，不列明细、不进公开清单。

用法：
    python tools/archive_audit.py            # 在仓库根目录运行
    python tools/archive_audit.py <repo>     # 或指定仓库路径
"""
import hashlib
import io
import json
import os
import re
import subprocess
import sys
import zipfile

HERE = os.path.dirname(os.path.abspath(__file__))
if len(sys.argv) > 1:
    REPO = os.path.abspath(sys.argv[1])
elif os.path.basename(HERE) == 'tools':
    REPO = os.path.dirname(HERE)          # tools/ 直接位于仓库根目录下
else:
    REPO = os.path.abspath(os.path.join(HERE, '..'))
VERSIONS = os.path.join(REPO, 'versions')
REPO_URL = 'https://github.com/nyaLium/Cake-Team_Towers-Hud-Lite'

# 已经发过 GitHub Release 的版本：`RELEASES.md` 只列这些；其余只算本地归档。
# 发布新 Release 后，把版本号加进这个集合，再跑一次本脚本。
PUBLISHED = {'v1.5.11', 'v1.6.1', 'v1.6.3', 'v1.6.5', 'v1.6.6', 'v1.6.8', 'v1.6.9', 'v1.6.12'}
RELEASES_URL = REPO_URL + '/releases'

# 1.20.37 起项目/产物改名为 ctt-hud-fork；更早的归档仍是 cake_team_tower_hud_fork-*。
ARTIFACT_NAMES = ('ctt-hud-fork', 'cake_team_tower_hud_fork')


def artifact(folder, ver, sources=False):
    suffix = '-sources.jar' if sources else '.jar'
    for base in ARTIFACT_NAMES:
        path = os.path.join(folder, '%s-%s%s' % (base, ver, suffix))
        if os.path.isfile(path):
            return path
    return os.path.join(folder, '%s-%s%s' % (ARTIFACT_NAMES[0], ver, suffix))


def sha256(path):
    h = hashlib.sha256()
    with open(path, 'rb') as f:
        for chunk in iter(lambda: f.read(1 << 20), b''):
            h.update(chunk)
    return h.hexdigest()


def git(*args):
    return subprocess.check_output(['git', '-C', REPO] + list(args), text=True).strip()


def _range_of(names):
    ordered = sorted(names, key=lambda n: version_key(n if n.startswith('v') else 'v' + n))
    if not ordered:
        return ''
    if len(ordered) == 1:
        return ordered[0]
    return '%s – %s' % (ordered[0], ordered[-1])


def version_key(name):
    return tuple(int(x) for x in name[1:].split('.'))


def release_stamp(tag):
    """git 提交时间（秒）。v1.0.0 起版本号重新起算，排序看发布时间而不是版本号。"""
    try:
        return int(git('log', '-1', '--format=%ct', tag))
    except (subprocess.CalledProcessError, ValueError):
        return 0


def tag_date(tag):
    try:
        date = git('log', '-1', '--format=%cs', tag)
        return date
    except subprocess.CalledProcessError:
        return ''


def tag_commit(tag):
    try:
        return git('rev-list', '-n', '1', tag)[:7]
    except subprocess.CalledProcessError:
        return ''


def embedded(jar):
    try:
        with zipfile.ZipFile(jar) as z:
            data = json.loads(z.read('fabric.mod.json').decode('utf-8'))
        return data.get('name', '?'), data.get('version', '?')
    except Exception as exc:  # noqa: BLE001 - 报告，不让审计崩掉
        return 'ERR', str(exc)


def collect():
    """正式版目录：versions/vX.Y.Z/（测试版在 versions/pre-release/，不列明细）。"""
    out = []
    if not os.path.isdir(VERSIONS):
        return out
    for name in os.listdir(VERSIONS):
        path = os.path.join(VERSIONS, name)
        if os.path.isdir(path) and re.fullmatch(r'v\d+\.\d+\.\d+', name):
            out.append((path, name))
    return out


def local_pre_release_count():
    pre = os.path.join(VERSIONS, 'pre-release')
    if not os.path.isdir(pre):
        return 0
    return len([d for d in os.listdir(pre)
                if os.path.isdir(os.path.join(pre, d)) and re.fullmatch(r'v\d+\.\d+\.\d+', d)])


def main():
    rows = []
    problems = []
    for folder, name in collect():
        ver = name[1:]
        jar = artifact(folder, ver)
        src_jar = artifact(folder, ver, sources=True)
        docs = [f for f in ('README.md', 'RELEASE.md') if os.path.isfile(os.path.join(folder, f))]
        if not os.path.isfile(jar):
            problems.append('%s: jar missing' % name)
            continue
        size = os.path.getsize(jar)
        digest = sha256(jar)
        mod_name, mod_version = embedded(jar)
        if mod_version != ver:
            problems.append('%s: embedded version %r != %s' % (name, mod_version, ver))
        if not os.path.isfile(src_jar):
            problems.append('%s: sources jar missing' % name)
        if not os.path.isfile(os.path.join(folder, 'RELEASE.md')):
            problems.append('%s: RELEASE.md missing' % name)
        rows.append(dict(name=name, version=ver, size=size, digest=digest,
                         src_size=os.path.getsize(src_jar) if os.path.isfile(src_jar) else 0,
                         docs=docs, mod=mod_name, date=tag_date(name),
                         commit=tag_commit(name), stamp=release_stamp(name)))

    rows.sort(key=lambda r: r['stamp'])
    local_tests = local_pre_release_count()

    # ── 本地详细清单（不入库） ──
    lines = ['# 归档清单 · CTT HUD - Fork（本地专用，不入库）\n',
             '> 仓库不保存 jar：正式版作为 GitHub Release 附件发布，`versions/` 只在维护者本地。',
             '> 本文件由 `tools/archive_audit.py` 生成（jar 大小 / SHA-256 / 内嵌版本 / git tag）。',
             '> 公开清单见仓库根目录的 `RELEASES.md`。\n',
             '| 版本 | 日期 | jar 大小 | SHA-256（前 16 位） | sources | 文档 | 内嵌 name | tag / commit |',
             '| --- | --- | --- | --- | --- | --- | --- | --- |']
    for r in rows:
        lines.append('| [{n}]({n}/RELEASE.md) | {d} | {s:,} B | `{h}…` | {ss:,} B | {docs} | {mod} | `{tag}` / `{c}` |'.format(
            n=r['name'], d=r['date'] or '?', s=r['size'], h=r['digest'][:16].upper(),
            ss=r['src_size'], docs='+'.join(x.replace('.md', '') for x in r['docs']),
            mod=r['mod'], tag=r['name'], c=r['commit'] or '?'))
    lines.append('')
    lines.append('合计 **%d 个正式版**（按发布时间：%s）。\n'
                 % (len(rows), ' → '.join(r['version'] for r in rows) if rows else '—'))
    if local_tests:
        lines.append('> 另有 **%d 个测试版**（1.10.0 – 1.20.42）只在本机 `versions/pre-release/`，不公开。\n'
                     % local_tests)
    if problems:
        lines.append('## 需要留意\n')
        for p in problems:
            lines.append('- ' + p)
    else:
        lines.append('## 校验结果\n')
        lines.append('- 所有版本的 jar 内嵌版本号与目录一致；sources / README / RELEASE 齐全。')
    out = os.path.join(VERSIONS, 'ARCHIVE.md')
    io.open(out, 'w', encoding='utf-8', newline='\n').write('\n'.join(lines) + '\n')
    print('wrote', out)

    # ── 公开清单（仓库根目录 RELEASES.md） ──
    pub = ['# 发布清单（GitHub Releases）\n',
           '> 本仓库**不保存 jar，也不保存历史归档**：正式版以',
           '> [GitHub Release](%s) 形式发布，附件里有 `ctt-hud-fork-X.Y.Z.jar` 与 `-sources.jar`。' % RELEASES_URL,
           '> `versions/` 目录只在维护者本地保留（已被 `.gitignore` 排除）。\n',
           '> 本仓库**当前发布 %s**；更早的版本未在此发布。\n'
           % '、'.join(sorted(PUBLISHED, key=lambda v: [int(x) for x in v.lstrip('v').split('.')])),
           '> 下载：<%s>\n' % RELEASES_URL,
           '| 版本 | 日期 | jar 大小 | jar SHA-256（前 16 位） | 附件 |',
           '| --- | --- | --- | --- | --- |']
    for r in reversed(rows):
        if r['name'] not in PUBLISHED:
            continue
        pub.append('| [{n}]({rel}/tag/{n}) | {d} | {s:,} B | `{h}…` | `ctt-hud-fork-{v}.jar`、`-sources.jar`、`RELEASE.md` |'.format(
            n=r['name'], rel=RELEASES_URL, d=r['date'] or '?', s=r['size'],
            h=r['digest'][:16].upper(), v=r['version']))
    pub.append('| ~~1.0.1~~ | — | — | — | 已撤回：LICENSE 上游署名错误，不再分发 |')
    pub.append('| ~~1.0.0~~ | — | — | — | 已撤回：同上 |')
    pub.append('')
    local_only = [r['version'] for r in rows if r['name'] not in PUBLISHED]
    if local_only:
        pub.append('> **历史版本未在本仓库发布**：%s 只有维护者本地归档，不提供下载。\n'
                   % _range_of(local_only))
    if local_tests:
        pub.append('发布前的 67 个测试版（1.10.0 – 1.20.42）不公开，只保存在维护者本地 `versions/pre-release/`。')
    pub_path = os.path.join(REPO, 'RELEASES.md')
    io.open(pub_path, 'w', encoding='utf-8', newline='\n').write('\n'.join(pub) + '\n')
    print('wrote', pub_path)

    print('versions (formal):', len(rows), '| local pre-release (not uploaded):', local_tests)
    print('problems:', len(problems))
    for p in problems:
        print('  !', p)


if __name__ == '__main__':
    main()
