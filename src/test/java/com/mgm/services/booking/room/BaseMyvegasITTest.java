package com.mgm.services.booking.room;

import java.io.IOException;
import java.util.Arrays;
import java.util.stream.Collectors;

import org.junit.BeforeClass;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;

import com.mgm.services.booking.room.constant.ServiceConstant;
import com.mgm.services.booking.room.constant.TestConstant;
import com.mgm.services.booking.room.model.MyvegasTestData;
import com.mgm.services.booking.room.model.response.TokenResponse;
import com.mgm.services.booking.room.validator.MyVegasTokenScopes;

public abstract class BaseMyvegasITTest extends BaseRoomBookingV2IntegrationTest {

    private static final String myvegasTestDataFileName = "/myvegas-data.json";
    protected static MyvegasTestData myvegasTestData;
    protected static WebClient guestAuthClient;

    @BeforeClass
    public static void myvegasSetup() throws IOException {
        setup();
        myvegasTestData = getObjectFromJSON(myvegasTestDataFileName, MyvegasTestData.class);
        guestAuthClient = WebClient.builder().baseUrl(myvegasTestData.getGuestIdentityUrl()).build();
    }

    protected String createGuestToken() {
        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        body.add(TestConstant.HEADER_KEY_USERNAME, myvegasTestData.getMyvegasGuestIdentityUsername());
        body.add(TestConstant.HEADER_KEY_PASSWORD, myvegasTestData.getMyvegasGuestIdentityPassword());
        body.add(ServiceConstant.HEADER_GRANT_TYPE, myvegasTestData.getGrantTypePassword());
        // if scope param null, then scope picked from env variable.
        body.add(ServiceConstant.HEADER_SCOPE, Arrays.stream(MyVegasTokenScopes.values())
                .map(MyVegasTokenScopes::getValue).collect(Collectors.joining(ServiceConstant.WHITESPACE_STRING)));
        TokenResponse result = guestAuthClient.post().body(BodyInserters.fromFormData(body)).headers(headers -> {
            headers.add(ServiceConstant.HEADER_CONTENT_TYPE, ServiceConstant.CONTENT_TYPE_URLENCODED);
            headers.add(ServiceConstant.HEADER_AUTHORIZATION, myvegasTestData.getBasicAuthToken());
        }).exchange().flatMap(clientResponse -> clientResponse.bodyToMono(TokenResponse.class)).log().block();
        return result.getAccessToken();

    }

}
