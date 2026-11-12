#!/usr/bin/env python3
"""Test conservative attack-packet correlation using a minimal Java map helper stub.

Uses actual production PacketCorrelation.java and PacketObservation.java.
Requires Java 21 JDK; no Paper APIs needed for this pure test.
"""
from pathlib import Path
from tempfile import TemporaryDirectory
import subprocess

base = Path(__file__).resolve().parents[1]
source = base / 'src/main/java/org/errsa/combatdiag'
with TemporaryDirectory() as temp:
    folder = Path(temp)
    for file in ('PacketCorrelation.java', 'PacketObservation.java'):
        (folder / file).write_bytes((source / file).read_bytes())
    (folder / 'CombatDiagnosticsModule.java').write_text('''package org.errsa.combatdiag;
import java.util.*;
class CombatDiagnosticsModule {
  static Map<String, Object> map(Object... kv) {
    Map<String, Object> values = new LinkedHashMap<>();
    for (int i = 0; i < kv.length; i += 2) values.put((String) kv[i], kv[i + 1]);
    return values;
  }
}
''')
    (folder / 'CorrelationSmoke.java').write_text('''package org.errsa.combatdiag;
import java.util.*;
class CorrelationSmoke {
  private static PacketObservation p(long at, String target) {
    return new PacketObservation(at, at + 8, target, "victim", Map.of("x", 1), Map.of("x", 2));
  }
  static void expect(String actual, String expected) {
    if (!actual.equals(expected)) throw new AssertionError(actual + " != " + expected);
  }
  public static void main(String[] args) {
    expect(PacketCorrelation.evaluate(List.of(p(1000, "B")), "B", 1030, 400, 50).quality(),
        "packet-correlated");
    expect(PacketCorrelation.evaluate(List.of(p(1000, "A")), "B", 1030, 400, 50).quality(),
        "uncorrelated");
    expect(PacketCorrelation.evaluate(List.of(p(1035, "B")), "B", 1030, 400, 50).quality(),
        "timing-ambiguous");
    expect(PacketCorrelation.evaluate(List.of(p(1000, "B"), p(1010, "B")), "B", 1030,
        400, 50).quality(), "timing-ambiguous");
    expect(PacketCorrelation.evaluate(List.of(p(1000, null)), "B", 1030, 400, 50).quality(),
        "timing-ambiguous");
    expect(PacketCorrelation.evaluate(List.of(p(500, "B")), "B", 1030, 400, 50).quality(),
        "uncorrelated");
    if (!PacketCorrelation.packetFields(PacketCorrelation.evaluate(
          List.of(p(1000, "B")), "B", 1030, 400, 50))
          .containsKey("packet_attacker_position")) throw new AssertionError("packet position missing");
    System.out.println("PASS: exact, wrong-target, reordered, multiple, unresolved, stale, positions");
  }
}
''')
    subprocess.run(['javac', '-d', str(folder), *map(str, folder.glob('*.java'))], check=True)
    subprocess.run(['java', '-cp', str(folder), 'org.errsa.combatdiag.CorrelationSmoke'], check=True)
