package com.mgm.services.booking.room.dao.impl;

import com.mgm.services.booking.room.BaseRoomBookingTest;
import com.mgm.services.booking.room.dao.IDMSTokenDAO;
import com.mgm.services.booking.room.dao.helper.ReferenceDataDAOHelper;
import com.mgm.services.booking.room.model.phoenix.RoomProgram;
import com.mgm.services.booking.room.model.request.GroupSearchV2Request;
import com.mgm.services.booking.room.model.request.dto.ApplicableProgramRequestDTO;
import com.mgm.services.booking.room.model.response.ApplicableProgramsResponse;
import com.mgm.services.booking.room.model.response.ENRRatePlanSearchResponse;
import com.mgm.services.booking.room.model.response.GroupSearchV2Response;
import com.mgm.services.booking.room.model.response.TokenResponse;
import com.mgm.services.booking.room.properties.AcrsProperties;
import com.mgm.services.booking.room.properties.ApplicationProperties;
import com.mgm.services.booking.room.properties.DomainProperties;
import com.mgm.services.booking.room.properties.URLProperties;
import com.mgm.services.booking.room.service.cache.RoomProgramCacheService;
import com.mgm.services.booking.room.service.cache.impl.RoomProgramCacheServiceImpl;
import com.mgm.services.booking.room.util.CommonUtil;
import com.mgmresorts.aurora.messages.GetApplicableProgramsResponse;
import com.mgmresorts.aurora.service.Client;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentMatchers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import javax.net.ssl.SSLException;
import java.io.File;
import java.text.ParseException;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.Silent.class)
public class GroupSearchDAOStrategyGSEImplTest extends BaseRoomBookingTest {

    @Mock
    Client client;

    @Mock
    RoomProgramCacheService roomProgramCacheService;

    /**
     * Check getApplicableProgramsTest.
     */
    @Test
    public void groupSearchTest() {

        try {
            final GetApplicableProgramsResponse response = GetApplicableProgramsResponse.create();
            final GroupSearchDAOStrategyGSEImpl daoImpl = new GroupSearchDAOStrategyGSEImpl() {
                @Override
                public Client getAuroraClient(String propertyId) {
                    return client;
                }

                @Override
                public RoomProgramCacheService getRoomProgramCacheService() {
                    return roomProgramCacheService;
                }
            };

            final RoomProgram program = new RoomProgram();
            program.setCategory("Group");
            program.setId("id");
            program.setName("name");
            response.addProgramIds(program.getId());

            when(client.getApplicablePrograms(any())).thenReturn(response);
            when(roomProgramCacheService.getRoomProgram(anyString())).thenReturn(program);

            final GroupSearchV2Request request = new GroupSearchV2Request();
            List<GroupSearchV2Response> groupSearchV2Responses = daoImpl.searchGroup(request);
            Assert.assertNotNull(groupSearchV2Responses);
            Assert.assertEquals(program.getId(), groupSearchV2Responses.get(0).getId());
            Assert.assertEquals(program.getName(), groupSearchV2Responses.get(0).getName());

        } catch (Exception e) {
            Assert.fail("groupSearchTest Failed");
        }
    }
}
