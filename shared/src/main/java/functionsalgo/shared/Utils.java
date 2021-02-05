package functionsalgo.shared;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Properties;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import functionsalgo.exceptions.StandardJavaException;

public class Utils {

    private static final Logger logger = LogManager.getLogger();
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

    public static void sleep(long timeMillis) {
        try {
            Thread.sleep(timeMillis);
        } catch (InterruptedException e) {
            logger.error("couldn't sleep", e);
        }
    }

    /**
     * example: Properties props = getProperties("fileA.properties",
     * "path/to/fileB.properties", "FileC.properties"); if fileA exists in the
     * resources/ directory it will be the one to be loaded, else fileB will be
     * loaded and if fileB doesn't exist then fileC will be loaded. If none exist an
     * exception will be thrown (FileNotFoundException).
     * 
     * @param propertiesFilesNamesByOrderToBeLoaded
     * @return a Properties instance loaded from the first available file name/path
     *         passed as parameter in the resources directory.
     * @throws StandardJavaException
     * @throws IOException            (FileNotFoundException) if no file was found
     * @throws ClassNotFoundException
     */
    public static Properties getProperties(String... propertiesFilesNamesByOrderToBeLoaded)
            throws StandardJavaException {

        try {
            ClassLoader callerCL = Class.forName(new Exception().getStackTrace()[1].getClassName()).getClassLoader();
            InputStream keysFile = null;

            for (String propFileName : propertiesFilesNamesByOrderToBeLoaded) {
                if (propFileName != null) {
                    keysFile = callerCL.getResourceAsStream(propFileName);
                    if (keysFile != null) {
                        break;
                    }
                }
            }

            if (keysFile == null) {
                throw new FileNotFoundException("resources/"
                        + propertiesFilesNamesByOrderToBeLoaded[propertiesFilesNamesByOrderToBeLoaded.length - 1]);
            }

            Properties keys = new Properties();
            keys.load(keysFile);
            return keys;
        } catch (IOException | ClassNotFoundException e) {
            throw new StandardJavaException(e);
        }
    }
}
