package com.mgm.services.booking.room.transformer;

import com.mgm.services.booking.room.constant.ACRSConversionUtil;
import com.mgm.services.booking.room.constant.ServiceConstant;
import com.mgm.services.booking.room.model.crs.searchoffers.*;
import com.mgm.services.booking.room.model.phoenix.RoomComponent;
import com.mgm.services.booking.room.model.reservation.RoomRequest;
import lombok.experimental.UtilityClass;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.commons.collections.CollectionUtils;

@UtilityClass
public class RoomComponentResponseTransformer {

	public static List<RoomRequest> getRoomRequestComponentResponse(SuccessfulPricing response, String gsePropertyID,
			String acrsPropertyCode) {
		final List<RoomRequest> roomRequests = new ArrayList<>();
		final Map<String, String> longDescriptionDetails = new HashMap<>();
		final Map<String, String> descriptionDetails = new HashMap<>();
		final Map<String, BaseAcrsTransformer.AddonComponentRateInfo> ratePlansMap = new HashMap<>();

		DataRsPricing data = response.getData();
		ratePlansMap.putAll(BaseAcrsTransformer.getRatePlans(data.getRatePlans()));

		List<ProductPricing> productPricingList = data.getProducts();
		if (CollectionUtils.isNotEmpty(productPricingList)) {
			for (ProductPricing productPricing : productPricingList) {
				if (null != productPricing.getInventoryTypeName()) {
					final Description inventoryTypeName = productPricing.getInventoryTypeName();
					if (null != inventoryTypeName) {
					descriptionDetails.put(productPricing.getInventoryTypeCode(), inventoryTypeName.values().toString().replaceAll("[\\[\\]]", ""));
				      }
				}
				if (null != productPricing.getDesc()) {
					final Description descriptionName = productPricing.getDesc();
					if (null != descriptionName) {
						longDescriptionDetails.put(productPricing.getInventoryTypeCode(),
								descriptionName.values().toString().replaceAll("[\\[\\]]", ""));
					}
				}
			}
		}

		List<AdditionalOfferPricing> additionalOfferPricingList = data.getAdditionalOffers();
		if (CollectionUtils.isNotEmpty(additionalOfferPricingList)) {
			for (AdditionalOfferPricing additionalOfferPricing : additionalOfferPricingList) {
				for (AdditionalProductUse additionalProductUse : additionalOfferPricing.getProductUses()) {
					final AdditionalProductRate rates = additionalProductUse.getRates();
					final TotalRateDetails totalRate = additionalOfferPricing.getTotalRate();
					if (null != rates && null != totalRate) {
						final RoomRequest roomRequest = new RoomRequest();
						final String inventoryTypeCode = additionalProductUse.getInventoryTypeCode();
						final String componentId = ACRSConversionUtil.createComponentGuid(inventoryTypeCode,
								additionalOfferPricing.getRatePlanCode(), ServiceConstant.COMPONENT_FORMAT, acrsPropertyCode);
						roomRequest.setId(componentId);
						roomRequest.setCode(additionalOfferPricing.getRatePlanCode());
						BaseAcrsTransformer.AddonComponentRateInfo ratePlanInfo = ratePlansMap.get(additionalOfferPricing.getRatePlanCode());
						if(null != ratePlanInfo) {
							roomRequest.setRatePlanName(ratePlanInfo.getName());
							//CBSR-1906 set shortDescription long from ACRS rate plan
							roomRequest.setShortDescription(ratePlanInfo.getShortDesc());
							roomRequest.setLongDescription(ratePlanInfo.getLongDesc());
						}
						roomRequest.setRatePlanCode(additionalOfferPricing.getRatePlanCode());
						roomRequest.setDescription(descriptionDetails.get(inventoryTypeCode));

						roomRequest.setSelected(true);

						final AdditionalProductRate.PricingFrequencyEnum pricingFrequency = rates.getPricingFrequency();
						if (pricingFrequency == AdditionalProductRate.PricingFrequencyEnum.PERNIGHT) {
							// PER NIGHT
							roomRequest.setNightlyCharge(true);
							roomRequest.setPricingApplied(ServiceConstant.NIGHTLY_PRICING_APPLIED);
							final DailyTypeAmount avg = totalRate.getAvg();
							final Float amtBfTax = Float.parseFloat(totalRate.getBsAmt());
							final double amtAfTax = Double.parseDouble(avg.getAmtAfTx());
							double taxRate = 0;
							if (amtBfTax != 0) {
								taxRate = ((amtAfTax - amtBfTax) / amtBfTax) * 100;
							}
							roomRequest.setTaxRate(Float.parseFloat(String.format("%.2f", taxRate)));
							roomRequest.setPrice(amtBfTax);
							roomRequest.setAmtAftTax(amtAfTax);
						} else {
							roomRequest.setNightlyCharge(false);
							if (pricingFrequency == AdditionalProductRate.PricingFrequencyEnum.PERSTAY) {
								// PER STAY
								roomRequest.setPricingApplied(ServiceConstant.CHECKIN_PRICING_APPLIED);
							} else {
								// PER USE
								if (rates
										.getBookingPattern() == AdditionalProductRate.BookingPatternEnum.DAYOFCHECKIN) {
									roomRequest.setPricingApplied(ServiceConstant.CHECKIN_PRICING_APPLIED);
								} else {
									roomRequest.setPricingApplied(ServiceConstant.CHECKOUT_PRICING_APPLIED);
								}
							}
							Float amtBfTax = 0f;
							if(totalRate.getAmtBfTx() != null) {
								amtBfTax = Float.parseFloat(totalRate.getAmtBfTx());
							}
							final double amtAfTax = Double.parseDouble(totalRate.getAmtAfTx());
							double taxRate = 0;
							if (amtBfTax != 0) {
								taxRate = ((amtAfTax - amtBfTax) / amtBfTax) * 100;
							}
							roomRequest.setTaxRate(Float.parseFloat(String.format("%.2f", taxRate)));
							roomRequest.setPrice(amtBfTax);
							roomRequest.setAmtAftTax(amtAfTax);
						}
						roomRequests.add(roomRequest);
					}
				}
			}
		}
		return roomRequests;
	}

	public static RoomComponent getRoomComponentResponse (RoomRequest roomRequest) {
		RoomComponent roomComponent = new RoomComponent();
		roomComponent.setId(roomRequest.getId());
		roomComponent.setPrice((float) roomRequest.getPrice());
		roomComponent.setName(roomRequest.getCode());
		roomComponent.setTaxRate(roomRequest.getTaxRate());
		roomComponent.setPricingApplied(roomRequest.getPricingApplied());
		return roomComponent;
	}
}
