#!/usr/bin/env python3
"""Use compiler modules even when the JDK executable launchers are absent."""
from pathlib import Path
import subprocess
ROOT = Path(__file__).resolve().parents[1]
out = ROOT / "build" / "core-checks"
out.mkdir(parents=True, exist_ok=True)
sources = sorted((ROOT / "src/main/java/dev/hbmmayo/core").glob("*.java"))
sources += sorted((ROOT / "src/test/java/dev/hbmmayo/core").glob("*.java"))
subprocess.run(["java", "-m", "jdk.compiler/com.sun.tools.javac.Main", "-source", "21", "-target", "21", "-d", str(out), *map(str, sources)], check=True)
subprocess.run(["java", "-cp", str(out), "dev.hbmmayo.core.CoreChecks"], check=True)
