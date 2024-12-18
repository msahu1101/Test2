package com.mgm.services.booking.room.service;

public interface UserInfoDecryptionService {
    
    String decrypt(String data);
    
    String encrypt(String data);
}
