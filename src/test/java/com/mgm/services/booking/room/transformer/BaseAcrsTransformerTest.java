package com.mgm.services.booking.room.transformer;

import com.mgm.services.booking.room.BaseAcrsRoomBookingTest;
import com.mgm.services.booking.room.model.crs.reservation.*;
import com.mgm.services.booking.room.model.reservation.ItemizedChargeItem;
import com.mgm.services.booking.room.model.reservation.RoomChargeItemType;
import com.mgm.services.booking.room.model.reservation.RoomChargesAndTaxes;
import com.mgm.services.booking.room.properties.AcrsProperties;
import com.mgm.services.booking.room.util.ReservationUtil;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.text.ParseException;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

class BaseAcrsTransformerTest extends BaseAcrsRoomBookingTest {
	AcrsProperties acrsProperties;

	Date mockFirstNightDate;
	Date mockLastNightDate;

	@BeforeEach
	protected void init(){
		super.init();
		acrsProperties = new AcrsProperties();
		acrsProperties.setLiveCRS(true);
		setPropertyTaxAndChargesConfig(acrsProperties);
	}

	private List<RatePlanRes> setupBaseRatePlans() {
		ReservationRetrieveResReservation responseBody = getCrsRetrieveResv("three_night_no_components")
				.getBody();
		Assertions.assertNotNull(responseBody);
		return responseBody.getData().getHotelReservation().getRatePlans();
	}

	private SegmentResItem setupBaseMainSegmentResItem() {
		ReservationRetrieveResReservation responseBody = getCrsRetrieveResv("three_night_no_components")
				.getBody();
		Assertions.assertNotNull(responseBody);
		return BaseAcrsTransformer.getMainSegment(responseBody.getData().getHotelReservation().getSegments());
	}

	private List<SegmentResItem> setupBaseComponentSegmentResItem() {
		ReservationRetrieveResReservation responseBody = getCrsRetrieveResv("three_night_multiple_components")
				.getBody();
		Assertions.assertNotNull(responseBody);
		return BaseAcrsTransformer.getComponentSegments(responseBody.getData().getHotelReservation().getSegments());
	}

	@Test
	void getChargesAndTaxesMainSegmentBasicTest() {
		// Setup Mock Parameters
		SegmentResItem segment = setupBaseMainSegmentResItem();
		RoomChargeItemType roomChargeItemType = RoomChargeItemType.RoomCharge;
		AcrsProperties acrsPropertiesParam = acrsProperties;
		List<RatePlanRes> ratePlans = setupBaseRatePlans();
		String acrsPropertyCode = "MV021";

		// Execute Test
		RoomChargesAndTaxes response = BaseAcrsTransformer.getChargesAndTaxes(segment, roomChargeItemType,
				acrsPropertiesParam, ratePlans, acrsPropertyCode);

		// Validate Response
		assertNotNull(response);
		assertEquals(3, response.getCharges().size()); // Size of charges expected to be equal to nights
		assertEquals(3, response.getTaxesAndFees().size()); // Size of TaxesAndFees expected to be equal to nights

		// Room Charge and Resort fee values the same for each day so same check for each day
		response.getCharges().forEach(charge -> {
				assertEquals(199.0, charge.getAmount()); // charge amount equals known value
				Optional<ItemizedChargeItem> roomChargeOptional = charge.getItemized().stream()
						.filter(item -> RoomChargeItemType.RoomCharge.equals(item.getItemType()))
						.findFirst();
				assertTrue(roomChargeOptional.isPresent());
				assertEquals(162.0, roomChargeOptional.get().getAmount()); // RoomCharge equals known value
				Optional<ItemizedChargeItem> resortFeeChargeOptional = charge.getItemized().stream()
						.filter(item -> RoomChargeItemType.ResortFee.equals(item.getItemType()))
						.findFirst();
				assertTrue(resortFeeChargeOptional.isPresent());
				assertEquals(37.0, resortFeeChargeOptional.get().getAmount()); // Resort fee equals known value
		});

		// Room Charge Taxes and Resort Fee Taxes the same for each day so same check for each day
		response.getTaxesAndFees().forEach(taxCharge -> {
			assertEquals(26.63, taxCharge.getAmount()); // tax charge amount equals known value
			Optional<ItemizedChargeItem> roomChargeTaxOptional = taxCharge.getItemized().stream()
					.filter(item -> RoomChargeItemType.RoomChargeTax.equals(item.getItemType()))
					.findFirst();
			assertTrue(roomChargeTaxOptional.isPresent());
			assertEquals(21.68, roomChargeTaxOptional.get().getAmount()); // RoomChargeTax amount equals known value
			Optional<ItemizedChargeItem> resortFeeTaxOptional = taxCharge.getItemized().stream()
					.filter(item -> RoomChargeItemType.ResortFeeTax.equals(item.getItemType()))
					.findFirst();
			assertTrue(resortFeeTaxOptional.isPresent());
			assertEquals(4.95, resortFeeTaxOptional.get().getAmount()); // ResortFeeTax amount equals known value
		});
	}

	@Test
	void getChargesAndTaxesComponentSegmentBasicTest() throws ParseException {
		// Setup Mock Parameters
		mockFirstNightDate = fmt.parse("2024-06-13");
		mockLastNightDate = fmt.parse("2024-06-15");
		setupBaseComponentSegmentResItem().forEach(segment -> {
			Assertions.assertNotNull(segment);
			RoomChargeItemType roomChargeItemType = RoomChargeItemType.ComponentCharge;
			AcrsProperties acrsPropertiesParam = acrsProperties;
			List<RatePlanRes> ratePlans = setupBaseRatePlans();
			String acrsPropertyCode = "MV021";

			// Execute Test
			RoomChargesAndTaxes response = BaseAcrsTransformer.getChargesAndTaxes(segment, roomChargeItemType,
					acrsPropertiesParam, ratePlans, acrsPropertyCode);

			// Validate Response
			assertNotNull(response);
			assertComponentSegmentRoomChargesAndTaxesResponse(segment, response);
		});
	}

	private void assertComponentSegmentRoomChargesAndTaxesResponse(SegmentResItem segment,
																		  RoomChargesAndTaxes response) {
		Optional<ProductUseResItem> productUseResItemOptional = segment.getOffer().getProductUses().stream()
				.filter(ProductUseResItem::getIsMainProduct)
				.findFirst();
		if(!productUseResItemOptional.isPresent()) {
			Assertions.fail("Component Segment does not have a main product Product Use.");
		}
		ProductUseResItem productUse = productUseResItemOptional.get();
		String ratePlanCode = productUse.getRatePlanCode();
		PricingFrequency productUsePricingFrequency = productUse.getProductRates().getPricingFrequency();
		if (PricingFrequency.PERNIGHT.equals(productUsePricingFrequency)) {
			assertTrue("DOGFRIENDLY".equalsIgnoreCase(ratePlanCode));
			assertPerNightComponentResponse(response);
		} else if (PricingFrequency.PERUSE.equals(productUsePricingFrequency)) {
			if("EARLYCI".equalsIgnoreCase(ratePlanCode)) {
				assertPerUseCheckInComponentResponse(response);
			}
			if("LATECO".equalsIgnoreCase(ratePlanCode)) {
				assertPerUseCheckOutComponentResponse(response);
			}
		} else if (PricingFrequency.PERSTAY.equals(productUsePricingFrequency)) {
			// TODO currently don't have any in mock reservation.
		} else {
			Assertions.fail("Unexpected Pricing Frequency.");
		}
	}

	private void assertPerUseCheckOutComponentResponse(RoomChargesAndTaxes response) {
		// Component Charge should only be applicable for 1 day == check in date
		assertEquals(1,response.getCharges().size()); // assert only 1 day
		response.getCharges().forEach(charge -> {
			ReservationUtil.areDatesEqualExcludingTime(mockLastNightDate, charge.getDate());
			assertEquals(30.0, charge.getAmount()); // charge amount equals known value
			Optional<ItemizedChargeItem> componentChargeOptional = charge.getItemized().stream()
					.filter(item -> RoomChargeItemType.ComponentCharge.equals(item.getItemType()))
					.findFirst();
			assertTrue(componentChargeOptional.isPresent());
			assertEquals(30.0, componentChargeOptional.get().getAmount()); // ComponentCharge equals known value
		});

		// Component charge Taxes should only be applicable for 1 day == check in date
		assertEquals(1,response.getTaxesAndFees().size()); // assert only 1 day

		response.getTaxesAndFees().forEach(taxCharge -> {
			ReservationUtil.areDatesEqualExcludingTime(mockLastNightDate, taxCharge.getDate());
			assertEquals(4.01, taxCharge.getAmount()); // tax charge amount equals known value
			Optional<ItemizedChargeItem> componentChargeTaxOptional = taxCharge.getItemized().stream()
					.filter(item -> RoomChargeItemType.ComponentChargeTax.equals(item.getItemType()))
					.findFirst();
			assertTrue(componentChargeTaxOptional.isPresent());
			assertEquals(4.01, componentChargeTaxOptional.get().getAmount()); // ComponentChargeTax amount equals known value
		});
	}

	private void assertPerUseCheckInComponentResponse(RoomChargesAndTaxes response) {
		// Component Charge should only be applicable for 1 day == check in date
		assertEquals(1,response.getCharges().size()); // assert only 1 day
		response.getCharges().forEach(charge -> {
			ReservationUtil.areDatesEqualExcludingTime(mockFirstNightDate, charge.getDate());
			assertEquals(20.0, charge.getAmount()); // charge amount equals known value
			Optional<ItemizedChargeItem> componentChargeOptional = charge.getItemized().stream()
					.filter(item -> RoomChargeItemType.ComponentCharge.equals(item.getItemType()))
					.findFirst();
			assertTrue(componentChargeOptional.isPresent());
			assertEquals(20.0, componentChargeOptional.get().getAmount()); // ComponentCharge equals known value
		});

		// Component charge Taxes should only be applicable for 1 day == check in date
		assertEquals(1,response.getTaxesAndFees().size()); // assert only 1 day

		response.getTaxesAndFees().forEach(taxCharge -> {
			ReservationUtil.areDatesEqualExcludingTime(mockFirstNightDate, taxCharge.getDate());
			assertEquals(2.68, taxCharge.getAmount()); // tax charge amount equals known value
			Optional<ItemizedChargeItem> componentChargeTaxOptional = taxCharge.getItemized().stream()
					.filter(item -> RoomChargeItemType.ComponentChargeTax.equals(item.getItemType()))
					.findFirst();
			assertTrue(componentChargeTaxOptional.isPresent());
			assertEquals(2.68, componentChargeTaxOptional.get().getAmount()); // ComponentChargeTax amount equals known value
		});
	}

	private static void assertPerNightComponentResponse(RoomChargesAndTaxes response) {
		// Component Charge values are the same for each day so same check for each day
		assertEquals(3,response.getCharges().size()); // assert 3 days
		response.getCharges().forEach(charge -> {
			assertEquals(50.0, charge.getAmount()); // charge amount equals known value
			Optional<ItemizedChargeItem> componentChargeOptional = charge.getItemized().stream()
					.filter(item -> RoomChargeItemType.ComponentCharge.equals(item.getItemType()))
					.findFirst();
			assertTrue(componentChargeOptional.isPresent());
			assertEquals(50.0, componentChargeOptional.get().getAmount()); // ComponentCharge equals known value
		});

		// Component Charge Taxes are the same for each day so same check for each day
		assertEquals(3,response.getTaxesAndFees().size()); // assert 3 days
		response.getTaxesAndFees().forEach(taxCharge -> {
			assertEquals(6.69, taxCharge.getAmount()); // tax charge amount equals known value
			Optional<ItemizedChargeItem> componentChargeTaxOptional = taxCharge.getItemized().stream()
					.filter(item -> RoomChargeItemType.ComponentChargeTax.equals(item.getItemType()))
					.findFirst();
			assertTrue(componentChargeTaxOptional.isPresent());
			assertEquals(6.69, componentChargeTaxOptional.get().getAmount()); // ComponentChargeTax amount equals known value
		});
	}
}