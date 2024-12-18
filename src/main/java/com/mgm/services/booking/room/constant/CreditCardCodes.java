package com.mgm.services.booking.room.constant;

import com.mgm.services.common.exception.BusinessException;
import com.mgm.services.common.exception.ErrorCode;

import lombok.extern.log4j.Log4j2;

@Log4j2
public enum CreditCardCodes {
	
	// JCB : Japan Credit Bureau, China Union Pay : Union Pay
	AX("American Express","378282246310005"), CU("Union Pay","6282000123842342"), DC("Diners Club","30569309025904"), DS("Discover","6011000990099818"), JC("JCB","3566002020360505"), MC("Mastercard","5555555555554444"), VI("Visa","4387751111111111"),
	CP("CP",""), PC("PC",""), EC("EC","");

	String value;
	String number;

	public String getValue() {
		return this.value;
	}
	
	public String getNumber() {
		return this.number;
	}

	CreditCardCodes(String value, String number) {
		this.value = value;
		this.number = number;
	}

	private static CreditCardCodes fromValue(String value) {
		for (CreditCardCodes creditCardCode : CreditCardCodes.values()) {
			if (String.valueOf(creditCardCode.value).equals(value)) {
				return creditCardCode;
			}
		}
		return null;
	}

	public static String getCodeFromValue(String value) {
		final CreditCardCodes creditCardCode = fromValue(value);
		if (creditCardCode != null) {
			return creditCardCode.name();
		} else {
			log.error("Not supported paymentCard: " +value);
			throw new BusinessException(ErrorCode.INVALID_TYPE, "Not supported paymentCard: " +value);
		}
	}

	public static CreditCardCodes getCreditCardCodesFromCode(String code) {
		if (CreditCardCodes.valueOf(code) != null) {
			return CreditCardCodes.valueOf(code);
		} else {
			log.error("Not supported paymentCard: " +code);
			throw new BusinessException(ErrorCode.INVALID_TYPE, "Not supported paymentCard: " +code);
		}
	}
	

}
