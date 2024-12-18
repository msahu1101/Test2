package com.mgm.services.booking.room;

import com.mgm.services.booking.room.model.request.AuroraPriceRequest;

import java.time.LocalDate;

public abstract class BaseRoomPriceBookingTest extends BaseRoomBookingTest {

	protected static void staticInit() {
		// Static initializer called from child classes
		// Used for initializing static dependencies.
		if (null == mapper || null == crsMapper) {
			runOnceBeforeClass();
		}
	}

	public static AuroraPriceRequest.AuroraPriceRequestBuilder<?, ?> makeBaseAuroraPriceRequest(LocalDate checkInDate,
			LocalDate checkOutDate) {
		return AuroraPriceRequest.builder()
				.source("mgmresorts")
				.customerId(-1)
				.checkInDate(checkInDate)
				.checkOutDate(checkOutDate)
				.numGuests(2)
				.propertyId("MV021")
				.programId("baseProgramId")
				.numRooms(1)
				.mlifeNumber("-1");
	}

	public static AuroraPriceRequest.AuroraPriceRequestBuilder<?, ?> makeBaseAuroraPriceRequestWithoutPropertyIds(LocalDate checkInDate,
			LocalDate checkOutDate) {
		return AuroraPriceRequest.builder()
				.source("mgmresorts")
				.customerId(-1)
				.checkInDate(checkInDate)
				.checkOutDate(checkOutDate)
				.numGuests(2)
				.propertyId(null)
				.programId("baseProgramId")
				.numRooms(1)
				.mlifeNumber("-1");
	}
}
