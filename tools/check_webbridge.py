#!/usr/bin/env python3
"""Run WebBridge Java-only format checks without downloading Paper APIs."""
from pathlib import Path
import subprocess
import tempfile
import sys

root = Path(__file__).resolve().parents[1]
java = root / 'src/main/java/com/errsa/webbridge'
smoke = root / 'tools/webbridge-smoke'
sources = [
    java / 'snapshot/SnapshotJson.java',
    java / 'eventbridge/EventBridgeFields.java',
    java / 'eventbridge/EventBridgeSnapshotWriter.java',
    java / 'quickshop/QuickShopReflection.java',
    java / 'quickshop/QuickShopStockReader.java',
    java / 'quickshop/QuickShopStockRow.java',
    java / 'quickshop/CachedStock.java',
    java / 'quickshop/QuickShopCacheStore.java',
    java / 'quickshop/QuickShopSnapshotWriter.java',
    smoke / 'QuickShopSmoke.java',
    smoke / 'EventBridgeSmoke.java',
]
with tempfile.TemporaryDirectory(prefix='errsa-webbridge-') as output:
    commands = [
        ['javac', '--release', '21', '-d', output, *map(str, sources)],
        ['java', '-cp', output, 'com.errsa.webbridge.quickshop.QuickShopSmoke'],
        ['java', '-cp', output, 'com.errsa.webbridge.eventbridge.EventBridgeSmoke'],
    ]
    for command in commands:
        result = subprocess.run(command, check=False)
        if result.returncode != 0:
            sys.exit(result.returncode)
print('PASS WebBridge Java-only regression checks')
