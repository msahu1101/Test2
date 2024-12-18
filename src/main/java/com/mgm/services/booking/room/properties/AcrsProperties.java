package com.mgm.services.booking.room.properties;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mgm.services.booking.room.constant.ServiceConstant;
import com.mgm.services.booking.room.model.acrschargeandtax.*;
import com.mgm.services.booking.room.model.crs.reservation.PaymentType;
import com.mgm.services.booking.room.model.crs.reservation.SegmentPmsStatus;
import com.mgm.services.booking.room.util.CommonUtil;
import com.mgm.services.booking.room.util.ReservationUtil;
import com.mgm.services.common.exception.ErrorCode;
import com.mgm.services.common.exception.SystemException;
import lombok.Data;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import javax.annotation.PostConstruct;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Configuration class to read properties from application.properties file with
 * "aurora" prefix
 *
 */
@Component
@Log4j2
@ConfigurationProperties(
        prefix = "acrs")
public @Data class AcrsProperties {

    @Autowired
    private ApplicationProperties appProperties;
    private String modifyDateStartPath;
    private String modifyDateEndPath;
    private String modifySpecialRequestPath;
    private String deleteSpecialRequestPath;
    private String modifyCommentPath;
    private String modifyRoomTypeIdPath;
    private String modifyProgramIdPath;
    private String modifyGuaranteeCodePath;
    private String modifyGuestCountsPath;
    private String modifyProfileTitlePath;
    private String modifyProfileFirstNamePath;
    private String modifyProfileLastNamePath;
    private String modifyProfilePhoneTypePath;
    private String modifyProfilePhoneNumberPath;
    private String modifyProfileEmailPath;
    private String modifyProfileEmail2Path;
    private String modifyProfileMlifeNoPath;
    private String modifyProfileStreetPath;
    private String modifyProfileCityPath;
    private String modifyProfileStatePath;
    private String modifyProfilePostalCodePath;
    private String modifyProfileCountryPath;
    private String modifyBillAmountPath;
    private String modifyCardHolderPath;
    private String modifyCardNumberPath;
    private String modifyCvvPath;
    private String modifyExpiryPath;
    private String modifyTypePath;
    private String modifyBillStreet1Path;
    private String modifyBillStreet2Path;
    private String modifyBillCityPath;
    private String modifyBillStatePath;
    private String modifyBillPostalCodePath;
    private String modifyBillCountryPath;
    private String modifyForcedSellPath;
    private String modifyPaymentInfoPath;
    private String modifyAddOnPaymentInfoPath;
    private String modifyPaymentInfoAmountPath;
    private String modifyDepositPaymentsPath;
    private String modifyStreet1Path;
    private String modifyStreet2Path;
    private String modifyCityPath;
    private String modifyStatePath;
    private String modifyPostalCodePath;
    private String modifyCountryPath;
    private String modifyProfileTierPath;
    private String modifyProfileDominancePath;
    private String modifyRoutingInstructionsPath;
    private String deleteAllManualRIProductUsePath;
    private String addComponentPath;
    private String modifyPerpetualOfferPath;
    private String updateComponentStatusPath;
    private String updateComponentCancelReasonPath;
    private String updateComponentCheckedInPath;
    private String updateComponentCheckedOutPath;
    private String modifyPartyConfirmationNumberPath;
    private String modifyExtConfirmationNumberPath;
    private String modifyCustomDataPath;
    private String addProductUsePath;
    private String deleteProductUsePath;
    private String modifyRequestedRatesPath;
    private String modifyAlertsPath;
    private String modifyTracesPath;
    private String modifyGroupCodePath;
    private String addProfileAdditionalMembershipPath;
    private String environment;
    private String chainCode;
    private String reservationsVersion;
    private String availabilityVersion;
    private String organizationVersion;
    private String searchVersion;
    private String groupSearchVersion;
    private String iceUser;
    private String profileUser;
    private String amaApiReservationsVersion;
    private boolean liveCRS;
    private boolean liveCRSIata;
    private boolean promoFeedActivated;
    private boolean enableOAuth2;
	private boolean activeCustomerOperaId;
	private String oauth2Version;
    private Integer maxAcrsCommentLength;

	private String defaultBasePriceRatePlan;
	private String defaultSearchOfferRatePlanCode;
	private Map<String, String> basePriceRatePlan = new HashMap<>();
	private Map<String, String> searchOfferRatePlanCode = new HashMap<>();
	private List<String> operaReservationReferenceTypes = new ArrayList<>();
    private Set<String> allowedTitleList = new HashSet<>();

	private Map<String,List<TaxDetails>> acrsPropertyTaxCodeMap = new HashMap<String,List<TaxDetails>>() ;
	private Map<String, List<String>> acrsPropertyTaxCodeExceptionMap = new HashMap<String, List<String>>();
	private Map<String,List<ChargeDetails>> acrsPropertyChargeCodeMap = new HashMap<String,List<ChargeDetails>>();
    private Map<SegmentPmsStatus, String> segmentPmsStateOperaStateMap = new HashMap<SegmentPmsStatus, String>();
	private Map<String,PaymentType> paymentTypeGuaranteeCodeMap = new HashMap<>();
    private String acrsPropertyListSecretKey;
    private List<String> pseudoExceptionProperties;
    private int maxPropertiesForResortPricing;
    private List<String> suppresWebComponentPatterns;
    private String iataSimpleFiltersKey;
    private String petDisabledKey;

    private List<String> whiteListMarketCodeList =new ArrayList<>();
    private List<String> packageComponentCodes;
    private String nonRoomInventoryType;

	public String getBaseRatePlan(String propertyId) {
		if(basePriceRatePlan.containsKey(propertyId.toUpperCase())) {
			return basePriceRatePlan.get(propertyId.toUpperCase());
		}
		return defaultBasePriceRatePlan;
	}

	public String getSearchOfferRatePlan(String propertyId) {
		if(searchOfferRatePlanCode.containsKey(propertyId.toUpperCase())) {
			return searchOfferRatePlanCode.get(propertyId.toUpperCase());
		}
		return defaultSearchOfferRatePlanCode;
	}

    @PostConstruct
    private void postConstruct() {
        acrsPropertyListSecretKey = String.format(appProperties.getAcrsPropertyListSecretKey(), appProperties.getRbsEnv());
        final String acrsPropertyTaxCodesCodeListStr = System.getenv("acrsPropertyTaxCodeList");
        final String acrsPropertyTaxCodeExceptionListStr = System.getenv("acrsPropertyTaxCodeExceptionList");
        final String acrsPropertyChargeCodeListStr = System.getenv("acrsPropertyChargeCodeList");
        petDisabledKey = String.format(petDisabledKey, appProperties.getRbsEnv());
        final ObjectMapper mapper = CommonUtil.getMapper();

        // set property charges map
        if (StringUtils.isNotEmpty(acrsPropertyChargeCodeListStr)) {
            try {
                final List<ACRSChargeDetail> acrsChargeDetailList = mapper.readValue(acrsPropertyChargeCodeListStr,
                        new TypeReference<List<ACRSChargeDetail>>() {
                        });
                if (!CollectionUtils.isEmpty(acrsChargeDetailList)) {
                    acrsPropertyChargeCodeMap = acrsChargeDetailList.stream()
                            .collect(Collectors.toMap(ACRSChargeDetail::getPropertyCode, ACRSChargeDetail::getCharges));
                }
            } catch (JsonProcessingException e) {
                log.error("Fatal Error while parsing the acrsPropertyChargeCodeList env variable.");
                throw new SystemException(ErrorCode.SYSTEM_ERROR, null);
            }
        }
        // set property taxs map
        if (StringUtils.isNotEmpty(acrsPropertyTaxCodesCodeListStr)) {
            try {
                final List<ACRSTaxDetail> acrsPropertyTaxCodesCodeList = mapper.readValue(acrsPropertyTaxCodesCodeListStr,
                        new TypeReference<List<ACRSTaxDetail>>() {
                        });
                if (!CollectionUtils.isEmpty(acrsPropertyTaxCodesCodeList)) {
                    acrsPropertyTaxCodeMap = acrsPropertyTaxCodesCodeList.stream()
                            .collect(Collectors.toMap(ACRSTaxDetail::getPropertyCode, ACRSTaxDetail::getTaxes));
                }
            } catch (JsonProcessingException e) {
                log.error("Fatal Error while parsing the acrsPropertyTaxCodesCodeListStr env variable.");
                throw new SystemException(ErrorCode.SYSTEM_ERROR, null);
            }
        }
        // set property exceptionTax code map.
        if (StringUtils.isNotEmpty(acrsPropertyTaxCodeExceptionListStr)) {
            try {
                final List<TaxCodeExceptionDetails> acrsPropertyTaxCodeExceptionList = mapper.readValue(
                        acrsPropertyTaxCodeExceptionListStr, new TypeReference<List<TaxCodeExceptionDetails>>() {
                        });
                if (!CollectionUtils.isEmpty(acrsPropertyTaxCodeExceptionList)) {
                    acrsPropertyTaxCodeExceptionMap = acrsPropertyTaxCodeExceptionList.stream().collect(Collectors
                            .toMap(TaxCodeExceptionDetails::getPropertyCode, ReservationUtil::getTaxExceptions));
                }
            } catch (JsonProcessingException e) {
                log.error("Fatal Error while parsing the acrsPropertyTaxCodeExceptionListStr env variable.");
                throw new SystemException(ErrorCode.SYSTEM_ERROR, null);
            }
        }
        // CBSR-934
        // create ACRS segment pms status - OPERA reservation status map.
        segmentPmsStateOperaStateMap.put(SegmentPmsStatus.IN_HOUSE, ServiceConstant.INHOUSE);
        segmentPmsStateOperaStateMap.put(SegmentPmsStatus.NO_SHOW, ServiceConstant.NOSHOW);
        segmentPmsStateOperaStateMap.put(SegmentPmsStatus.CHECKED_OUT, ServiceConstant.CHECKEDOUT);
        segmentPmsStateOperaStateMap.put(SegmentPmsStatus.WAITLISTED, ServiceConstant.WAITLISTED);
        segmentPmsStateOperaStateMap.put(SegmentPmsStatus.PRE_CHECKIN, ServiceConstant.RESERVED_STRING);
        // guaranteeCode payment Type Map
        //30
        paymentTypeGuaranteeCodeMap.put("MA", PaymentType.NUMBER_30);
        paymentTypeGuaranteeCodeMap.put("DB", PaymentType.NUMBER_30);
        paymentTypeGuaranteeCodeMap.put("GM", PaymentType.NUMBER_30);
        //44
        paymentTypeGuaranteeCodeMap.put("CG", PaymentType.NUMBER_44);
        paymentTypeGuaranteeCodeMap.put("DR", PaymentType.NUMBER_44);
        paymentTypeGuaranteeCodeMap.put("SH", PaymentType.NUMBER_44);
        paymentTypeGuaranteeCodeMap.put("SC", PaymentType.NUMBER_44);
        paymentTypeGuaranteeCodeMap.put("CO", PaymentType.NUMBER_44);
        paymentTypeGuaranteeCodeMap.put("8P", PaymentType.NUMBER_44);
        paymentTypeGuaranteeCodeMap.put("4P", PaymentType.NUMBER_44);
        paymentTypeGuaranteeCodeMap.put("OW", PaymentType.NUMBER_44);
        paymentTypeGuaranteeCodeMap.put("OF", PaymentType.NUMBER_44);
        paymentTypeGuaranteeCodeMap.put("WK", PaymentType.NUMBER_44);
        paymentTypeGuaranteeCodeMap.put("DI", PaymentType.NUMBER_44);
        paymentTypeGuaranteeCodeMap.put("EX", PaymentType.NUMBER_44);
        paymentTypeGuaranteeCodeMap.put("6P", PaymentType.NUMBER_44);
        paymentTypeGuaranteeCodeMap.put("WC", PaymentType.NUMBER_44);
        paymentTypeGuaranteeCodeMap.put("CS", PaymentType.NUMBER_44);
        paymentTypeGuaranteeCodeMap.put("FF", PaymentType.NUMBER_44);
        paymentTypeGuaranteeCodeMap.put("LT", PaymentType.NUMBER_44);
        //CBSR-2406
        final String whiteListMarketCodes = System.getenv("whiteListMarketCodes");
        if(StringUtils.isNotBlank(whiteListMarketCodes)){
            String[] marketCodeArr = whiteListMarketCodes.split(",");
            whiteListMarketCodeList.addAll(Arrays.asList(marketCodeArr));
        }

	}
}
