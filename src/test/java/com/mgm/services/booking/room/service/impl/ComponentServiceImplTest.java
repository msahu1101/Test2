package com.mgm.services.booking.room.service.impl;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.mgm.services.booking.room.service.helper.ReservationServiceHelper;
import java.io.File;
import java.io.IOException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.mgm.services.booking.room.BaseRoomBookingTest;
import com.mgm.services.booking.room.dao.ComponentDAO;
import com.mgm.services.booking.room.model.phoenix.Room;
import com.mgm.services.booking.room.model.phoenix.RoomComponent;
import com.mgm.services.booking.room.model.request.PackageComponentRequest;
import com.mgm.services.booking.room.model.request.RoomComponentRequest;
import com.mgm.services.booking.room.model.request.RoomComponentV2Request;
import com.mgm.services.booking.room.model.request.dto.NonRoomProducts;
import com.mgm.services.booking.room.model.request.dto.PackageComponentsRequestDTO;
import com.mgm.services.booking.room.model.request.dto.PkgRequestDTO;
import com.mgm.services.booking.room.model.request.dto.PkgDateDTO;
import com.mgm.services.booking.room.model.reservation.RoomRequest;
import com.mgm.services.booking.room.service.cache.RoomCacheService;

/**
 * Unit test class for CompoenntServiceImpl implementation class
 *
 */
@RunWith(MockitoJUnitRunner.class)
public class ComponentServiceImplTest extends BaseRoomBookingTest {

    @Mock
    private RoomCacheService roomCacheService;

    @Mock
    private ComponentDAO componentDao;
	@Mock
	private ReservationServiceHelper reservationServiceHelper;

    @InjectMocks
    private ComponentServiceImpl componentService;
   

    private List<RoomComponent> getComponents() {
        File file = new File(getClass().getResource("/components.json").getPath());

        return convert(file, mapper.getTypeFactory().constructCollectionType(List.class, RoomComponent.class));

    }

    /**
     * Test getAvailableRoomComponents service method.
     */
    @Test
    public void getAvailableRoomComponentsTest() {

        List<RoomRequest> roomReqList = new ArrayList<>();
        RoomRequest roomRequest = new RoomRequest();
        roomRequest.setId("COMPONENTCD-v-CFBP1-d-TYP-v-COMPONENT-d-PROP-v-MV021-d-NRPCD-v-CFBP1");
        roomRequest.setDescription("CFBP1");
        roomRequest.setNightlyCharge(true);
        roomRequest.setSelected(true);
        roomReqList.add(roomRequest);
        
        RoomRequest roomRequest1 = new RoomRequest();
        roomRequest1.setId("COMPONENTCD-v-CFBP2-d-TYP-v-COMPONENT-d-PROP-v-MV021-d-NRPCD-v-CFBP3");
        roomRequest1.setDescription("CFBP2");
        roomRequest1.setNightlyCharge(true);
        roomRequest1.setSelected(true);
        roomReqList.add(roomRequest1);

        when(componentDao.getRoomComponentAvailability(Mockito.any())).thenReturn(roomReqList);

        RoomComponentRequest componentRequest = new RoomComponentRequest();
        componentRequest.setRoomTypeId("5c69b832-4cc0-4241-b1cc-4d08d89e260d");
        componentRequest.setPropertyId("dc00e77f-d6bb-4dd7-a8ea-dc33ee9675ad");

        List<RoomRequest> response = componentService.getAvailableRoomComponents(componentRequest);

        assertEquals(roomReqList.get(0).getId(), response.get(0).getId());
        assertTrue(response.get(0).isNightlyCharge());
        assertTrue(response.get(0).isSelected());
        assertTrue(StringUtils.isNotEmpty(response.get(0).getDescription()));
    }

    /**
     * Test getRoomComponent service method.
     */
    @Test
    public void getRoomComponentTest() {

        Room room = new Room();
        room.setComponents(getComponents());
        when(roomCacheService.getRoom(Mockito.anyString())).thenReturn(room);

        RoomRequest response = componentService.getRoomComponent("5c69b832-4cc0-4241-b1cc-4d08d89e260d",
                "13c0f7cf-e371-415f-961c-1dcc384f6031");

        assertEquals("13c0f7cf-e371-415f-961c-1dcc384f6031", response.getId());
        assertEquals(24.0, response.getPrice(), 0);
        assertTrue(response.isNightlyCharge());
        assertTrue(response.isSelected());
        assertTrue(StringUtils.isNotEmpty(response.getDescription()));
    }
    


@Test
	public void getAvailablePackageComponentsTest() throws JsonParseException, JsonMappingException, IOException{

	/*	PackageComponentRequest packageComponentRequest = new PackageComponentRequest();

		PackageComponentsRequestDTO requestDTO = new PackageComponentsRequestDTO();
		PkgRequestDTO pkgData = new PkgRequestDTO();
		pkgData.setPropertyId("dc00e77f-d6bb-4dd7-a8ea-dc33ee9675ad");
		pkgData.setRoomTypeId("5c69b832-4cc0-4241-b1cc-4d08d89e260d");

		List<NonRoomProducts> roomProductsList = new ArrayList<NonRoomProducts>();
		NonRoomProducts roomProducts1 = new NonRoomProducts();
		roomProducts1.setCode("CFBP3");
		roomProducts1.setQty(1);
		roomProductsList.add(roomProducts1);

		NonRoomProducts roomProducts2 = new NonRoomProducts();
		roomProducts2.setCode("CFBP2");
		roomProducts2.setQty(2);
		roomProductsList.add(roomProducts2);

		pkgData.setNonRoomProducts(roomProductsList);   

		PkgDateDTO pkgDateDTO = new PkgDateDTO();
		pkgDateDTO.setCheckIn(LocalDate.of(2024, 8, 1));
		pkgDateDTO.setCheckOut(LocalDate.of(2024, 8, 10));


		requestDTO.setDates(Collections.singletonList(pkgDateDTO));
		requestDTO.setData(Collections.singletonList(pkgData));
		requestDTO.setNumAdults(2);
		packageComponentRequest.setRequest(requestDTO);

		List<RoomRequest> roomReqList = new ArrayList<>();
		RoomRequest roomRequest = new RoomRequest();
		roomRequest.setId("COMPONENTCD-v-CFBP2-d-TYP-v-COMPONENT-d-PROP-v-MV930-d-NRPCD-v-CFBP2");
		roomRequest.setDescription("CFBP2");
		roomRequest.setNightlyCharge(true);
		roomRequest.setSelected(true);
		roomRequest.setShortDescription("CFBP2");
		roomRequest.setPrice(32.24);
	    roomRequest.setAmtAftTax(50.0);
		roomRequest.setRatePlanCode("CFBP2");
		roomRequest.setTaxRate(5.0f);
		roomReqList.add(roomRequest);

		when(componentDao.getRoomComponentAvailability(Mockito.any())).thenReturn(roomReqList); 

		PropertyPackageComponentResponse pkgResponse = new PropertyPackageComponentResponse();
		List<com.mgm.services.booking.room.model.RoomComponent> pkgComponentsList= new ArrayList<>(); 
		pkgResponse.setPropertyId("dc00e77f-d6bb-4dd7-a8ea-dc33ee9675ad");
		pkgResponse.setPkgComponents(pkgComponentsList);
        
		List<PackageComponentResponse> response = componentService.getAvailablePackageComponents(packageComponentRequest);
		assertNotNull(response);
		assertEquals(pkgDateDTO,response.get(0).getDate());	
		
		PackageComponentResponse firstResponse = response.get(0);
		
		assertEquals(pkgDateDTO.getCheckIn(), firstResponse.getDate().getCheckIn());
		assertEquals(pkgDateDTO.getCheckOut(), firstResponse.getDate().getCheckOut());
		assertEquals("dc00e77f-d6bb-4dd7-a8ea-dc33ee9675ad",firstResponse.getPkgComponent().get(0).getPropertyId());
		assertEquals("COMPONENTCD-v-CFBP2-d-TYP-v-COMPONENT-d-PROP-v-MV930-d-NRPCD-v-CFBP2",firstResponse.getPkgComponent().get(0).getPkgComponents().get(0).getId());
		assertTrue(firstResponse.getPkgComponent().get(0).getPkgComponents().get(0).isNightlyCharge());
		double actualPrice = firstResponse.getPkgComponent().get(0).getPkgComponents().get(0).getPrice();
		assertEquals(32.24, actualPrice, 0.001);
	    double actualAmtAftTax = firstResponse.getPkgComponent().get(0).getPkgComponents().get(0).getAmtAftTax();
	    assertEquals(50.0, actualAmtAftTax, 0.001);
		assertEquals("CFBP2",firstResponse.getPkgComponent().get(0).getPkgComponents().get(0).getRatePlanCode());
*/
	}

	@Test
	public void getAvailableComponentsTest_Success() {
		RoomComponentV2Request request = new RoomComponentV2Request();

		request.setPropertyId("dc00e77f-d6bb-4dd7-a8ea-dc33ee9675ad");
		request.setRoomTypeId("5c69b832-4cc0-4241-b1cc-4d08d89e260d");
		request.setCheckInDate(LocalDate.of(2024, 8, 1));
		request.setCheckOutDate(LocalDate.of(2024, 8, 10));    

		List<RoomRequest> roomReqList = new ArrayList<>();
		RoomRequest roomRequest = new RoomRequest();
		roomRequest.setId("COMPONENTCD-v-CFBP2-d-TYP-v-COMPONENT-d-PROP-v-MV930-d-NRPCD-v-CFBP2");
		roomRequest.setDescription("CFBP2");
		roomReqList.add(roomRequest);
		when(componentDao.getRoomComponentAvailability(Mockito.any())).thenReturn(roomReqList);

		List<com.mgm.services.booking.room.model.RoomComponent> response = componentService.getAvailableRoomComponents(request);
		assertNotNull(response);
		assertEquals(1, response.size());

		com.mgm.services.booking.room.model.RoomComponent component = response.get(0);

		assertEquals("COMPONENTCD-v-CFBP2-d-TYP-v-COMPONENT-d-PROP-v-MV930-d-NRPCD-v-CFBP2", component.getId());
		assertEquals("CFBP2", component.getDescription());   
		assertFalse(component.isNightlyCharge());
	}

}
