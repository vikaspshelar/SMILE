/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.smilecoms.commons.bpel;


import javax.crypto.Cipher;
import java.security.Key;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.crypto.spec.SecretKeySpec;

/**
 *
 * @author paul
 */
class SymmetricCrypto {

    private static final String algorithm = "AES";
    private static final Logger log = Logger.getLogger(SymmetricCrypto.class.getName());
    private static Key q = null;
    private static final ThreadLocal<Cipher> encryptCiphers = new ThreadLocal<>();

    private static Cipher getEncryptCipher() throws Exception {
        Cipher myCipher = encryptCiphers.get();
        if (myCipher == null) {
            myCipher = Cipher.getInstance(algorithm);
            myCipher.init(Cipher.ENCRYPT_MODE, q);
            encryptCiphers.set(myCipher);
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
            log.log(Level.WARNING, "Error: ", e);
            throw new java.lang.InstantiationError();
        }
    }

    protected static byte[] encrypt(byte[] inputToEncrypt)  {
        try {
            return getEncryptCipher().doFinal(inputToEncrypt);
        } catch (Exception e) {
            log.log(Level.WARNING, "Error: ", e);
        }
        return null;
    }

        
}
