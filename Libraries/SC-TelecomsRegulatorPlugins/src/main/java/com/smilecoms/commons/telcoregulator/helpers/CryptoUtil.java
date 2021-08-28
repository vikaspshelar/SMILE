/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.smilecoms.commons.telcoregulator.helpers;

//import com.smilecoms.commons.base.BaseUtils;
import com.smilecoms.commons.telcoregulator.nida.core.RequestBaseCryptoInfoPayload;
import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.KeyPair;
import java.security.KeyStore;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.Signature;
import java.security.cert.Certificate;
import java.security.spec.InvalidKeySpecException;
import java.util.concurrent.locks.ReentrantLock;
import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.Mac;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;
import org.apache.commons.codec.binary.Base64;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author sabza
 */
public class CryptoUtil {

    private static final Logger log = LoggerFactory.getLogger(CryptoUtil.class);
    private static final SecureRandom random = new SecureRandom();

    private static final Base64 BASE64 = new Base64();

    private static final String ENCODING = "UTF-8";
    private static final String HMAC_SHA1 = "HmacSHA1";
    private static final String ALGORITHM_AES = "AES";
    private static final String ALGORITHM_AES256 = "AES/CBC/PKCS5Padding";
    /**
     * The number of bytes that should be used to generate the nonce value.
     */
    private static final int SALT_SIZE = 20;

    /**
     * Returns the secret key to be used to encrypt and decrypt values. The key
     * will be generated the first time it is requested. Will generate key
     * material using a SecureRandom and then manufacture the key. Once the key
     * is created it is cached locally and the same key instance will be
     * returned until the application is shutdown or restarted.
     *
     * @param password
     * @param salt
     * @return SecretKey the secret key used to encrypt and decrypt values
     * @throws java.security.NoSuchAlgorithmException
     */
    public static SecretKeySpec createKey(String password, byte[] salt) throws NoSuchAlgorithmException, InvalidKeySpecException {
        // Now manufacture the actual Secret Key instance
        /* Derive the key, given password and salt. */

        //SecretKeyFactory factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
        SecretKeyFactory factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA1");
        PBEKeySpec spec = new PBEKeySpec(password.toCharArray(), salt, 65556, 256);
        //KeySpec spec = new PBEKeySpec(password.toCharArray(), salt.getBytes(), 65536, 256);
        SecretKey tmp = factory.generateSecret(spec);
        SecretKeySpec secret = new SecretKeySpec(tmp.getEncoded(), "AES");
        log.info("Finished creating a key");
        return secret;
    }

    /**
     * Generates a nonce value using a secure random.
     *
     * @param ivSizeBytes The number of bytes that should be used to generate
     * the nonce value.
     * @return IvParameterSpec instance
     */
    //public static IvParameterSpec createIV(final int ivSizeBytes, final Optional<SecureRandom> rng) {
    public static IvParameterSpec createIV(final int ivSizeBytes) {
        log.info("Creating a createIV");
        //The IV doesn't have to be secret, but it has to be unpredictable for CBC mode (and unique for CTR). It can be sent along with the ciphertext.
        //A common way to do this is by prefixing the IV to the ciphertext and slicing it off before decryption. It should be generated through SecureRandom
        byte[] nonce = new byte[ivSizeBytes];
        random.nextBytes(nonce);
        log.info("Finished creating createIV");
        return new IvParameterSpec(nonce);
    }

    public static byte[] getSalt() {
        log.info("Creating a salt");
        byte[] nonce = new byte[SALT_SIZE];
        new SecureRandom().nextBytes(nonce);
        log.info("Finished creating salt");
        return nonce;
    }

    public static IvParameterSpec readIV(final int ivSizeBytes, final InputStream is) throws IOException {
        log.info("Reading IV");
        final byte[] iv = new byte[ivSizeBytes];
        int offset = 0;
        while (offset < ivSizeBytes) {
            final int read = is.read(iv, offset, ivSizeBytes - offset);
            if (read == -1) {
                throw new IOException("Too few bytes for IV in input stream");
            }
            offset += read;
        }
        log.info("Finished reading IV");
        return new IvParameterSpec(iv);
    }

    private static Cipher getCipher(int encryptMode, SecretKeySpec secretKeySpec, IvParameterSpec iv) throws Exception {
        Cipher cipher = Cipher.getInstance(ALGORITHM_AES256);
        cipher.init(encryptMode, secretKeySpec, iv);
        return cipher;
    }

    /**
     * Takes message and encrypts with Key
     *
     * @param message String
     * @return String Base64 encoded
     */
    public static String getEncryptedMessage(String message, SecretKeySpec secretKeySpec, IvParameterSpec iv) throws Exception {
        Cipher cipher = getCipher(Cipher.ENCRYPT_MODE, secretKeySpec, iv);
        byte[] encryptedTextBytes = cipher.doFinal(message.getBytes("UTF-8"));
        return base64Encode(encryptedTextBytes);
    }

    /**
     * Takes Base64 encoded String and decodes with provided key
     *
     * @param message String encoded with Base64
     * @return String
     */
    public static String getDecryptedMessage(String message, SecretKeySpec secretKeySpec, IvParameterSpec iv) throws Exception {
        Cipher cipher = getCipher(Cipher.DECRYPT_MODE, secretKeySpec, iv);
        byte[] encryptedTextBytes = decodeBase64(message);
        byte[] decryptedTextBytes = cipher.doFinal(encryptedTextBytes);
        return new String(decryptedTextBytes);

    }

    private static byte[] decodeBase64(String s) {
        return BASE64.decode(s.getBytes());
    }

    private static String base64Encode(byte[] b) {
        return new String(BASE64.encode(b));
    }

    public static void encryptNidaAESCryptoParameters(Cipher cipher, SecretKey aesKey, IvParameterSpec ivForCBC, RequestBaseCryptoInfoPayload baseCryptoPayload) throws IllegalBlockSizeException, BadPaddingException {
        baseCryptoPayload.setEncryptedCryptoIV(base64Encode(cipher.doFinal(ivForCBC.getIV())));
        baseCryptoPayload.setEncryptedCryptoKey(base64Encode(cipher.doFinal(aesKey.getEncoded())));
    }

    public static void encryptNidaAESCryptoParameters(Cipher cipher, SecretKey aesKey, byte[] ivForCBC, RequestBaseCryptoInfoPayload baseCryptoPayload) throws IllegalBlockSizeException, BadPaddingException {
        baseCryptoPayload.setEncryptedCryptoIV(base64Encode(cipher.doFinal(ivForCBC)));
        baseCryptoPayload.setEncryptedCryptoKey(base64Encode(cipher.doFinal(aesKey.getEncoded())));
    }

    public static Cipher getCipherForNidaAESCryptoParameters() throws Exception {
        log.info("In getCipherForNidaAESCryptoParameters");
        PublicKey publicKey = getPublicKey(CIGKeyStorePKCS, "cigsecurity's iris id");
        Cipher cipher = Cipher.getInstance(ALGORITHM_AES256);
        cipher.init(Cipher.ENCRYPT_MODE, publicKey);
        return cipher;
    }

    private static byte[] computeSignature(String inputString) throws Exception {

        String cigServerKey = "";

        byte[] keyBytes = cigServerKey.getBytes(ENCODING);
        SecretKey key = new SecretKeySpec(keyBytes, HMAC_SHA1);
        Mac mac = Mac.getInstance(HMAC_SHA1);
        mac.init(key);
        byte[] text = inputString.getBytes(ENCODING);

        return mac.doFinal(text);
    }

    public static String getSignature(String textToSign, String nameOfStore) throws Exception {
        log.info("Signing pay load");
        PrivateKey privateKey = null;
        if (nameOfStore.equals("CIG")) {
            privateKey = getPrivateKey(CIGKeyStorePKCS, "cigsecurity's iris id", keystorePass);
        } else if (nameOfStore.equals("Smile")) {
            privateKey = getPrivateKey(SmileKeyStorePKCS, "smile’s iris id", keystorePass);
        }
        Signature signer = Signature.getInstance("SHA1withRSA");
        signer.initSign(privateKey);
        signer.update(textToSign.getBytes());
        byte[] signature = signer.sign();
        String sign = base64Encode(signature);
        log.debug("Signature: {0}", sign);
        return sign;
    }

    public static PrivateKey getPrivateKey(KeyStore ks, String alias, String password) throws Exception {
        //initKeyStoreProps();
        log.debug("Going to retrieve private key from keystore with alias [{}]", alias);
        KeyStore.ProtectionParameter pp = new KeyStore.PasswordProtection(password.toCharArray());
        KeyStore.PrivateKeyEntry pkey = (KeyStore.PrivateKeyEntry) ks.getEntry(alias, pp);
        if (pkey != null) {
            log.debug("PrivateKeyEntry is ::: {0}", pkey.toString());
        }
        PrivateKey key = pkey.getPrivateKey();
        String keyData = base64Encode(key.getEncoded());
        log.debug("PrivateKey is ::: {0}", keyData);
        return key;
    }

    public static PublicKey getPublicKey(KeyStore ks, String alias) throws Exception {
        //initKeyStoreProps();
        /*You can extract the public key certificate from the keystore*/
        Certificate cert = ks.getCertificate(alias);
        if (cert != null) {
            log.debug("Found certificate for PublicKey: {} ", cert);
        }
        PublicKey publicKey = cert.getPublicKey();
        if (publicKey != null) {
            log.debug("PublicKey length is: ", publicKey.getEncoded().length);
        }
        log.debug("Public key is: ", base64Encode(publicKey.getEncoded()));
        return publicKey;
    }

    public KeyPair getKeyPair(String alias, String password) throws Exception {
        //initKeyStoreProps();

        PrivateKey priv = (PrivateKey) CIGKeyStorePKCS.getKey(alias, password.toCharArray());
        Certificate cert = CIGKeyStorePKCS.getCertificate(alias);
        PublicKey publicKey = cert.getPublicKey();
        return new KeyPair(publicKey, priv);
    }

    private static InputStream getDataStream(String fname) throws IOException {
        FileInputStream fis = new FileInputStream(fname);
        DataInputStream dis = new DataInputStream(fis);
        byte[] bytes = new byte[dis.available()];
        dis.readFully(bytes);
        ByteArrayInputStream bais = new ByteArrayInputStream(bytes);
        return bais;
    }

    private static final ReentrantLock lock = new ReentrantLock();
    private static String keystorePass = null;
    private static KeyStore CIGKeyStorePKCS = null;
    private static KeyStore SmileKeyStorePKCS = null;
    private static boolean keystorePropsInit = false;

    /*public static void initKeyStoreProps() {
        if (!keystorePropsInit) {
            // Only get props once for performance reasons
            try {
                if (!lock.tryLock(100, TimeUnit.MILLISECONDS)) {
                    log.info("Another thread is already initialising keystore properties");
                    return;
                }
            } catch (Exception e) {
            }
            try {

                if (!keystorePropsInit) {
                    keystorePass = BaseUtils.getSubProperty("env.nida.config", "KeyStorePassword");
                    String tempDir = System.getProperty("java.io.tmpdir") + "/hobit_keystore";

                    File cigKeyStorFile = new File(tempDir, "keyserverstore.keystore");
                    File smileKeyStoreFile = new File(tempDir, "keyclientstore.keystore");

                    cigKeyStorFile.mkdirs();
                    if (cigKeyStorFile.exists()) {
                        cigKeyStorFile.delete();
                    }

                    smileKeyStoreFile.mkdirs();
                    if (smileKeyStoreFile.exists()) {
                        smileKeyStoreFile.delete();
                    }

                    log.info("Directories created");

                    KeyStore CIGksPKC = KeyStore.getInstance("PKCS12");
                    KeyStore SmileksPKC = KeyStore.getInstance("PKCS12");

                    if (cigKeyStorFile.exists()) {
                        // if exists, load
                        log.info("Load CIG keystore from existing file");
                        CIGksPKC.store(new FileOutputStream(cigKeyStorFile), keystorePass.toCharArray());
                    } else {
                        // if not exists, create
                        log.info("Load CIG keystore from empty file");
                        String CIGCertsKeyStore = BaseUtils.getSubProperty("env.nida.config", "CIGCertsKeyStoreFileLocation");
                        CIGksPKC.load(new FileInputStream(CIGCertsKeyStore), keystorePass.toCharArray());
                        CIGksPKC.store(new FileOutputStream(cigKeyStorFile), keystorePass.toCharArray());
                    }

                    if (smileKeyStoreFile.exists()) {
                        // if exists, load
                        log.info("Load Smile keystore from existing file");
                        SmileksPKC.store(new FileOutputStream(smileKeyStoreFile), keystorePass.toCharArray());

                    } else {
                        // if not exists, create
                        log.info("Load Smile keystore from scratch config files");
                        String SmileIrisCertsKeyStoreFileLocation = BaseUtils.getSubProperty("env.nida.config", "SmileIrisCertsKeyStoreFileLocation");
                        SmileksPKC.load(new FileInputStream(SmileIrisCertsKeyStoreFileLocation), keystorePass.toCharArray());
                        SmileksPKC.store(new FileOutputStream(smileKeyStoreFile), keystorePass.toCharArray());
                    }

                    CIGKeyStorePKCS = CIGksPKC;
                    SmileKeyStorePKCS = SmileksPKC;
                    cigKeyStorFile.deleteOnExit();
                    smileKeyStoreFile.deleteOnExit();
                    keystorePropsInit = true;
                }
            } catch (Exception ex) {
                log.warn("Failed to create keystore: ", ex);
            } finally {
                lock.unlock();
            }
        }
    }*/
}
