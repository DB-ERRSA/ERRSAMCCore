#!/usr/bin/env python3
"""Safe offline plugin-folder migration. Stop server and back up plugins/ before --apply."""
import argparse
from pathlib import Path
import shutil

SOURCES = {
    'InventoryAudit': 'inventory-audit',
    'ERRSA-CombatDiagnostics': 'combat-diagnostics',
    'AutoUnlockPvPDrops': 'auto-unlock-pvp-drops',
    'ERRSACompatibilityPaper': 'compatibility-paper',
    'ERRSAWebBridge': 'web-bridge',
    'ERRSAWikiSync': 'wiki-sync',
    'ShulkerReroute': 'end-entity-reroute',
}

def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--plugins-dir', required=True, type=Path)
    parser.add_argument('--apply', action='store_true', help='Copy; default is preview only')
    args = parser.parse_args()
    plugins = args.plugins_dir.resolve()
    if not plugins.is_dir(): parser.error(f'Not a directory: {plugins}')
    for source, dest in SOURCES.items():
        old = plugins / source
        new = plugins / 'ERRSA-MC-Core' / dest
        if not old.is_dir():
            print(f'SKIP {old}: directory absent')
            continue
        for p in old.rglob('*'):
            if not p.is_file(): continue
            rel = p.relative_to(old)
            dst = new / rel
            if dst.exists():
                print(f'KEEP existing {dst}')
                continue
            print(f'{"COPY" if args.apply else "WOULD COPY"} {p} -> {dst}')
            if args.apply:
                dst.parent.mkdir(parents=True, exist_ok=True)
                shutil.copy2(p, dst)
    print('Done. Original plugin files left untouched. WebBridge data is migrated; enable Core module after removing standalone JAR and testing.')
    if not args.apply: print('Preview only. Stop Paper and back up plugins/, then add --apply.')

if __name__ == '__main__': main()
