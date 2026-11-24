import org.objectweb.asm.*;
import org.objectweb.asm.tree.*;

import java.io.*;
import java.util.*;
import java.util.jar.*;

/**
 * Verifies that every reference from the addon jar into Minecraft resolves in the target
 * (intermediary) Minecraft jar. Catches unremapped field/method names, which otherwise only
 * surface as NoSuchMethodError/NoSuchFieldError in game.
 *
 * Usage: java VerifyRemap <addon.jar> <minecraft.jar>
 */
public class VerifyRemap {
    private static final Map<String, ClassNode> CACHE = new HashMap<>();
    private static JarFile mcJar;

    public static void main(String[] args) throws Exception {
        mcJar = new JarFile(args[1]);
        int checked = 0;
        int missing = 0;
        try (JarFile addon = new JarFile(args[0])) {
            for (Enumeration<JarEntry> e = addon.entries(); e.hasMoreElements(); ) {
                JarEntry entry = e.nextElement();
                if (!entry.getName().endsWith(".class")) {
                    continue;
                }
                ClassNode node = new ClassNode();
                new ClassReader(addon.getInputStream(entry)).accept(node, 0);
                for (MethodNode method : node.methods) {
                    for (AbstractInsnNode insn : method.instructions) {
                        if (insn instanceof MethodInsnNode call) {
                            if (!call.owner.startsWith("net/minecraft/")) {
                                continue;
                            }
                            checked++;
                            if (!hasMethod(call.owner, call.name, call.desc)) {
                                missing++;
                                System.out.println("MISSING METHOD " + node.name + "." + method.name
                                        + " -> " + call.owner + "." + call.name + call.desc);
                            }
                        } else if (insn instanceof FieldInsnNode field) {
                            if (!field.owner.startsWith("net/minecraft/")) {
                                continue;
                            }
                            checked++;
                            if (!hasField(field.owner, field.name, field.desc)) {
                                missing++;
                                System.out.println("MISSING FIELD " + node.name + "." + method.name
                                        + " -> " + field.owner + "." + field.name + " " + field.desc);
                            }
                        }
                    }
                }
            }
        }
        System.out.println("checked=" + checked + " missing=" + missing);
        if (missing > 0) {
            System.exit(1);
        }
    }

    private static ClassNode load(String internalName) {
        return CACHE.computeIfAbsent(internalName, name -> {
            JarEntry entry = mcJar.getJarEntry(name + ".class");
            if (entry == null) {
                return null;
            }
            try (InputStream in = mcJar.getInputStream(entry)) {
                ClassNode node = new ClassNode();
                new ClassReader(in).accept(node, 0);
                return node;
            } catch (IOException exception) {
                return null;
            }
        });
    }

    private static boolean hasMethod(String owner, String name, String desc) {
        return hasMethod(owner, name, desc, new HashSet<>());
    }

    private static boolean hasMethod(String owner, String name, String desc, Set<String> visited) {
        if (owner == null || !visited.add(owner)) {
            return false;
        }
        ClassNode node = load(owner);
        if (node == null) {
            return false;
        }
        for (MethodNode method : node.methods) {
            if (method.name.equals(name) && method.desc.equals(desc)) {
                return true;
            }
        }
        if (hasMethod(node.superName, name, desc, visited)) {
            return true;
        }
        for (String itf : node.interfaces) {
            if (hasMethod(itf, name, desc, visited)) {
                return true;
            }
        }
        return false;
    }

    private static boolean hasField(String owner, String name, String desc) {
        return hasField(owner, name, desc, new HashSet<>());
    }

    private static boolean hasField(String owner, String name, String desc, Set<String> visited) {
        if (owner == null || !visited.add(owner)) {
            return false;
        }
        ClassNode node = load(owner);
        if (node == null) {
            return false;
        }
        for (FieldNode field : node.fields) {
            if (field.name.equals(name) && field.desc.equals(desc)) {
                return true;
            }
        }
        if (hasField(node.superName, name, desc, visited)) {
            return true;
        }
        for (String itf : node.interfaces) {
            if (hasField(itf, name, desc, visited)) {
                return true;
            }
        }
        return false;
    }
}
