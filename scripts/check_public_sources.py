"""Check public worktree and every commit; report paths, never matched secrets."""
from pathlib import Path
import re, subprocess, sys

ROOT = Path(__file__).resolve().parents[1]
ADS = {
    b'ca-app-pub-3940256099942544~3347511713',
    b'ca-app-pub-3940256099942544/6300978111',
    b'ca-app-pub-3940256099942544/1033173712',
}
SECRET = re.compile(rb'(?:gh[pousr]_[A-Za-z0-9]{20,}|github_pat_[A-Za-z0-9_]{30,}|AIza[A-Za-z0-9_-]{30,}|-----BEGIN (?:RSA |EC |OPENSSH )?PRIVATE KEY-----)')
FORBIDDEN = re.compile(r'(?:^|/)(?:local\.properties|secrets\.properties|admob\.properties|keystore\.properties|google-services\.json|service-account[^/]*\.json|\.env|tenthappkey01)$|\.(?:jks|keystore|p12|pem|key|apk|aab|db|sqlite)$', re.I)

def git(*args):
    return subprocess.check_output(['git', '-C', str(ROOT), *args])

def inspect(label, name, content):
    issues=[]
    if FORBIDDEN.search(name): issues.append(f'{label}: forbidden file {name}')
    if any(v not in ADS for v in re.findall(rb'ca-app-pub-\d+[~/]\d+', content)):
        issues.append(f'{label}: non-test advertising ID in {name}')
    if SECRET.search(content): issues.append(f'{label}: credential pattern in {name}')
    return issues

def main():
    issues=[]
    count=0
    for p in ROOT.rglob('*'):
        if not p.is_file() or any(x in {'.git','.gradle','build','.idea'} for x in p.relative_to(ROOT).parts): continue
        issues.extend(inspect('worktree',p.relative_to(ROOT).as_posix(),p.read_bytes()))
        count+=1
    commits=git('rev-list','--all').decode().splitlines()
    seen=set()
    for commit in commits:
        for entry in git('ls-tree','-rz','--full-tree',commit).split(b'\0'):
            if not entry: continue
            meta,path=entry.split(b'\t',1)
            sha=meta.split()[2].decode()
            name=path.decode()
            if (sha,name) in seen: continue
            seen.add((sha,name))
            issues.extend(inspect('history',name,git('cat-file','blob',sha)))
    if issues:
        print('\n'.join(issues)); return 1
    print(f'PASS: {count} worktree files; {len(commits)} commits; {len(seen)} historical file versions checked.')
    return 0

if __name__ == '__main__': sys.exit(main())
