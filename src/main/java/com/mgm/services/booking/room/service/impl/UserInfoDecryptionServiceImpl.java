package com.mgm.services.booking.room.service.impl;

import com.mgm.services.booking.room.constant.ServiceConstant;
import com.mgm.services.booking.room.properties.ApplicationProperties;
import com.mgm.services.booking.room.properties.SecretsProperties;
import com.mgm.services.booking.room.service.UserInfoDecryptionService;
import com.mgm.services.common.exception.BusinessException;
import com.mgm.services.common.exception.ErrorCode;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.crypto.*;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.util.Base64;

@Component
@Log4j2
public class UserInfoDecryptionServiceImpl implements UserInfoDecryptionService {

    @Autowired
    private SecretsProperties secretProperties;

    @Autowired
    private ApplicationProperties appProps;
    private static final int TAG_LENGTH = 128;

    private SecretKey deriveKeyFromPassword() {
        return new SecretKeySpec(Base64.getDecoder().decode(secretProperties.getSecretValue(appProps.getPoDeepLinkKey()).getBytes()), ServiceConstant.PO_DEEPLINK_ENC_ALGORITHM);
    }

    private byte[] decryptData(String data)
            throws NoSuchAlgorithmException, InvalidKeySpecException, NoSuchPaddingException,
            InvalidAlgorithmParameterException, InvalidKeyException, IllegalBlockSizeException, BadPaddingException {

        byte[] encryptedData = Base64.getDecoder().decode(data);
        byte[] iv = Base64.getDecoder().decode(secretProperties.getSecretValue(appProps.getPoDeepLinkIV()).getBytes());

        SecretKey keySpec = deriveKeyFromPassword();
        Cipher cipher = Cipher.getInstance(secretProperties.getSecretValue(appProps.getPoDeepLinkEncAlgorithm()));
        GCMParameterSpec gcmParameterSpec = new GCMParameterSpec(TAG_LENGTH, iv);
        cipher.init(Cipher.DECRYPT_MODE, keySpec, gcmParameterSpec);

        byte[] decryptedData = cipher.doFinal(encryptedData);
        return decryptedData;
    }
    
    private byte[] encryptData(String data)
            throws NoSuchAlgorithmException, InvalidKeySpecException, NoSuchPaddingException,
            InvalidAlgorithmParameterException, InvalidKeyException, IllegalBlockSizeException, BadPaddingException {

        
        byte[] iv = Base64.getDecoder().decode(secretProperties.getSecretValue(appProps.getPoDeepLinkIV()).getBytes());

        SecretKey keySpec = deriveKeyFromPassword();
        Cipher cipher = Cipher.getInstance(secretProperties.getSecretValue(appProps.getPoDeepLinkEncAlgorithm()));
        GCMParameterSpec gcmParameterSpec = new GCMParameterSpec(TAG_LENGTH, iv);
        cipher.init(Cipher.ENCRYPT_MODE, keySpec, gcmParameterSpec);

        byte[] decryptedData = cipher.doFinal(data.getBytes());
        byte[] encryptedData = Base64.getEncoder().encode(decryptedData);
        return encryptedData;
    }

    @Override
    public String decrypt(String data) {
        try {
            return new String(decryptData(data));
        } catch (NoSuchAlgorithmException e) {
            throw new BusinessException(ErrorCode.INVALID_CUSTOMER_DETAILS);
        } catch (InvalidKeySpecException | NoSuchPaddingException | InvalidAlgorithmParameterException | InvalidKeyException | IllegalBlockSizeException | BadPaddingException e) {
            throw new BusinessException(ErrorCode.INVALID_CUSTOMER_DETAILS);
        } catch (Exception e) {
        	return null;
        }

    }
    
    @Override
    public String encrypt(String data) {
        try {
            return new String(encryptData(data));
        } catch (NoSuchAlgorithmException e) {
            throw new BusinessException(ErrorCode.INVALID_CUSTOMER_DETAILS);
        } catch (InvalidKeySpecException | NoSuchPaddingException | InvalidAlgorithmParameterException | InvalidKeyException | IllegalBlockSizeException | BadPaddingException e) {
            throw new BusinessException(ErrorCode.INVALID_CUSTOMER_DETAILS);
        } catch (Exception e) {
        	return null;
        }

    }

}
