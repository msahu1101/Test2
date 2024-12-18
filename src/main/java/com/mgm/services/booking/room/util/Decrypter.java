package com.mgm.services.booking.room.util;

import com.google.inject.Inject;
import org.apache.commons.codec.binary.Base64;

import javax.crypto.Cipher;
import java.io.*;
import java.math.BigInteger;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.spec.RSAPrivateKeySpec;

/**
 * The Aurora decrypter implementation.
 */
final public class Decrypter {
    /*
     * Private scope members
     */
    private PrivateKey _key;
    private Cipher _cipher;

    /**
      * Default constructor
      */
    @Inject
    public Decrypter() {}

    /*
     * Initialize the private key
     */
    final private PrivateKey readKey(final byte [] keyBytes) throws Exception {
        final ObjectInputStream oin = new ObjectInputStream(new ByteArrayInputStream(keyBytes));
        try {
            final BigInteger m = (BigInteger)oin.readObject();
            final BigInteger e = (BigInteger)oin.readObject();
            final RSAPrivateKeySpec keySpec = new RSAPrivateKeySpec(m, e);
            final KeyFactory fact = KeyFactory.getInstance("RSA");
            return fact.generatePrivate(keySpec);
        }
        finally {
            oin.close();
        }
    }

    final public Decrypter open(final byte [] privateKeyBytes) throws Exception {
        try {
            _key = readKey(privateKeyBytes);
            _cipher = Cipher.getInstance("RSA");
            _cipher.init(Cipher.DECRYPT_MODE, _key);
            return this;
        }
        catch (Exception e) {
            throw e;
        }
    }

    final public synchronized String decrypt(final String encryptedStr) throws Exception {
        if (_cipher == null) {
            throw new IllegalStateException("not open");
        }
        if (encryptedStr == null) {
            return null;
        }
        try {
            return new String(_cipher.doFinal(Base64.decodeBase64(encryptedStr.getBytes("UTF-8"))), "UTF-8");
        }
        catch (Throwable e) {
            throw e;
        }
    }
}
