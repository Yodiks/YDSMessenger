package com.yds.messenger.utils.crypto;

import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;

import javax.crypto.KeyGenerator;

/**
 * Created by yds on 30/11/15.
 */

/**
 * That that helps generate unique keys.
 *
 * Generates 256 bit keys for AES encryption.
 */
public class KeyGeneratorUtils {
    /**
     * Generate a new key (256 bit for AES encryption)
     * @param secretUsername - Secret username of user
     * @return - The generated secret key
     */
    public static String getNewKey(String secretUsername) {
        String salt = "KNzFSWX7hCcG3qoZJx0V";
        String timestamp = System.currentTimeMillis() + "";
        String seed = salt + secretUsername + timestamp + salt;
        byte[] arr = null;
        try {
            SecureRandom sr = SecureRandom.getInstance("SHA1PRNG");
            sr.setSeed(seed.getBytes());
            KeyGenerator kg = KeyGenerator.getInstance("AES");
            kg.init(256, sr);
            arr = (kg.generateKey()).getEncoded();
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        if (arr == null) {
            return null;
        } else {
            return Base64Utils.encode(arr);
        }
    }
}
/**
 * Created by yds on 30/11/15.
 */