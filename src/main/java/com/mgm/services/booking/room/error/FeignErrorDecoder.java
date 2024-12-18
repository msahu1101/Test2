package com.mgm.services.booking.room.error;

import com.mgm.services.common.exception.BusinessException;
import com.mgm.services.common.exception.ErrorCode;

import feign.Response;
import feign.codec.ErrorDecoder;

/**
 * Customer error decoder to be used for feign clients.
 *
 */
public class FeignErrorDecoder implements ErrorDecoder {

    @Override
    public Exception decode(String methodKey, Response response) {
        return new BusinessException(ErrorCode.ACCOUNT_NOT_FOUND);
    }
}
