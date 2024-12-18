package com.mgm.services.booking.room.constant;

public enum PaymentStatus {
	   DCCChecked("DCCChecked"),
	   Authorized("Authorized"),
	   Settled("Settled");
	
	 private String value;

	  PaymentStatus(String value) {
	    this.value = value;
	  }

	  public String getValue() {
	    return value;
	  }

}
