package functionsalgo.shared;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.io.OutputStream;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Properties;

import javax.crypto.Cipher;
import javax.crypto.CipherInputStream;
import javax.crypto.CipherOutputStream;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import functionsalgo.exceptions.StandardJavaException;

public class Utils {

    private static final Logger logger = LogManager.getLogger();
    private static final byte[] HEX_ARRAY = { 48, 49, 50, 51, 52, 53, 54, 55, 56, 57, 65, 66, 67, 68, 69, 70 };

    private static final SecureRandom secRnd = new SecureRandom();
    private static final int GCM_IV_LEN_BYTES = 12; // TODO try 16bytes
    private static final int GCM_TLEN_BITS = 128;

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
     * example: URL props = getFileOrResource("fileA.properties",
     * "path/to/fileB.properties", "FileC.properties"); if fileA exists in the
     * resources/ directory it will be the one to be returned, else fileB will be
     * returned and if fileB doesn't exist then fileC will be returned. If none
     * exist an exception will be thrown (FileNotFoundException).
     * 
     * @param filesNamesByOrder
     * @return URL of the first file name/path that points to a valid file
     * @throws FileNotFoundException if no file was found
     */
    public static URL getFileOrResource(String... filesNamesOrPathByOrder) throws FileNotFoundException {
        ClassLoader callerCL;
        try {
            callerCL = Class.forName(new Exception().getStackTrace()[1].getClassName()).getClassLoader();
        } catch (ClassNotFoundException e) {
            throw new FileNotFoundException(e.toString());
        }
        URL url = null;
        for (String file : filesNamesOrPathByOrder) {
            try {
                File f = new File(file);
                if (f.exists()) {
                    url = f.toURI().toURL();
                } else {
                    throw new Exception();
                }
            } catch (Exception e1) {
                try {
                    url = callerCL.getResource(file);
                } catch (Exception e2) {
                    // continue
                }
            }
            if (url != null) {
                break;
            }
        }
        if (url == null) {
            throw new FileNotFoundException("no file with any of the file names/paths was found: "
                    + String.join(" , ", filesNamesOrPathByOrder));
        }
        return url;
    }

    public static Properties loadEncryptedProperties(URL encryptedProps, URL keyToEncryptedProps)
            throws IOException, StandardJavaException {
        try (BufferedReader br = new BufferedReader(
                new InputStreamReader(keyToEncryptedProps.openStream(), StandardCharsets.UTF_8))) {
            return loadEncryptedProperties(encryptedProps, br.readLine());
        }
    }

    public static Properties loadEncryptedProperties(URL encryptedProps, String keyToEncryptedProps)
            throws IOException, StandardJavaException {
        Properties props = new Properties();
        try (CipherInputStream cis = decryptAES256GCMNoPadding(encryptedProps.openStream(),
                keyToEncryptedProps.getBytes(StandardCharsets.UTF_8))) {
            props.load(cis);
        }
        return props;
    }

    public static void saveAndEncryptProperties(Properties propsToEncrypt, URL encryptedPropsDestination,
            URL keyToEncryptProps) throws IOException, StandardJavaException {
        try (BufferedReader br = new BufferedReader(
                new InputStreamReader(keyToEncryptProps.openStream(), StandardCharsets.UTF_8))) {
            saveAndEncryptProperties(propsToEncrypt, encryptedPropsDestination, br.readLine());
        }
    }

    public static void saveAndEncryptProperties(Properties propsToEncrypt, URL encryptedPropsDestination,
            String keyToEncryptProps) throws StandardJavaException {
        try (CipherOutputStream cos = encryptAES256GCMNoPadding(
                new FileOutputStream(new File(encryptedPropsDestination.toURI())),
                keyToEncryptProps.getBytes(StandardCharsets.UTF_8))) {
            propsToEncrypt.store(cos, "");
        } catch (IOException | URISyntaxException e) {
            throw new StandardJavaException(e);
        }
    }

    public static CipherOutputStream encryptAES256GCMNoPadding(OutputStream out, byte[] passKey)
            throws StandardJavaException {
        try {
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            SecretKey key256 = new SecretKeySpec(MessageDigest.getInstance("SHA-256").digest(passKey), "AES");
            byte[] initVector = new byte[GCM_IV_LEN_BYTES];
            secRnd.nextBytes(initVector);
            GCMParameterSpec ivSpec = new GCMParameterSpec(GCM_TLEN_BITS, initVector);
            cipher.init(Cipher.ENCRYPT_MODE, key256, ivSpec);

            out.write(initVector);

            return new CipherOutputStream(out, cipher);
        } catch (IOException | NoSuchAlgorithmException | NoSuchPaddingException | InvalidKeyException
                | InvalidAlgorithmParameterException e) {
            throw new StandardJavaException(e);
        }
    }

    public static CipherInputStream decryptAES256GCMNoPadding(InputStream in, byte[] passKey)
            throws StandardJavaException {
        try {
            byte[] initVector = new byte[GCM_IV_LEN_BYTES];
            if (in.read(initVector) < GCM_IV_LEN_BYTES) {
                throw new IOException("couldn't read IV");
            }
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            SecretKey key256 = new SecretKeySpec(MessageDigest.getInstance("SHA-256").digest(passKey), "AES");
            GCMParameterSpec ivSpec = new GCMParameterSpec(GCM_TLEN_BITS, initVector);
            cipher.init(Cipher.DECRYPT_MODE, key256, ivSpec);

            return new CipherInputStream(in, cipher);
        } catch (IOException | NoSuchAlgorithmException | NoSuchPaddingException | InvalidKeyException
                | InvalidAlgorithmParameterException e) {
            throw new StandardJavaException(e);
        }
    }

    public static Object loadObjectResource(String fileName) throws StandardJavaException {
        try {
            ClassLoader callerCL = Class.forName(new Exception().getStackTrace()[1].getClassName()).getClassLoader();
            try (ObjectInputStream in = new ObjectInputStream(
                    new BufferedInputStream(callerCL.getResourceAsStream(fileName)))) {
                return in.readObject();
            }
        } catch (ClassNotFoundException | IOException e) {
            throw new StandardJavaException(e);
        }
    }

    public static void deleteFile(File file) {
        if (file.exists()) {
            for (int i = 0; i < 10; i++) {
                if (file.delete()) {
                    return;
                }
                sleep(500);
            }
            throw new IllegalStateException("file couldn't be deleted: " + file.getAbsolutePath());
        }
    }
}
