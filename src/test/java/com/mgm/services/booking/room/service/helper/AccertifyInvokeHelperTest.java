/**
 * 
 */
package com.mgm.services.booking.room.service.helper;

import com.mgm.services.booking.room.properties.SecretsProperties;
import org.springframework.http.HttpHeaders;

import com.mgm.services.common.model.authorization.Products;
import com.mgm.services.common.model.authorization.RoomDetail;

import com.mgm.services.booking.room.BaseRoomBookingTest;
import com.mgm.services.booking.room.constant.ServiceConstant;
import com.mgm.services.booking.room.dao.AccertifyDAO;
import com.mgm.services.booking.room.dao.helper.ReferenceDataDAOHelper;
import com.mgm.services.booking.room.dao.impl.ProgramContentDAOImpl;
import com.mgm.services.booking.room.dao.impl.RoomContentDAOImpl;
import com.mgm.services.booking.room.mapper.RoomReservationRequestMapper;
import com.mgm.services.booking.room.model.content.Room;
import com.mgm.services.booking.room.model.request.CreateRoomReservationRequest;
import com.mgm.services.booking.room.model.request.RoomPaymentDetailsRequest;
import com.mgm.services.booking.room.model.request.RoomReservationRequest;
import com.mgm.services.booking.room.model.reservation.RoomReservation;
import com.mgm.services.booking.room.properties.ApplicationProperties;
import com.mgm.services.booking.room.service.cache.impl.PropertyContentCacheServiceImpl;
import com.mgm.services.booking.room.service.impl.AccertifyServiceImpl;
import com.mgm.services.common.model.authorization.AuthorizationTransactionDetails;
import com.mgm.services.common.model.authorization.AuthorizationTransactionRequest;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mapstruct.factory.Mappers;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.servlet.http.HttpServletRequest;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * @author laknaray
 *
 */
@RunWith(MockitoJUnitRunner.class)

public class AccertifyInvokeHelperTest extends BaseRoomBookingTest {

    @InjectMocks
    private AccertifyInvokeHelper accertifyInvokeHelper;

    @InjectMocks
    private RoomReservationRequestMapper requestMapper = Mappers.getMapper(RoomReservationRequestMapper.class);

    @Mock
    private ApplicationProperties appProperties;

    @InjectMocks
    private AccertifyServiceImpl transactionService;
       
    @Mock
    private RoomContentDAOImpl roomContentDao;

    @Mock
    private ProgramContentDAOImpl programContentDao;

    @Mock
    private PropertyContentCacheServiceImpl propertyCacheService;

    @Mock
    private ReferenceDataDAOHelper referenceDataDAOHelper;
    
    @Mock
    private static RestTemplate client;
    
    @Mock
    private HttpHeaders headersMock;
    
    @Mock
    private AccertifyDAO accertifyDaoMock;
    
    @Mock
    private RestTemplate restTemplateMock;
    @Mock
    private SecretsProperties secretProperties;

    @Before
    public void setup() {
        HttpServletRequest request = Mockito.mock(HttpServletRequest.class);
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));

        when(request.getHeader(ServiceConstant.HEADER_CHANNEL)).thenReturn("ice");

        ReflectionTestUtils.setField(accertifyInvokeHelper, "afsCheckEnabled", true);

        ReflectionTestUtils.setField(accertifyInvokeHelper, "appProperties", appProperties);
        ReflectionTestUtils.setField(accertifyInvokeHelper, "secretProperties", secretProperties);
       // ReflectionTestUtils.setField(accertifyInvokeHelper, "acrsProp", acrsProp);
        
        ReflectionTestUtils.setField(transactionService, "roomContentDao", roomContentDao);
        ReflectionTestUtils.setField(transactionService, "programContentDao", programContentDao);
        ReflectionTestUtils.setField(transactionService, "propertyCacheService", propertyCacheService);
        ReflectionTestUtils.setField(accertifyInvokeHelper, "transactionService", transactionService);
    }
    
    private <T> T getObject(String fileName, Class<T> target) {
        File file = new File(getClass().getResource(fileName).getPath());
        return convert(file, target);
    }

    @Test
    public void test_performAFSCheck_withChannelNotInBypassList_returnTure() {
        String[] channels = {"web"};
        when(appProperties.getBypassAfsChannels()).thenReturn(Arrays.asList(channels));

        List<RoomPaymentDetailsRequest> billing = new ArrayList<>();
        billing.add(new RoomPaymentDetailsRequest());

        RoomReservationRequest reservationRequest = new RoomReservationRequest();
        reservationRequest.setBilling(billing);
        
        assertTrue(accertifyInvokeHelper.performAFSCheck(reservationRequest));        
    }

    @Test
    public void test_performAFSCheck_withChannelInBypassList_returnFalse() {
        String[] channels = {"ice"};
        when(appProperties.getBypassAfsChannels()).thenReturn(Arrays.asList(channels));

        List<RoomPaymentDetailsRequest> billing = new ArrayList<>();
        billing.add(new RoomPaymentDetailsRequest());

        RoomReservationRequest reservationRequest = new RoomReservationRequest();
        reservationRequest.setBilling(billing);
        
        assertFalse(accertifyInvokeHelper.performAFSCheck(reservationRequest));        
    }

    @Test
    public void test_performAFSCheck_withChannelNotInBypassList_withoutBilling_returnFalse() {
        String[] channels = {"web"};
        when(appProperties.getBypassAfsChannels()).thenReturn(Arrays.asList(channels));

        RoomReservationRequest reservationRequest = new RoomReservationRequest();
        
        assertFalse(accertifyInvokeHelper.performAFSCheck(reservationRequest));        
    }
    
    @Test
    public void test_performAFSCheck_withZeroCharges_returnFalse() {
    	RoomReservation resv = getObject("/modifyreservationdao-modifyReservation-response.json",
                RoomReservation.class);

        
        assertFalse(accertifyInvokeHelper.performAFSCheck(resv));        
    }
    
    @Test
    public void test_createAuthorizeRequest_withMultipleCC_returnAuthorizationTransactionRequestWithMultiplePaymentMethods() {
        CreateRoomReservationRequest createRoomReservationRequest = convert(
                new File(getClass().getResource("/createroomreservationrequest-multiplecc.json").getPath()),
                CreateRoomReservationRequest.class);

        when(referenceDataDAOHelper.isPropertyManagedByAcrs(Mockito.anyString())).thenReturn(true);

        RoomReservation roomReservation = requestMapper.roomReservationRequestToModel(createRoomReservationRequest.getRoomReservation());
        
        when(appProperties.getTimezone(Mockito.anyString())).thenReturn("America/Los_Angeles");

        Room room = new Room();
        room.setName("Grand King");
        room.setId("23f5bef8-63ea-4ba9-a290-13b5a3056595");        
        when(roomContentDao.getRoomContent(Mockito.anyString())).thenReturn(room);
        
        AuthorizationTransactionRequest authorizeRequest = accertifyInvokeHelper.createAuthorizeRequest(roomReservation, null);
        assertEquals(2, authorizeRequest.getTransaction().getBilling().getPaymentMethods().size());        
    }
    
    @Test
    public void test_createAuthorizeRequest_withPartyReservation_returnAuthorizationTransactionRequestWithMultipleProducts() {
        CreateRoomReservationRequest createRoomReservationRequest = convert(
                new File(getClass().getResource("/createpartyroomreservationrequest-basic.json").getPath()),
                CreateRoomReservationRequest.class);

        when(referenceDataDAOHelper.isPropertyManagedByAcrs(Mockito.anyString())).thenReturn(true);
        RoomReservation roomReservation = requestMapper.roomReservationRequestToModel(createRoomReservationRequest.getRoomReservation());
        
        when(appProperties.getTimezone(Mockito.anyString())).thenReturn("America/Los_Angeles");

        Room room = new Room();
        room.setName("Grand King");
        room.setId("23f5bef8-63ea-4ba9-a290-13b5a3056595");        
        when(roomContentDao.getRoomContent(Mockito.anyString())).thenReturn(room);
        
        AuthorizationTransactionRequest authorizeRequest = accertifyInvokeHelper.createAuthorizeRequest(roomReservation, null);
        assertEquals(roomReservation.getNumRooms(), authorizeRequest.getTransaction().getProducts().getRooms().size());        
    }
    
    @Test
    public void confirm_shouldSetConfirmationNumbersAndCallTransactionServiceConfirm() {
    	// Arrange
    	AuthorizationTransactionRequest authorizeRequest = new AuthorizationTransactionRequest();
    	AuthorizationTransactionDetails authorizeDetails = new AuthorizationTransactionDetails();
    	authorizeDetails.setBookingType("LasVegas");
    	Products products = new Products();
    	List<RoomDetail> roomDetaillist = new ArrayList<>();
    	RoomDetail roomDetail1 = new RoomDetail();
    	roomDetail1.setRoomId("1");
    	RoomDetail roomDetail2 = new RoomDetail();
    	roomDetail2.setRoomId("2");
    	roomDetaillist.add(roomDetail1);
    	roomDetaillist.add(roomDetail2);        
    	products.setRooms(roomDetaillist);
    	authorizeDetails.setProducts(products);
    	authorizeRequest.setTransaction(authorizeDetails);      
    	String confirmationNumbers = "123,456";

    	// Act
    	accertifyInvokeHelper.confirm(authorizeRequest, confirmationNumbers, headersMock);

    	// Assert
    	ArgumentCaptor<AuthorizationTransactionRequest> requestCaptor = ArgumentCaptor.forClass(AuthorizationTransactionRequest.class);
    	verify(accertifyDaoMock).confirm(requestCaptor.capture(), eq(headersMock));

    	AuthorizationTransactionRequest capturedRequest = requestCaptor.getValue();
    	assertEquals(confirmationNumbers, capturedRequest.getTransaction().getConfirmationNumbers());
    }
    
    @Test
    public void confirmAsyncCall_shouldSetUpHeadersAndCallConfirmAsync() {
    	// Arrange
    	AuthorizationTransactionRequest authorizeRequest = new AuthorizationTransactionRequest();
    	AuthorizationTransactionDetails authorizeDetails = new AuthorizationTransactionDetails();
    	authorizeDetails.setBookingType("LasVegas");
    	Products products = new Products();
    	List<RoomDetail> roomDetailList = new ArrayList<>();
    	RoomDetail roomDetail1 = new RoomDetail();
    	roomDetail1.setRoomId("1");
    	RoomDetail roomDetail2 = new RoomDetail();
    	roomDetail2.setRoomId("2");
    	roomDetailList.add(roomDetail1);
    	roomDetailList.add(roomDetail2);
    	products.setRooms(roomDetailList);
    	authorizeDetails.setProducts(products);
    	authorizeRequest.setTransaction(authorizeDetails);
    	String confirmationNumber = "123,456";

    	// Act and Assert
    	assertDoesNotThrow(() -> accertifyInvokeHelper.confirmAsyncCall(authorizeRequest, confirmationNumber));
    	Assert.assertNotNull(authorizeRequest);     
    }
}
