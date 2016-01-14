package hermit.util;

public class RubbleUtil {
    private static final int[] minAmount = {100, 116, 132, 150, 168, 188, 208, 230, 252, 276, 301, 327, 355,
            384, 415, 448, 482, 517, 555, 595, 637, 681, 727, 776, 827, 882, 938, 998,
            1061, 1128, 1198, 1271, 1349, 1430, 1516, 1606, 1701, 1801, 1907, 2018, 2134, 2257, 2387,
            2523, 2666, 2817, 2976, 3143, 3319, 3504, 3699, 3904, 4120, 4347, 4587, 4839,
            5104, 5383, 5677, 5986, 6312, 6655, 7015, 7395, 7795, 8216, 8659, 9125, 9616,
            10132, 10676, 11248 };

    public static final int TOO_MANY_ROUNDS = Integer.MAX_VALUE;

    public static int getRoundsToMakeMovable(int rubble) {
        int low = 0;
        int high = minAmount.length - 1;

        if (rubble > minAmount[high]) {
            return TOO_MANY_ROUNDS;
        }
        else if (rubble < minAmount[0]) {
            return 0;
        }

        while (low < high) {
            int mid = (low + high) / 2;
            if (minAmount[mid] <= rubble
                    && minAmount[mid + 1] > rubble) {
                return mid + 1;
            }
            else if (minAmount[mid] < rubble) {
                low = mid + 1;
            }
            else {
                high = mid - 1;
            }
        }

        return low + 1;
    }
}
