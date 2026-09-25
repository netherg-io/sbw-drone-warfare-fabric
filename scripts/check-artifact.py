"""Check the distribution boundary: never package unported Forge code/assets."""
import json
from pathlib import Path
from zipfile import ZipFile

# Our own CC BY 4.0 icons (tools/item_icons.py; see CREDITS.md). Every other
# .png/.ogg/.obj/.mtl stays out of the jar: unaudited or third-party assets
# referenced by the installed Superb Warfare jar, not redistributed here.
ALLOWED_BINARY_ASSETS = {
    "assets/sbwdroneconfig/icon.png",
    "assets/sbwdroneconfig/textures/item/cubed_fpv_drone.png",
    "assets/sbwdroneconfig/textures/item/fibre_fpv_drone.png",
    "assets/sbwdroneconfig/textures/item/signal_jammer.png",
}

jars = [p for p in Path("build/libs").glob("*.jar") if not p.name.endswith("-sources.jar")]
assert len(jars) == 1, jars
with ZipFile(jars[0]) as jar:
    names = jar.namelist()
    meta = json.loads(jar.read("fabric.mod.json"))
    assert meta["license"] == "AGPL-3.0-only"
    assert "${" not in meta["version"]
    assert {"LICENSE", "NOTICE", "CREDITS.md"}.issubset(names)
    assert not any(
        n.startswith("META-INF/mods")
        or (n.endswith((".png", ".ogg", ".obj", ".mtl")) and n not in ALLOWED_BINARY_ASSETS)
        for n in names
    )
    assert ALLOWED_BINARY_ASSETS.issubset(names), ALLOWED_BINARY_ASSETS - set(names)
    for name in names:
        if name.endswith(".class"):
            data = jar.read(name)
            assert b"net/minecraftforge/" not in data, name
            assert b"net/neoforged/" not in data, name
            assert int.from_bytes(data[6:8], "big") == 65, name
    assert "nl/smartstreamlabs/sbwdroneconfig/FpvDrone.class" in names
print("Fabric artifact boundary OK:", jars[0])
