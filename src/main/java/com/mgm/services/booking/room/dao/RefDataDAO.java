package com.mgm.services.booking.room.dao;

        import com.mgm.services.booking.room.model.refdata.RefDataEntitySearchRefReq;
        import com.mgm.services.booking.room.model.refdata.AlertAndTraceSearchRefDataRes;
        import com.mgm.services.booking.room.model.refdata.RoutingInfoRequest;
        import com.mgm.services.booking.room.model.refdata.RoutingInfoResponseList;
        import org.springframework.http.HttpEntity;

        import java.util.List;

public interface RefDataDAO {
    String getRoutingAuthAppUserId(HttpEntity<?> request, String phoenixId);

    String getRoutingAuthAppPhoenixId(HttpEntity<?> request, String appUserId);

    RoutingInfoResponseList getRoutingInfo(HttpEntity<List<RoutingInfoRequest>> request);

    //INC-4
    AlertAndTraceSearchRefDataRes searchRefDataEntity(RefDataEntitySearchRefReq alertAndTraceSearchRefDataReq);

    String getRoutingAuthPhoenixId(String authorizer, String propertyId);
}
