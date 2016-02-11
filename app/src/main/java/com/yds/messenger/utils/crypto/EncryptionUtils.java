package com.yds.messenger.utils.crypto;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;

/**
 * Created by yds on 30/11/15.
 */

/**
 * Class that abstracts out the encryption process.
 *
 * Uses AES encryption.
 */
public class EncryptionUtils {
    /**
     * Encrypts the String
     * @param plaintext
     * @param key
     * @return - The encrypted text
     */
    public static String encrypt(String plaintext, String key) {
        byte[] keyarr = Base64Utils.decode(key);
        SecretKeySpec sks = new SecretKeySpec(keyarr, "AES");
        byte[] encodedBytes = null;
        try {
            Cipher c = Cipher.getInstance("AES");
            c.init(Cipher.ENCRYPT_MODE, sks);
            encodedBytes = c.doFinal(plaintext.getBytes());
        } catch (Exception e) {
            e.printStackTrace();
        }
        String ciphertext = Base64Utils.encode(encodedBytes);
        return ciphertext;
    }

    /**
     * Decrypts the text
     * @param ciphertext
     * @param key
     * @return - The plaintext
     */
    public static String decrypt(String ciphertext, String key) {
        byte[] keyarr = Base64Utils.decode(key);
        SecretKeySpec sks = new SecretKeySpec(keyarr, "AES");
        byte[] encodedBytes = Base64Utils.decode(ciphertext);
        byte[] decodedBytes = null;
        try {
            Cipher c = Cipher.getInstance("AES");
            c.init(Cipher.DECRYPT_MODE, sks);
            decodedBytes = c.doFinal(encodedBytes);
        } catch (Exception e) {
            e.printStackTrace();
        }
        String plaintext = new String(decodedBytes);
        return plaintext;
    }
}
/**
 * Created by yds on 30/11/15.
 */