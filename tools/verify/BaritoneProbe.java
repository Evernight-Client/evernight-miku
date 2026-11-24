import java.io.File;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.List;

/** Checks that reflecting over BaritoneModule fields no longer requires Baritone on the classpath. */
public class BaritoneProbe {
    public static void main(String[] args) throws Exception {
        List<URL> urls = new ArrayList<>();
        for (String arg : args) {
            urls.add(new File(arg).toURI().toURL());
        }
        URLClassLoader loader = new URLClassLoader(urls.toArray(new URL[0]), ClassLoader.getPlatformClassLoader());
        Class<?> module = Class.forName("evernight.client.modules.client.Baritone.BaritoneModule", false, loader);
        try {
            module.getDeclaredFields();
            System.out.println("OK: field reflection works without Baritone");
        } catch (Throwable throwable) {
            System.out.println("FAIL: " + throwable);
        }
        try {
            Class.forName("baritone.api.BaritoneAPI", false, loader);
            System.out.println("NOTE: baritone is on the classpath");
        } catch (Throwable ignored) {
            System.out.println("NOTE: baritone is absent (as intended)");
        }
    }
}
