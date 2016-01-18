package ella2.util;

public class PrintUtil {
    public static String toString(int[] array) {
        StringBuilder sb = new StringBuilder();
        sb.append("[ ");
        for (int i = 0; i < array.length; i++) {
            if (i != 0) {
                sb.append(",");
            }

            sb.append(" " + array[i]);
        }
        sb.append(" ]");
        return sb.toString();
    }
}
