#!/usr/bin/env python3
"""Offline build script for the Miku addon.

The project itself is a normal Loom project (see build.gradle); Loom 1.18.4 requires a JDK 25
runtime, which is not always available. This script produces the exact same artifact with plain
javac plus tiny-remapper, using the named Minecraft jar that Loom already cached:

  1. compile src/main/java against the named Minecraft jar + named Evernight jar
  2. remap the compiled classes from named to intermediary (the runtime namespace)
  3. package classes + resources into build/libs/evernight-miku-<version>.jar
"""
import glob
import os
import shutil
import subprocess
import sys

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
GRADLE = os.path.expanduser("~/.gradle/caches")
MC_DIR = os.path.join(GRADLE, "fabric-loom/minecraftMaven/net/minecraft")
OUT = os.path.join(ROOT, "build")
CLASSES = os.path.join(OUT, "classes")
REMAP = os.path.join(OUT, "remapped")
LIBS = os.path.join(ROOT, "libs")
def read_version():
    """Single source of truth for the addon version: gradle.properties."""
    path = os.path.join(ROOT, "gradle.properties")
    if os.path.isfile(path):
        for line in open(path, encoding="utf-8"):
            if line.startswith("mod_version="):
                return line.split("=", 1)[1].strip()
    raise SystemExit("mod_version is missing from gradle.properties")


VERSION = read_version()


def first(pattern):
    matches = sorted(glob.glob(pattern))
    if not matches:
        raise SystemExit("missing dependency: " + pattern)
    return matches[-1]


def module(pattern):
    matches = [m for m in glob.glob(pattern) if "sources" not in m]
    if not matches:
        raise SystemExit("missing dependency: " + pattern)
    return matches[0]


def main():
    mc_named = first(os.path.join(
        MC_DIR, "minecraft-merged/1.21.11-loom.mappings.1_21_11.layered+hash.2198-v2/*.jar"))
    evernight = os.path.join(ROOT, "libs/evernight-named.jar")
    if not os.path.exists(evernight):
        raise SystemExit("libs/evernight-named.jar missing")

    cp = [mc_named, evernight]
    cp.append(module(os.path.join(GRADLE, "modules-2/files-2.1/com.mojang/brigadier/*/*/brigadier-*.jar")))
    cp.append(module(os.path.join(GRADLE, "modules-2/files-2.1/com.google.code.gson/gson/2.13.2/*/gson-*.jar")))
    cp.append(module(os.path.join(GRADLE, "modules-2/files-2.1/org.slf4j/slf4j-api/2.0.20/*/slf4j-api-*.jar")))
    cp.append(module(os.path.join(GRADLE, "modules-2/files-2.1/net.fabricmc/fabric-loader/0.20.1/*/fabric-loader-*.jar")))
    cp.append(module(os.path.join(GRADLE, "modules-2/files-2.1/net.fabricmc/sponge-mixin/0.17.5+mixin.0.8.7/*/sponge-mixin-*.jar")))
    cp.append(module(os.path.join(GRADLE, "modules-2/files-2.1/org.jetbrains/annotations/*/*/annotations-*.jar")))
    for netty_module in ("netty-buffer", "netty-codec", "netty-common", "netty-handler",
                         "netty-resolver", "netty-transport", "netty-transport-native-unix-common"):
        matches = [m for m in glob.glob(os.path.join(
            GRADLE, "modules-2/files-2.1/io.netty/%s/4.2.19.Final/*/%s-*.jar" % (netty_module, netty_module)))
            if "sources" not in m]
        if matches:
            cp.append(matches[0])
    cp.append(module(os.path.join(GRADLE, "modules-2/files-2.1/com.mojang/datafixerupper/*/*/datafixerupper-*.jar")))

    sources = []
    for base, _dirs, files in os.walk(os.path.join(ROOT, "src/main/java")):
        sources += [os.path.join(base, f) for f in files if f.endswith(".java")]

    for path in (CLASSES, REMAP):
        shutil.rmtree(path, ignore_errors=True)
        os.makedirs(path)

    argfile = os.path.join(OUT, "javac.args")
    with open(argfile, "w", encoding="utf-8") as handle:
        handle.write("-cp\n" + os.pathsep.join(cp) + "\n")
        handle.write("-d\n" + CLASSES + "\n")
        handle.write("-encoding\nUTF-8\n")
        handle.write("--release\n21\n")
        handle.write("-proc:none\n")
        for source in sources:
            handle.write(source.replace("\\", "/") + "\n")

    print("compiling %d sources" % len(sources))
    result = subprocess.run(["javac", "@" + argfile], capture_output=True, text=True, encoding="gbk", errors="replace")
    if result.returncode != 0:
        print(result.stdout)
        print(result.stderr)
        raise SystemExit("compilation failed")
    if result.stderr.strip():
        print(result.stderr[-4000:])

    print("remapping named -> intermediary")
    remap_cp = [
        module(os.path.join(GRADLE, "modules-2/files-2.1/net.fabricmc/tiny-remapper/0.12.3/*/tiny-remapper-*.jar")),
        module(os.path.join(GRADLE, "modules-2/files-2.1/net.fabricmc/mapping-io/0.8.0/*/mapping-io-*.jar")),
        module(os.path.join(GRADLE, "modules-2/files-2.1/net.fabricmc/lorenz-tiny/4.0.2/*/lorenz-tiny-*.jar")),
        module(os.path.join(GRADLE, "modules-2/files-2.1/org.ow2.asm/asm/9.10.1/*/asm-*.jar")),
        module(os.path.join(GRADLE, "modules-2/files-2.1/org.ow2.asm/asm-tree/9.10.1/*/asm-tree-*.jar")),
        module(os.path.join(GRADLE, "modules-2/files-2.1/org.ow2.asm/asm-commons/9.10.1/*/asm-commons-*.jar")),
        module(os.path.join(GRADLE, "modules-2/files-2.1/org.ow2.asm/asm-util/9.10.1/*/asm-util-*.jar")),
        module(os.path.join(GRADLE, "modules-2/files-2.1/org.ow2.asm/asm-analysis/9.10.1/*/asm-analysis-*.jar")),
    ]
    # Remapping named -> intermediary needs the classpath in the *source* namespace (named),
    # otherwise tiny-remapper cannot resolve method owners and silently keeps named method names.
    mc_source = mc_named
    classes_jar = os.path.join(OUT, "classes.jar")
    subprocess.run(["jar", "cf", classes_jar, "-C", CLASSES, "."], check=True)
    result = subprocess.run(
        ["java", "-cp", os.pathsep.join(remap_cp), "net.fabricmc.tinyremapper.Main",
         classes_jar, os.path.join(REMAP, "classes.jar"),
         os.path.join(ROOT, "tools/named2int.tiny"), "named", "intermediary",
         mc_source, evernight],
        capture_output=True, text=True, encoding="gbk", errors="replace")
    if result.returncode != 0:
        print(result.stdout[-3000:])
        print(result.stderr[-3000:])
        raise SystemExit("remap failed")

    print("packaging")
    staging = os.path.join(OUT, "staging")
    shutil.rmtree(staging, ignore_errors=True)
    os.makedirs(staging)
    subprocess.run(["jar", "xf", os.path.join(REMAP, "classes.jar")], cwd=staging, check=True)
    for base, _dirs, files in os.walk(os.path.join(ROOT, "src/main/resources")):
        for name in files:
            source = os.path.join(base, name)
            rel = os.path.relpath(source, os.path.join(ROOT, "src/main/resources"))
            target = os.path.join(staging, rel)
            os.makedirs(os.path.dirname(target), exist_ok=True)
            shutil.copyfile(source, target)
            if rel == "fabric.mod.json":
                with open(target, encoding="utf-8") as handle:
                    text = handle.read().replace("${version}", VERSION)
                with open(target, "w", encoding="utf-8") as handle:
                    handle.write(text)

    os.makedirs(os.path.join(OUT, "libs"), exist_ok=True)
    jar_path = os.path.join(OUT, "libs", "evernight-miku-%s.jar" % VERSION)
    if os.path.exists(jar_path):
        os.remove(jar_path)
    subprocess.run(["jar", "cf", jar_path, "-C", staging, "."], check=True)
    print("built " + jar_path)


if __name__ == "__main__":
    sys.exit(main())
