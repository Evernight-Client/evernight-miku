import java.io.*;
import java.net.*;
import java.util.*;
import java.util.jar.*;

public class CheckJar {
    public static void main(String[] args) throws Exception {
        List<URL> urls = new ArrayList<>();
        for (int i = 0; i < args.length; i++) {
            urls.add(new File(args[i]).toURI().toURL());
        }
        URLClassLoader loader = new URLClassLoader(urls.toArray(new URL[0]), ClassLoader.getPlatformClassLoader());
        int ok = 0;
        int fail = 0;
        try (JarFile jar = new JarFile(args[0])) {
            for (Enumeration<JarEntry> e = jar.entries(); e.hasMoreElements(); ) {
                JarEntry entry = e.nextElement();
                String name = entry.getName();
                if (!name.endsWith(".class") || name.startsWith("META-INF")) {
                    continue;
                }
                String cls = name.substring(0, name.length() - 6).replace('/', '.');
                try {
                    Class<?> c = Class.forName(cls, false, loader);
                    c.getDeclaredMethods();
                    c.getDeclaredFields();
                    ok++;
                } catch (Throwable t) {
                    fail++;
                    System.out.println("FAIL " + cls + " -> " + t);
                }
            }
        }
        System.out.println("ok=" + ok + " fail=" + fail);
    }
}
