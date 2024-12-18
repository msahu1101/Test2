package com.mgm.services.booking.room.util;

import java.security.InvalidKeyException;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;

import lombok.experimental.UtilityClass;

/**
 * Tokenization service requires credit card info to be encrypted in to a string
 * using the public key supplied by freedompay
 *
 */
@UtilityClass
public class TokenizationEncryptionUtil {

    /**
     * Read public key based on public key contents
     * 
     *            Key contents
     * @param keyContent
     * @return Returns public key object
     * @throws NoSuchAlgorithmException
     *             Algorithm exception
     * @throws InvalidKeySpecException
     *             Invalid key spec exception
     */
    private PublicKey readPublicKey(String keyContent) throws NoSuchAlgorithmException, InvalidKeySpecException {
        X509EncodedKeySpec publicSpec = new X509EncodedKeySpec(Base64.getDecoder().decode(keyContent.getBytes()));
        KeyFactory keyFactory = KeyFactory.getInstance("RSA");
        return keyFactory.generatePublic(publicSpec);
    }

    /**
     * Encrypts a string using the public key.
     * 
     * @param key
     *            Public key
     * @param plaintext
     *            Content to be encrypted
     * @return Returns encrypted string based on public key
     * @throws NoSuchAlgorithmException
     *             Algorithm exception
     * @throws NoSuchPaddingException
     *             Padding exception
     * @throws InvalidKeyException
     *             Invalid key exception
     * @throws IllegalBlockSizeException
     *             Block size exception
     * @throws BadPaddingException
     *             Bad padding exception
     */
    private String encrypt(PublicKey key, String plaintext) throws NoSuchAlgorithmException, NoSuchPaddingException,
            InvalidKeyException, IllegalBlockSizeException, BadPaddingException {
        Cipher cipher = Cipher.getInstance("RSA/ECB/OAEPWithSHA1AndMGF1Padding");
        cipher.init(Cipher.ENCRYPT_MODE, key);
        return Base64.getEncoder().encodeToString(cipher.doFinal(plaintext.getBytes()));
    }

    /**
     * Encrypt the card number and expiry by fetching the public key supplied by
     * freedom pay
     * 
     * @param cardNumber
     *            Card number
     * @param expiry
     *            Expiry date
     * @return Returns encrypted string
     */
    public static String encrypt(String paymentInfoStr, String publicKeySecret) {

        try {
            PublicKey publicKey = readPublicKey(publicKeySecret);
            return encrypt(publicKey, paymentInfoStr);
        } catch (NoSuchAlgorithmException | InvalidKeySpecException | InvalidKeyException | NoSuchPaddingException
                | IllegalBlockSizeException | BadPaddingException e) {
            return "";
        }

    }
}
