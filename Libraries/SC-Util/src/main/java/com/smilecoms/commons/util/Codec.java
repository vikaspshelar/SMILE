/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.smilecoms.commons.util;

/**
 *
 * @author paul
 */
public class Codec {

    public static final char[] hexChar = {'0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'a', 'b', 'c', 'd', 'e', 'f'};

    private static byte[] decodeHex(String string) {
        byte[] bts = new byte[string.length() / 2];
        for (int i = 0; i < bts.length; i++) {
            bts[i] = (byte) Integer.parseInt(string.substring(2 * i, 2 * i + 2), 16);
        }
        return bts;
    }

    private static String encodeHex(byte[] bytes) {
        if (bytes == null) {
            return null;
        }

        StringBuilder sb = new StringBuilder(bytes.length * 2);
        for (int i = 0; i < bytes.length; i++) {
            // look up high nibble
            sb.append(hexChar[(bytes[i] & 0xf0) >>> 4]);

            // look up low nibble
            sb.append(hexChar[bytes[i] & 0x0f]);
        }
        return sb.toString();
    }

    public static String binToHexString(byte[] in) {
        return encodeHex(in);
    }

    public static String binToEncryptedHexString(byte[] in) {
        return encodeHex(SymmetricCrypto.encrypt(in));
    }

    public static String stringToEncryptedHexString(String in) {
        try {
            byte[] b = in.getBytes("UTF8");
            return binToEncryptedHexString(b);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static byte[] encryptedHexStringToDecryptedBin(String in) {
        return SymmetricCrypto.decrypt(decodeHex(in));
    }

    public static String encryptedHexStringToDecryptedHexString(String in) {
        return encodeHex(SymmetricCrypto.decrypt(decodeHex(in)));
    }

    public static String encryptedHexStringToDecryptedString(String in) {
        try {
            return new String(SymmetricCrypto.decrypt(decodeHex(in)), "UTF8");
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static byte[] hexStringToBin(String in) {
        return decodeHex(in);
    }

    public static String hexStringToEncryptedHexString(String in) {
        return encodeHex(SymmetricCrypto.encrypt(decodeHex(in)));
    }

    public static byte[] encryptedBinToDecryptedBin(byte[] in) {
        return SymmetricCrypto.decrypt(in);
    }

    public static byte[] binToEncryptedBin(byte[] in) {
        return SymmetricCrypto.encrypt(in);
    }

    
}
