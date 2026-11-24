import java.lang.reflect.Method;

public class BitTest {
    public static void main(String[] args) throws Exception {
        Class<?> clazz = Class.forName("com.github.mikumiku.evernight.modules.PrinterModule");
        Method readBits = clazz.getDeclaredMethod("readBits", long[].class, int.class, int.class);
        readBits.setAccessible(true);
        long[] data = {3679553186613532803L, -8993399962691430008L, -8632538029916143208L};
        int[] expected = {3, 0, 2, 4, 1, 3, 0, 2, 4, 1, 3, 0, 2, 4, 1, 3, 0, 2, 4, 1, 3, 0, 2, 4, 1, 3, 0, 2, 4, 1, 3, 0, 2, 4, 1, 3, 0, 2, 4, 1, 3, 0, 2, 4, 1, 3, 0, 2, 4, 1, 3, 0, 2, 4, 1, 3, 0, 2, 4, 1, 3, 0, 2, 4};
        int bad = 0;
        for (int i = 0; i < expected.length; i++) {
            int got = (int) readBits.invoke(null, data, 3, i);
            if (got != expected[i]) {
                bad++;
                System.out.println("mismatch at " + i + ": got " + got + " want " + expected[i]);
            }
        }
        System.out.println(bad == 0 ? "OK: bit unpacking matches" : "FAIL: " + bad + " mismatches");
    }
}
