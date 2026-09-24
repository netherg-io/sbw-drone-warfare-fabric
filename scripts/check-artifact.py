"""Check the distribution boundary: never package unported Forge code/assets."""
import json
from pathlib import Path
from zipfile import ZipFile

jars = [p for p in Path("build/libs").glob("*.jar") if not p.name.endswith("-sources.jar")]
assert len(jars) == 1, jars
with ZipFile(jars[0]) as jar:
    names = jar.namelist()
    meta = json.loads(jar.read("fabric.mod.json"))
    assert meta["license"] == "AGPL-3.0-only"
    assert "${" not in meta["version"]
    assert {"LICENSE", "NOTICE", "CREDITS.md"}.issubset(names)
    assert not any(n.startswith("META-INF/mods") or n.endswith((".png", ".ogg", ".obj", ".mtl")) for n in names)
    for name in names:
        if name.endswith(".class"):
            data = jar.read(name)
            assert b"net/minecraftforge/" not in data, name
            assert b"net/neoforged/" not in data, name
            assert int.from_bytes(data[6:8], "big") == 65, name
    assert "nl/smartstreamlabs/sbwdroneconfig/FpvDrone.class" in names
print("Fabric artifact boundary OK:", jars[0])
