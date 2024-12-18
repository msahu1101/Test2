package com.mgm.services.booking.room.transformer;

import com.mgm.services.booking.room.model.request.RoomProgramValidateRequest;
import com.mgm.services.booking.room.model.response.ApplicableProgramsResponse;
import com.mgm.services.booking.room.model.response.CustomerOfferResponse;
import com.mgm.services.booking.room.model.response.ENRRatePlanSearchResponse;
import com.mgm.services.booking.room.model.response.RoomProgramValidateResponse;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

import java.text.Format;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@RunWith(MockitoJUnitRunner.class)
public class RoomRatePlanResponseTransformerTest {

    @Test
    public void getRoomRatePlanResponseTest() {

        final List<ENRRatePlanSearchResponse> enrRatePlanResponses = createEnrRatePlanResponses();
        final ApplicableProgramsResponse roomRatePlanResponse = RoomRatePlanResponseTransformer.getRoomRatePlanResponse(
                null, enrRatePlanResponses.toArray(new ENRRatePlanSearchResponse[0]), "80665010");
        Assert.assertEquals(2, roomRatePlanResponse.getProgramIds().size());
        Assert.assertEquals(2, roomRatePlanResponse.getPrograms().size());
        Assert.assertEquals(enrRatePlanResponses.get(0).getRateCode(), roomRatePlanResponse.getPrograms().get(0).getRateCode());
        Assert.assertEquals(enrRatePlanResponses.get(1).getRateCode(), roomRatePlanResponse.getPrograms().get(1).getRateCode());

    }

    @Test
    public void getCustomerOfferTest() {

        final List<ENRRatePlanSearchResponse> enrRatePlanResponses = createEnrRatePlanResponses();
        final CustomerOfferResponse customerOfferRatePlanResponse = RoomRatePlanResponseTransformer.getCustomerOfferRatePlanResponse(
                enrRatePlanResponses.toArray(new ENRRatePlanSearchResponse[0]), 0, "80665010");
        Assert.assertEquals(2, customerOfferRatePlanResponse.getOffers().size());
        Assert.assertEquals(enrRatePlanResponses.get(0).getRatePlanId(), customerOfferRatePlanResponse.getOffers().get(0).getId());
        Assert.assertEquals(enrRatePlanResponses.get(1).getRatePlanId(), customerOfferRatePlanResponse.getOffers().get(1).getId());

    }

    @Test
    public void getValidateProgramResponse() {

        final RoomProgramValidateRequest request = new RoomProgramValidateRequest();
        request.setProgramId("f115f057-0b8e-4a7c-a183-e0e37a8441c6");
        request.setPropertyId("propertyId");
        final List<ENRRatePlanSearchResponse> enrRatePlanResponses = createEnrRatePlanResponses();
        RoomProgramValidateResponse response = RoomRatePlanResponseTransformer.getValidateProgramResponse(request, enrRatePlanResponses.toArray(new ENRRatePlanSearchResponse[0]), true);
        Assert.assertTrue(response.isValid());
        Assert.assertTrue(response.isEligible());
        Assert.assertFalse(response.isExpired());

        request.setPropertyId("RPCD-v-PREVL-d-PROP-v-MV021");
        response = RoomRatePlanResponseTransformer.getValidateProgramResponse(request, enrRatePlanResponses.toArray(new ENRRatePlanSearchResponse[0]), true);
        Assert.assertTrue(response.isValid());
        Assert.assertTrue(response.isEligible());
        Assert.assertFalse(response.isExpired());

        request.setPropertyId("RPCD-v-PREVL-d-PROP-v-MV021");
        request.setPromoCode("1NFREE");
        enrRatePlanResponses.stream().forEach(x -> x.setPromo(request.getPromoCode()));
        response = RoomRatePlanResponseTransformer.getValidateProgramResponse(request, enrRatePlanResponses.toArray(new ENRRatePlanSearchResponse[0]), true);
        Assert.assertTrue(response.isValid());
        Assert.assertTrue(response.isEligible());
        Assert.assertFalse(response.isExpired());
        Assert.assertNotNull(response.getProgramId());
    }

    private List<ENRRatePlanSearchResponse> createEnrRatePlanResponses() {
        final List<ENRRatePlanSearchResponse> enrRatePlanSearchResponses = new ArrayList<ENRRatePlanSearchResponse>();
        final ENRRatePlanSearchResponse ratePlan1 = new ENRRatePlanSearchResponse();
        ratePlan1.setRatePlanId("id1");
        ratePlan1.setPropertyId("propertyId");
        ratePlan1.setPropertyCode("MV021");
        ratePlan1.setRateCode("RP1");
        ratePlan1.setName(ratePlan1.getRateCode());
        ratePlan1.setStatus("Update");
        ratePlan1.setLoyaltyNumberRequired(false);
        Format formatter = new SimpleDateFormat("yyyy-MM-dd");
        String date = formatter.format(new Date());
        ratePlan1.setBookingEndDate(date);
        ratePlan1.setBookingStartDate(date);
        ratePlan1.setTravelStartDate(date);
        ratePlan1.setTravelEndDate(date);
        enrRatePlanSearchResponses.add(ratePlan1);

        final ENRRatePlanSearchResponse ratePlan2 = new ENRRatePlanSearchResponse();
        ratePlan2.setRatePlanId("id2");
        ratePlan2.setPropertyId("propertyId");
        ratePlan2.setPropertyCode("MV021");
        ratePlan2.setRateCode("RP2");
        ratePlan2.setName(ratePlan2.getRateCode());
        ratePlan2.setStatus("Update");
        ratePlan2.setLoyaltyNumberRequired(false);
        ratePlan2.setBookingEndDate(date);
        ratePlan2.setBookingStartDate(date);
        ratePlan2.setTravelStartDate(date);
        ratePlan2.setTravelEndDate(date);
        enrRatePlanSearchResponses.add(ratePlan2);

        return enrRatePlanSearchResponses;
    }


}
