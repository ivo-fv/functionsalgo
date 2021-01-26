package functionsalgo.shared;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;

public class Utils {

    private static final byte[] HEX_ARRAY = { 48, 49, 50, 51, 52, 53, 54, 55, 56, 57, 65, 66, 67, 68, 69, 70 };

    public static String bytesToHex(byte[] bytes) {
        byte[] hexChars = new byte[bytes.length * 2];
        for (int j = 0; j < bytes.length; j++) {
            int v = bytes[j] & 0xFF;
            hexChars[j * 2] = HEX_ARRAY[v >>> 4];
            hexChars[j * 2 + 1] = HEX_ARRAY[v & 0x0F];
        }
        return new String(hexChars, StandardCharsets.UTF_8);
    }

    public static void sleep(long timeMillis, Logger logger) {

        try {
            Thread.sleep(timeMillis);

        } catch (InterruptedException e) {

            if (logger != null) {

                logger.log(2, -1, e.toString(),
                        Arrays.toString(e.getStackTrace()) + " ; " + Arrays.toString(e.getStackTrace()));
            }
            
        }
        
    }
}
