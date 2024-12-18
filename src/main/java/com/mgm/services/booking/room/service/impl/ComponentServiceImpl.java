package com.mgm.services.booking.room.service.impl;

import com.mgm.services.booking.room.constant.ACRSConversionUtil;
import com.mgm.services.booking.room.constant.ServiceConstant;
import com.mgm.services.booking.room.dao.ComponentDAO;
import com.mgm.services.booking.room.exception.AuroraError;
import com.mgm.services.booking.room.model.phoenix.RoomComponent;
import com.mgm.services.booking.room.model.request.PackageComponentRequest;
import com.mgm.services.booking.room.model.request.PackageComponentRequestV1;
import com.mgm.services.booking.room.model.request.RoomComponentRequest;
import com.mgm.services.booking.room.model.request.RoomComponentV2Request;
import com.mgm.services.booking.room.model.request.dto.*;
import com.mgm.services.booking.room.model.reservation.RoomRequest;
import com.mgm.services.booking.room.model.response.*;
import com.mgm.services.booking.room.properties.ApplicationProperties;
import com.mgm.services.booking.room.service.ComponentService;
import com.mgm.services.booking.room.service.cache.RoomCacheService;
import com.mgm.services.booking.room.service.helper.ReservationServiceHelper;
import com.mgm.services.booking.room.transformer.RoomComponentRequestTransformer;
import com.mgm.services.booking.room.util.ReservationUtil;
import com.mgm.services.common.exception.BusinessException;
import com.mgm.services.common.exception.ErrorCode;
import com.mgm.services.common.exception.SystemException;
import com.mgm.services.common.util.ValidationUtil;
import com.mgmresorts.aurora.service.EAuroraException;
import org.apache.commons.collections.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;


/**
 * Implementation class for ComponentService exposing services for room requests
 * related reservation operations.
 *
 */
@Component
public class ComponentServiceImpl implements ComponentService {

    private static final String NIGHTLY = "NIGHTLY";

    @Autowired
    private RoomCacheService roomCacheService;

    @Autowired
    private ComponentDAO componentDao;

    @Autowired
    private ApplicationProperties applicationProperties;

    @Autowired
    private ReservationServiceHelper reservationServiceHelper;

    /*
     * (non-Javadoc)
     *
     * @see com.mgm.services.booking.room.service.ComponentService#
     * getAvailableRoomComponents(com.mgm.services.booking.room.model.request.
     * RoomComponentRequest)
     */
    @Override
    public List<RoomRequest> getAvailableRoomComponents(RoomComponentRequest componentRequest) {
    	 return componentDao.getRoomComponentAvailability(componentRequest);
    }

    private List<RoomRequest> filterOutPkgComponent(List<RoomRequest> componentList, String propertyId) {
        if (componentList == null) {
            return Collections.emptyList();
        }
        List<String> pkgComponentCodes = reservationServiceHelper.getPkgComponentCodeByPropertyId(propertyId);
        return componentList.stream()
                .filter(c -> ValidationUtil.isUuid(c.getId()) || !ReservationUtil.isPkgComponent(c.getId(), pkgComponentCodes))
                .collect(Collectors.toList());
    }

    /*
     * (non-Javadoc)
     *
     * @see
     * com.mgm.services.booking.room.service.ComponentService#getRoomComponent(
     * java.lang.String, java.lang.String)
     */
    @Override
    public RoomRequest getRoomComponent(String roomTypeId, String componentId) {

        List<RoomComponent> components = roomCacheService.getRoom(roomTypeId).getComponents();

        RoomRequest roomRequest = new RoomRequest();
        components.forEach(component -> {
            if (component.getId().equals(componentId)) {

                roomRequest.setId(component.getId());
                roomRequest.setPrice(component.getPrice());
                roomRequest.setNightlyCharge(component.getPricingApplied().equals(NIGHTLY));
                roomRequest.setSelected(true);
                roomRequest.setDescription(component.getShortDescription());
            }

        });

        return roomRequest;
    }

    @Override
    public List<com.mgm.services.booking.room.model.RoomComponent> getAvailableRoomComponents(
            RoomComponentV2Request componentV2Request) {
        try {
            RoomComponentRequest componentRequest = RoomComponentRequestTransformer
                    .getRoomComponentRequest(componentV2Request.getSource(), componentV2Request);
            List<RoomRequest> filteredComponentList = filterOutPkgComponent(getAvailableRoomComponents(componentRequest), componentRequest.getPropertyId());
            return com.mgm.services.booking.room.model.RoomComponent
                    .transform(filteredComponentList, applicationProperties);
        } catch (EAuroraException ex) {
            // This is a temporary approach. This will be migrated to DAO layer later.
            String errorType = AuroraError.getErrorType(ex.getErrorCode().name());
            if (AuroraError.FUNCTIONAL_ERROR.equals(errorType)) {
                throw new BusinessException(ErrorCode.AURORA_FUNCTIONAL_EXCEPTION, ex.getMessage());
            } else {
                throw new SystemException(ErrorCode.SYSTEM_ERROR, ex);
            }
        }
    }

    @Override
    public List<PackageComponentResponse> getAvailablePackageComponents(PackageComponentRequest packageComponentsRequest) {
        List<PackageComponentResponse> packageComponentResponseList = new ArrayList<>();
        PackageComponentsRequestDTO packageComponentsRequestDTO = packageComponentsRequest.getRequest();
        List<PkgRequestDTO> pkgDataDTOList = packageComponentsRequestDTO.getData();
        pkgDataDTOList.forEach(request ->{
            PackageComponentResponse response = new PackageComponentResponse();
            response.setPropertyId(request.getPropertyId());
            response.setProgramId(request.getProgramId());
            response.setRoomTypeId(request.getRoomTypeId());
            //handle shows
            if(CollectionUtils.isNotEmpty(request.getShowComponents())){
                response.setShowComponents(invokeGetPkgComponent(request.getShowComponents(),request.getPropertyId(),request.getRoomTypeId(),request.getProgramId(),packageComponentsRequest.getSource()));
            }
            //handle inclusions
            if(CollectionUtils.isNotEmpty(request.getInclusionComponents())){
                response.setInclusionComponents(invokeGetPkgComponent(request.getInclusionComponents(),request.getPropertyId(),request.getRoomTypeId(),request.getProgramId(),packageComponentsRequest.getSource()));
            }
            packageComponentResponseList.add(response);
        });
        return packageComponentResponseList;
    }


    private List<PkgComponent> invokeGetPkgComponent(List<NonRoomProducts> requestedComponents,String propertyId,String roomId,String programId,String source) {
        List<PkgComponent> componentResList = new ArrayList<>();
        requestedComponents.forEach(show -> {
            int qty = show.getQty();
            show.getDates().forEach(event -> {
                RoomComponentRequest pkgComponentRequest = new RoomComponentRequest();
                Date checkInDate = ReservationUtil.convertLocalDateToDate(event.getStart());
                pkgComponentRequest.setTravelStartDate(checkInDate);
                Date checkOutDate = ReservationUtil.convertLocalDateToDate(event.getEnd());
                if(event.getStart().equals(event.getEnd())){
                    pkgComponentRequest.setTravelEndDate(
                            ReservationUtil.convertLocalDateToDate(event.getEnd().plusDays(1))
                    );
                }else{
                    pkgComponentRequest.setTravelEndDate(checkOutDate);
                }
                pkgComponentRequest.setPropertyId(propertyId);
                pkgComponentRequest.setRoomTypeId(roomId);
                pkgComponentRequest.setProgramId(programId);
                pkgComponentRequest.setComponentIds(Collections.singletonList(show.getCode()));
                pkgComponentRequest.setSource(source);
                List<RoomRequest> componentListResponse = getAvailableRoomComponents(pkgComponentRequest);
                List<com.mgm.services.booking.room.model.RoomComponent> roomComponents = com.mgm.services.booking.room.model.RoomComponent.transformPckgComponents(componentListResponse, applicationProperties);
                componentResList.addAll(convertRoomComponentsToPkgComponents(roomComponents, qty, show.getCode(), event.getStart(), event.getEnd()));
            });
        });
        return componentResList;
    }

    private List<PkgComponent> convertRoomComponentsToPkgComponents(List<com.mgm.services.booking.room.model.RoomComponent> roomComponents, int qty, String code, LocalDate start, LocalDate end) {
        List<PkgComponent> pkgComponenResList = new ArrayList<>();
        List<PkgComponent> pkgComponentList = roomComponents.stream().filter(rc->{
            String nrCode = ACRSConversionUtil.getComponentCode(rc.getId());
            return nrCode.equalsIgnoreCase(code);
        }).map(roomComponent-> roomComponenttoPKGComponent(roomComponent,code,start,end)).collect(Collectors.toList());
        IntStream.rangeClosed(1,qty).forEach(
                x->{
                    pkgComponenResList.addAll(pkgComponentList);
                }
        );
        return pkgComponenResList;
    }
    private PkgComponent roomComponenttoPKGComponent(com.mgm.services.booking.room.model.RoomComponent roomComponent, String code, LocalDate start, LocalDate end) {
        return new PkgComponent( code, start, end,roomComponent.getId(), roomComponent.isNightlyCharge(), roomComponent.getPrice(), roomComponent.getDescription(), roomComponent.getPricingApplied(),
                roomComponent.getTaxRate(), roomComponent.getShortDescription(), roomComponent.getLongDescription(), roomComponent.getRatePlanName(), roomComponent.getRatePlanCode(), roomComponent.getAmtAftTax());
    }
   ///Start of v1 need to be deleted once V2 is integrated
    @Override
    public List<PackageComponentResponseV1> getAvailablePackageComponentsV1(PackageComponentRequestV1 packageComponentsRequest) {
        PackageComponentsRequestDTOV1 packageComponentsRequestDTO = packageComponentsRequest.getRequest();
        List<PkgDataDTO> pkgDataDTO = packageComponentsRequestDTO.getData();
        RoomComponentRequest pkgComponentRequest = new RoomComponentRequest();
        List<PackageComponentResponseV1> packageComponentResponseList = new ArrayList<>();
        if (null != packageComponentsRequestDTO.getDates()) {
            for (PkgDateDTOV1 pkgDateDTO : packageComponentsRequestDTO.getDates()) {
                PackageComponentResponseV1 packageComponentResponse = new PackageComponentResponseV1();
                List<PropertyPackageComponentResponse> propertyComponentList = new ArrayList<>();
                packageComponentResponse.setDate(pkgDateDTO);
                if (null != pkgDateDTO.getCheckIn() && null != pkgDateDTO.getCheckOut()) {
                    Date checkInDate = ReservationUtil.convertLocalDateToDate(pkgDateDTO.getCheckIn());
                    Date checkOutDate = ReservationUtil.convertLocalDateToDate(pkgDateDTO.getCheckOut());
                    pkgComponentRequest.setTravelStartDate(checkInDate);
                    pkgComponentRequest.setTravelEndDate(checkOutDate);
                    for (PkgDataDTO packageData : pkgDataDTO) {
                        getPackagePropertyComponentListV1(packageData,propertyComponentList,pkgComponentRequest,packageComponentsRequest,packageComponentResponse);
                    }
                }
                packageComponentResponseList.add(packageComponentResponse);
            }
        }
        return packageComponentResponseList;
    }
    private void getPackagePropertyComponentListV1(PkgDataDTO packageData, List<PropertyPackageComponentResponse> propertyComponentList, RoomComponentRequest pkgComponentRequest, PackageComponentRequestV1 packageComponentsRequest, PackageComponentResponseV1 packageComponentResponse) {
        Double totalTax = 0.0;
        Map<String, Integer> componentQuantityMap = new HashMap<>();
        List<com.mgm.services.booking.room.model.RoomComponent> pkgComponentResponsesList = new ArrayList<>();
        PropertyPackageComponentResponse propertyPackageComponentResponse = new PropertyPackageComponentResponse();
        propertyComponentList.add(propertyPackageComponentResponse);
        propertyPackageComponentResponse.setPropertyId(packageData.getPropertyId());
        pkgComponentRequest.setPropertyId(packageData.getPropertyId());
        pkgComponentRequest.setRoomTypeId(packageData.getRoomTypeId());
        pkgComponentRequest.setProgramId(packageData.getProgramId());
        List<String> reqListOfComponents = packageData.getNonRoomProducts().stream().map(i->i.getCode()).collect(Collectors.toList());
        if (null != reqListOfComponents) {
            pkgComponentRequest.setComponentIds(new ArrayList<>(reqListOfComponents));
        }
        pkgComponentRequest.setSource(packageComponentsRequest.getSource());
        List<RoomRequest> componentListResponse = getAvailableRoomComponents(pkgComponentRequest);
        List<com.mgm.services.booking.room.model.RoomComponent> roomComponents = com.mgm.services.booking.room.model.RoomComponent.transformPckgComponents(componentListResponse, applicationProperties);
        if (CollectionUtils.isNotEmpty(roomComponents)) {
            getComponentQtyV1(packageData, componentQuantityMap);
            propertyPackageComponentResponse.setPkgComponents(getPkgComponentOnQtyBasedV1(roomComponents, reqListOfComponents, componentQuantityMap, pkgComponentResponsesList));
            packageComponentResponse.setPkgComponent(propertyComponentList);
        }
    }

    private void getComponentQtyV1(PkgDataDTO packageData, Map<String, Integer> componentQuantityMap) {
        for (NonRoomProducts nonRoomProducts : packageData.getNonRoomProducts()) {
            Integer quantity = nonRoomProducts.getQty();
            String code = nonRoomProducts.getCode();
            componentQuantityMap.put(code, quantity);
        }
    }

    private List<com.mgm.services.booking.room.model.RoomComponent> getPkgComponentOnQtyBasedV1(List<com.mgm.services.booking.room.model.RoomComponent> roomComponents, List<String> reqListOfComponents, Map<String, Integer> quantity, List<com.mgm.services.booking.room.model.RoomComponent> pkgComponentResponsesList) {
        for (com.mgm.services.booking.room.model.RoomComponent pkgComponentResponseLists : roomComponents) {
            reqListOfComponents.stream()
                    .filter(filteredComponents -> ACRSConversionUtil.getComponentCode(pkgComponentResponseLists.getId()).equals(filteredComponents))
                    .flatMapToInt(filteredComponents -> IntStream.rangeClosed(1, quantity.get(filteredComponents)))
                    .mapToObj(i -> pkgComponentResponseLists).forEach(pkgComponentResponsesList::add);
        }
        return pkgComponentResponsesList;
    }




}
