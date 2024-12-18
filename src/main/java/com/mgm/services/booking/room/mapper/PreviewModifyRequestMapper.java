package com.mgm.services.booking.room.mapper;

import org.mapstruct.Mapper;

import com.mgm.services.booking.room.model.request.PreModifyV2Request;
import com.mgm.services.booking.room.model.request.PreviewCommitRequest;

@Mapper(componentModel = "spring")
public interface PreviewModifyRequestMapper {

    public abstract PreModifyV2Request commitToPreviewRequest(PreviewCommitRequest request);
}
