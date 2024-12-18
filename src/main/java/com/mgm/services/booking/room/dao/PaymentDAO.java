package com.mgm.services.booking.room.dao;

import org.springframework.http.HttpEntity;

import com.mgm.services.booking.room.model.DestinationHeader;
import com.mgm.services.booking.room.model.crs.reservation.ReservationModifyPendingRes;
import com.mgm.services.booking.room.model.crs.reservation.ReservationPartialModifyReq;
import com.mgm.services.booking.room.model.crs.reservation.ReservationRetrieveResReservation;
import com.mgm.services.booking.room.model.paymentservice.AuthRequest;
import com.mgm.services.booking.room.model.paymentservice.AuthResponse;
import com.mgm.services.booking.room.model.paymentservice.CaptureRequest;
import com.mgm.services.booking.room.model.paymentservice.CaptureResponse;
import com.mgm.services.booking.room.model.paymentservice.RefundRequest;
import com.mgm.services.booking.room.model.paymentservice.RefundResponse;
import com.mgm.services.booking.room.model.request.PaymentTokenizeRequest;
import com.mgm.services.common.model.authorization.AuthorizationTransactionRequest;
import com.mgm.services.common.model.authorization.AuthorizationTransactionResponse;

public interface PaymentDAO {
	
	AuthResponse authorizePayment(HttpEntity<AuthRequest> authReq);
	
	CaptureResponse capturePayment(HttpEntity<CaptureRequest> capReq);
	
	RefundResponse refundPayment(HttpEntity<RefundRequest> refReq);
	
	String deTokenizeCreditCard(String token);

    String tokenizeCreditCard(PaymentTokenizeRequest tokenizeRequest);

	AuthorizationTransactionResponse afsAuthorize(HttpEntity<AuthorizationTransactionRequest> request);
	
    ReservationModifyPendingRes sendRequestToPaymentExchangeToken(ReservationPartialModifyReq request,
            String tokenPath, DestinationHeader destinationHeader, String confirmationNo, boolean isPoFlow);

    ReservationRetrieveResReservation sendRetrieveRequestToPaymentExchangeToken(String tokenPath,
            DestinationHeader destinationHeader, String confirmationNo, String cardExpireDateXpath);
}
