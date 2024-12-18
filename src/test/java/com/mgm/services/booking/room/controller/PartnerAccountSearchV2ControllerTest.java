package com.mgm.services.booking.room.controller;
import static org.junit.Assert.assertNotNull;

import static org.mockito.Mockito.mock;
import java.util.ArrayList;
import java.util.List;
import javax.servlet.http.HttpServletRequest;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.validation.BindingResult;
import org.springframework.validation.DirectFieldBindingResult;
import com.mgm.services.booking.room.BaseRoomBookingTest;
import com.mgm.services.booking.room.constant.TestConstant;
import com.mgm.services.booking.room.model.PartnerAccountDetails;
import com.mgm.services.booking.room.model.PartnerAccounts;
import com.mgm.services.booking.room.model.request.PartnerAccountV2Request;
import com.mgm.services.booking.room.model.response.PartnerAccountsSearchV2Response;
import com.mgm.services.booking.room.service.PartnerService;
import com.mgm.services.common.model.ServicesSession;

 

@RunWith(MockitoJUnitRunner.class)
public class PartnerAccountSearchV2ControllerTest extends BaseRoomBookingTest{

 

    @InjectMocks
    private PartnerController partnerAccountSearchV2Controller;

    @Mock
    protected ServicesSession sSession;

    @Mock
    private PartnerService partnerAccountSearchV2Service;
 

    @Test
    public void partnerAccountSearch_Success() {
        PartnerAccountV2Request req= buildPartnerSearchReq();
        HttpServletRequest mockRequest = mock(HttpServletRequest.class);
        BindingResult result = new DirectFieldBindingResult(req, "PartnerAccountV2Request");
        Mockito.when(partnerAccountSearchV2Service.searchPartnerAccount(Mockito.any())).thenReturn(buildPartnerSearchResponse());
        PartnerAccountsSearchV2Response finalRes =partnerAccountSearchV2Controller.searchPartnerAccount(TestConstant.WEB, buildPartnerSearchReq(), result, mockRequest);
        assertNotNull(finalRes);
    }

 

    private PartnerAccountV2Request buildPartnerSearchReq() {
        PartnerAccountV2Request req = PartnerAccountV2Request.builder()
                .firstName("john")
                .lastName("Doe")
                .emailAddress("john.doe@mail.com")
                .partnerAccountNo("1234556")
                .programCode("bv")
                .build();
        return req;
    }

 

    private PartnerAccountsSearchV2Response buildPartnerSearchResponse() {

        PartnerAccountsSearchV2Response res = PartnerAccountsSearchV2Response.builder()
                .partnerAccountDetails(PartnerAccountDetails.builder()
                        .firstName("John")
                        .lastName("Doe")
                        .partnerAccounts(buildNewPartnerAccountList()).build()).build();
        return res;
    }

    private List<PartnerAccounts> buildNewPartnerAccountList() {
        List<PartnerAccounts> acc = new ArrayList<PartnerAccounts>();
        acc.add(PartnerAccounts.builder()
                .partnerAccountNo("12345")
                .membershipLevel("member")
                .build());
        return acc;
    }

}