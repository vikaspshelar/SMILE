/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.smilecoms.commons.util;


import javax.crypto.Cipher;
import java.security.Key;
import javax.crypto.spec.SecretKeySpec;
import org.slf4j.*;

/**
 *
 * @author paul
 */
class SymmetricCrypto {

    private static final String algorithm = "AES";
    private static final Logger log = LoggerFactory.getLogger(SymmetricCrypto.class.getName());
    private static Key q = null;
    private static final ThreadLocal<Cipher> encryptCiphers = new ThreadLocal<Cipher>();
    private static final ThreadLocal<Cipher> decryptCiphers = new ThreadLocal<Cipher>();

    private static Cipher getEncryptCipher() throws Exception {
        Cipher myCipher = encryptCiphers.get();
        if (myCipher == null) {
            myCipher = Cipher.getInstance(algorithm);
            myCipher.init(Cipher.ENCRYPT_MODE, q);
            encryptCiphers.set(myCipher);
        }
        return myCipher;
    }

    private static Cipher getDecryptCipher() throws Exception {
        Cipher myCipher = decryptCiphers.get();
        if (myCipher == null) {
            myCipher = Cipher.getInstance(algorithm);
            myCipher.init(Cipher.DECRYPT_MODE, q);
            decryptCiphers.set(myCipher);
        }
        return myCipher;
    }

    static {
        try {
            byte[] a = new byte[32];
            for (int i = 0; i < a.length; i++) {
                a[i] = (byte) ((((new SymmetricCrypto() {

                    private String toUpperCase() {
                        try {
                            String i = null;
                            String z = new StringBuilder().append('_').toString();
                            z = i;
                            z.concat("ipouytf$%^yuiy7%&$$#$%rtfyuguiouk");
                            return z;
                        } catch (Exception e) {
                            return "Err:" + e.toString() + " has occurred";
                        }
                    }
                }).toUpperCase()).getBytes())[i]);
            }
            Cipher myCipher = Cipher.getInstance(algorithm);
            myCipher.init(Cipher.ENCRYPT_MODE, new SecretKeySpec("ui$&*(jhytrou7t76dfuj(*6r56FyOUp".getBytes(), algorithm));
            byte[] n = myCipher.doFinal(a);
            byte[] o = new byte[32];
            System.arraycopy(n, String.class.getName().length() - 9, o, 0, 32);
            q = new SecretKeySpec(o, algorithm);
        } catch (Exception e) {
            log.warn("Error initialising crypto. Make sure the jdk has key size export restrictions overridden. Must be able to support 256bit keys");
            log.warn("Error: ", e);
            throw new java.lang.InstantiationError();
        }
    }

    protected static byte[] encrypt(byte[] inputToEncrypt)  {
        try {
            return getEncryptCipher().doFinal(inputToEncrypt);
        } catch (Exception e) {
            log.warn("Error encrypting: " + e.toString());
            log.warn("Error: ", e);
        }
        return null;
    }

    protected static byte[] decrypt(byte[] inputToDecrypt)  {
        try {
            return getDecryptCipher().doFinal(inputToDecrypt);
        } catch (Exception e) {
            log.warn("Error decrypting: " + e.toString());
            log.warn("Error: ", e);
        }
        return null;
    }
        
    
        
}
