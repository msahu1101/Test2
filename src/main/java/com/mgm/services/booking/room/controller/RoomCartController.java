package com.mgm.services.booking.room.controller;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.Errors;
import org.springframework.validation.Validator;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.util.WebUtils;

import com.mgm.services.booking.room.constant.ServiceConstant;
import com.mgm.services.booking.room.model.CartSummary;
import com.mgm.services.booking.room.model.RatesSummary;
import com.mgm.services.booking.room.model.RoomCartItem;
import com.mgm.services.booking.room.model.phoenix.RoomProgram;
import com.mgm.services.booking.room.model.request.RoomCartRequest;
import com.mgm.services.booking.room.model.request.RoomCartUpdateRequest;
import com.mgm.services.booking.room.model.request.RoomComponentRequest;
import com.mgm.services.booking.room.model.request.RoomProgramValidateRequest;
import com.mgm.services.booking.room.model.reservation.ReservationProfile;
import com.mgm.services.booking.room.model.reservation.RoomRequest;
import com.mgm.services.booking.room.model.reservation.RoomReservation;
import com.mgm.services.booking.room.model.response.CartResponse;
import com.mgm.services.booking.room.model.response.RoomReservationResponse;
import com.mgm.services.booking.room.properties.ApplicationProperties;
import com.mgm.services.booking.room.service.ComponentService;
import com.mgm.services.booking.room.service.RoomCartService;
import com.mgm.services.booking.room.service.RoomProgramService;
import com.mgm.services.booking.room.service.cache.RoomProgramCacheService;
import com.mgm.services.booking.room.transformer.RoomComponentRequestTransformer;
import com.mgm.services.booking.room.transformer.RoomReservationTransformer;
import com.mgm.services.booking.room.util.CommonUtil;
import com.mgm.services.booking.room.validator.RoomCartRequestValidator;
import com.mgm.services.common.exception.BusinessException;
import com.mgm.services.common.exception.ErrorCode;
import com.mgm.services.common.exception.ValidationException;
import com.mgm.services.common.model.CartItem;
import com.mgm.services.common.model.ServicesSession;
import com.vdurmont.emoji.EmojiParser;

import lombok.extern.log4j.Log4j2;
import reactor.core.publisher.Mono;

/**
 * Controller to handle pre-reserve functions like add room, room requests etc.
 *
 */
@RestController
@RequestMapping("/v1")
@Log4j2
public class RoomCartController extends ExtendedBaseController {

    private final Validator validator = new RoomCartRequestValidator();

    @Autowired
    private RoomCartService prereserveService;

    @Autowired
    private ComponentService componentService;

    @Autowired
    private RoomProgramCacheService roomProgramCacheService;

    @Autowired
    private RoomProgramService roomProgramService;
    
    @Autowired
    private ApplicationProperties appProperties;

    /**
     * Service to handle POST request of adding room to cart. Invokes service to
     * update room reservation charges/taxes, by invoking aurora service.
     * 
     * @param source
     *            Source or channel
     * @param prereserveRequest
     *            Pre-reserve request
     * @param result
     *            Binding result
     * @param servletRequest
     *            HttpServlet request object
     * @param enableJwb
     *            enableJwb header
     * @return Returns created Room reservation response
     */
    @PostMapping("/cart/room")
    public RoomReservationResponse addRoomToCart(@RequestHeader String source,
            @RequestBody RoomCartRequest prereserveRequest, HttpServletRequest servletRequest, @RequestHeader(
                    defaultValue = "false") String enableJwb) {

        preprocess(source, prereserveRequest, null, servletRequest, enableJwb);

        Errors errors = new BeanPropertyBindingResult(prereserveRequest, "prereserveRequest");
        validator.validate(prereserveRequest, errors);
        handleValidationErrors(errors);

        prereserveRequest.setMyVegasRedemptionItems(sSession.getMyVegasRedemptionItems());
        prereserveRequest.setCustomer(sSession.getCustomer());
        prereserveRequest.setAuroraItineraryIds(CommonUtil.getAuroraItinerariesInCart(sSession.getCartItems()));
        validateProgramViolations(prereserveRequest, sSession);

        RoomReservation reservation = prereserveService.prepareRoomCartItem(prereserveRequest);

        setProfile(prereserveRequest, reservation);

        // Find all available components for the selected room and trip
        // dates
        RoomComponentRequest componentRequest = RoomComponentRequestTransformer.getRoomComponentRequest(source,
                prereserveRequest);

        List<RoomRequest> roomRequests = new ArrayList<>(componentService.getAvailableRoomComponents(componentRequest));

        // Transform the reservation object into application friendly
        // response structure
        RoomReservationResponse response = RoomReservationTransformer.transform(reservation, appProperties);
        response.setRoomRequests(roomRequests);

        // Save the reservation object as cart item into session
        RoomCartItem cartItem = new RoomCartItem();
        cartItem.setAvailableComponents(roomRequests);
        cartItem.setReservation(reservation);
        cartItem.setReservationId(response.getItemId());
        // Saving the request along with cart item, so it's easier during
        // re-price
        cartItem.setCartItemRequest(prereserveRequest);
        final Cookie cookie = WebUtils.getCookie(servletRequest, "enableJwb");
        final String enableJwbCookie = (cookie == null ? "" : cookie.getValue());
        cartItem.setPromotedMlifePrice(StringUtils.equalsIgnoreCase(enableJwbCookie, ServiceConstant.TRUE)
                || StringUtils.equalsIgnoreCase(enableJwb, ServiceConstant.TRUE));

        if (prereserveRequest.getCustomerId() > 0 && !cartItem.isPromotedMlifePrice()) {
            // Call the Aurora service to save the cart item on Aurora side
            // and save the itinerary id to the session for logged-in users
            RoomReservation saveResponse = prereserveService.saveRoomCartItemInAurora(reservation);
            reservation.setItineraryId(saveResponse.getItineraryId());
            reservation.setId(saveResponse.getId());
            cartItem.setAuroraItineraryId(saveResponse.getItineraryId());

        }
        sSession.getCartItems().add(cartItem);

        log.info("Session cart updated. Items {}", sSession.getCartItems().size());

        if (cartItem.isPromotedMlifePrice()) {
            log.info("Cart item added with JWB enabled");
        }

        return response;
    }

    private void setProfile(RoomCartRequest prereserveRequest, RoomReservation reservation) {
        if (reservation.getProfile() == null) {
            ReservationProfile profile = new ReservationProfile();
            profile.setId(prereserveRequest.getCustomerId());
            profile.setMlifeNo(NumberUtils.toInt(prereserveRequest.getMlifeNumber()));
            reservation.setProfile(profile);
        }
        if (reservation.getSource() == null) {
            reservation.setSource(prereserveRequest.getSource());
        }
    }

    /**
     * Check if an item with a patron program id is already present in the cart.
     * 
     * @param prereserveRequest
     *            the request object
     * @param session
     *            the session object
     * @return flag if there are any program violations
     */
    private void validateProgramViolations(RoomCartRequest prereserveRequest, ServicesSession session) {
        if (session.getCartItems().isEmpty() || StringUtils.isEmpty(prereserveRequest.getProgramId())) {
            return;
        }

        // If a patron program id is passed, stop adding further items into cart
        // for same patron promotion
        RoomProgram roomProgram = roomProgramCacheService.getRoomProgram(prereserveRequest.getProgramId());
        if (roomProgram != null && StringUtils.isNotEmpty(roomProgram.getPatronPromoId())) {
            List<RoomCartItem> cartItems = CommonUtil.getRoomCartItems(session.getCartItems(), null);
            for (RoomCartItem item : cartItems) {
                String itemProgramId = item.getReservation().getProgramId();
                if (StringUtils.isNotEmpty(itemProgramId) && itemProgramId.equals(prereserveRequest.getProgramId())) {
                    throw new BusinessException(ErrorCode.PROGRAM_ALREADY_IN_CART);
                }
            }
        }

        CommonUtil.isEligibleForMyVegasRedemption(prereserveRequest.getMyVegasRedemptionItems(), roomProgram,
                session.getCustomer());

    }

    /**
     * Service to delete room in the cart based on selected room id.
     * 
     * @param itemId
     *            room id to remove
     * @return Returns updated room reservation response
     */
    @DeleteMapping("/cart/room/{itemId}")
    @ResponseStatus(
            value = HttpStatus.NO_CONTENT)
    public void deleteRoomInCart(@PathVariable String itemId) {

        // Remove the booked item from the cart items
        List<CartItem> cartItems = new ArrayList<>();
        for (CartItem cartItem : sSession.getCartItems()) {
            if (!cartItem.getReservationId().equals(itemId)) {
                cartItems.add(cartItem);
            }
        }

        sSession.setCartItems(cartItems);

    }

    /**
     * Service to get all the rooms in the cart along with summary
     * 
     * @param source
     *            Source or channel
     * @return Returns updated room reservation response
     */
    @GetMapping("/cart/room")
    public Mono<CartResponse> getRoomsInCart(@RequestHeader String source) {

        List<RoomCartItem> allRoomCartItems = CommonUtil.getRoomCartItems(sSession.getCartItems(), source);

        if (allRoomCartItems.isEmpty()) {
            throw new BusinessException(ErrorCode.NO_CART_ITEMS);
        }
        // Iterate through the cart items in session and create and return a
        // cart summary
        List<RoomReservationResponse> cartItems = new ArrayList<>();
        double reservationTotal = ServiceConstant.ZERO_DOUBLE_VALUE;
        double depositDue = ServiceConstant.ZERO_DOUBLE_VALUE;
        double balanceUponCheckIn = ServiceConstant.ZERO_DOUBLE_VALUE;
        log.debug("Items in cart: {}", allRoomCartItems);
        for (RoomCartItem cartItem : allRoomCartItems) {
            RoomReservationResponse responseItem = RoomReservationTransformer.transform(cartItem.getReservation(), appProperties);

            responseItem.setRoomRequests(cartItem.getAvailableComponents());

            RatesSummary rates = responseItem.getRates();
            reservationTotal += rates.getReservationTotal();
            depositDue += rates.getDepositDue();
            balanceUponCheckIn += rates.getBalanceUponCheckIn();
            log.debug("Add transformed cart item: {}", responseItem);
            cartItems.add(responseItem);
        }

        CartSummary summary = new CartSummary();
        summary.setReservationTotal(reservationTotal);
        summary.setDepositDue(depositDue);
        summary.setBalanceUponCheckIn(balanceUponCheckIn);
        CartResponse response = new CartResponse();
        response.setItems(cartItems);
        response.setSummary(summary);

        log.debug("End getRoomsInCart method with response: {}", response);

        return Mono.just(response);
    }

    /**
     * Service to handle PUT request to update room reservation based on
     * selected room requests.
     * 
     * @param source
     *            Source/channel
     * @param putRequest
     *            Pre-reserve put request
     * @param itemId
     *            Reservation item identifier
     * @return Returns updated room reservation response
     */
    @PutMapping("/cart/room/{itemId}")
    public RoomReservationResponse updateRoomInCart(@RequestHeader String source,
            @RequestBody RoomCartUpdateRequest putRequest, @PathVariable String itemId) {

        preprocess(source, putRequest, null);

        RoomReservation roomReservation = null;
        List<RoomRequest> roomRequests = new ArrayList<>();
        List<RoomCartItem> allRoomCartItems = CommonUtil.getRoomCartItems(sSession.getCartItems(), null);

        log.info("Item Id {}", itemId);
        log.info("Session Items {}", allRoomCartItems.size());
        // Get available room components from the cart item saved in the session
        for (RoomCartItem cartItem : allRoomCartItems) {

            log.info("Cart Id {}", cartItem.getReservationId());
            if (cartItem.getReservationId().equals(itemId)) {
                roomReservation = cartItem.getReservation();
                roomRequests = cartItem.getAvailableComponents();
                break;
            }
        }

        // If there's no reservation object in session, consider the token as
        // expired
        if (null == roomReservation) {
            throw new ValidationException(Collections.singletonList(ErrorCode.ITEM_NOT_FOUND.getErrorCode()));
        }

        // Mark the selected room requests as true based on requests passed in
        // request
        List<String> roomRequestIds = new ArrayList<>();
        roomRequests.forEach(request -> putRequest.getRoomRequests().forEach(selectedRequest -> {
            if (request.getId().equals(selectedRequest.getId())) {

                if (selectedRequest.isSelected()) {
                    request.setSelected(true);
                    roomRequestIds.add(request.getId());
                } else {
                    request.setSelected(false);
                }

            }
        }));
        roomReservation.setSpecialRequests(roomRequestIds);
        roomReservation.setSource(source);
        if (StringUtils.isNotEmpty(putRequest.getSpecialRequests())) {
            // removing special chars & emojis since OXI doesn't support it
            String comment = 
                    putRequest.getSpecialRequests().replaceAll("[!@#$%^&*()=+\\[\\]{}\\|?><\",.?`~*\\+]", ServiceConstant.WHITESPACE_STRING);
            comment = EmojiParser.removeAllEmojis(comment);
            roomReservation.setComments(comment);
        }

        // Update room reservation object with latest prices/taxes by calling BE
        // service again
        RoomReservation reservation = prereserveService.addRoomRequests(roomReservation);

        reservation.setInSessionReservationId(itemId);
        RoomReservationResponse response = RoomReservationTransformer.transform(reservation, appProperties);
        // Update the cart item with latest reservation object
        for (RoomCartItem cartItem : allRoomCartItems) {
            if (cartItem.getReservationId().equals(itemId)) {
                cartItem.setReservation(reservation);
                break;
            }
        }

        response.setRoomRequests(roomRequests);

        return response;
    }

    /**
     * Checks the eligibility and re-prices the room item in cart if required
     * and not added when Jwb is enabled.
     */
    public void repriceRoomsOnLogin() {

        if (null == sSession || sSession.getCartItems().isEmpty()) {
            log.info("No cart items, nothing to reprice");
            return;
        }

        CommonUtil.getRoomCartItems(sSession.getCartItems(), null).forEach(item -> {
            if (item.isPromotedMlifePrice()) {
                log.info("Cart item is added via JWB, hence removing the cart item from session");
                sSession.getCartItems().remove(item);

            } else if (checkCartItemEligibility(sSession, item)) {
                log.info("Cart item is eligible, so no need to reprice");

            } else {

                // If there's any valid issue during repricing, cart item will
                // be removed
                try {
                    repriceReservation(sSession, item);
                } catch (BusinessException e) {
                    log.error("Exception occurred during repricing {}", e);
                    sSession.getCartItems().remove(item);
                }
            }
        });

    }

    private void repriceReservation(ServicesSession servicesSession, RoomCartItem item) {

        // re-price
        log.info("Cart item is not eligible for current user state, repricing the cart item");
        RoomCartRequest cartRequest = item.getCartItemRequest();
        cartRequest.setProgramId(null);

        // Subscribe to room reservation object from prereserveRoom
        // service
        RoomReservation reservation = prereserveService.prepareRoomCartItem(cartRequest);
        setProfile(cartRequest, reservation);
        log.info("Cart item replaced with re-priced reservation");

        // Reusing previous cart item ID
        reservation.setInSessionReservationId(item.getReservation().getInSessionReservationId());
        // Updating reservation in cart item
        item.setReservation(reservation);

        if (servicesSession.getCustomer().getCustomerId() > 0) {
            // Call the Aurora service to save the cart item on Aurora side
            // and save the itinerary id to the session for logged-in users
            RoomReservation saveResponse = prereserveService.saveRoomCartItemInAurora(reservation);
            item.setAuroraItineraryId(saveResponse.getItineraryId());

        }

    }

    /**
     * Checks if the item in the cart is eligible for the logged-in user.
     * User should be eligible for program at the reservation level as well as
     * other programs that may be used partially.
     * 
     * @param servicesSession
     *            Services session
     * @param cartItem
     *            item in cart
     * @return Returns false if the user is not eligible for any of the program
     */
    private boolean checkCartItemEligibility(ServicesSession servicesSession, RoomCartItem cartItem) {

        Set<String> programIds = new HashSet<>();
        if (StringUtils.isNotEmpty(cartItem.getReservation().getProgramId())) {
            programIds.add(cartItem.getReservation().getProgramId());
        }
        cartItem.getReservation().getBookings().forEach(booking -> programIds.add(booking.getProgramId()));

        log.info(cartItem.getCartItemRequest());

        Map<String, Boolean> outputMap = new HashMap<>();

        programIds.forEach(program -> {
            RoomProgramValidateRequest request = new RoomProgramValidateRequest();
            request.setProgramId(program);
            request.setPropertyId(cartItem.getReservation().getPropertyId());
            request.setCustomerId(servicesSession.getCustomer().getCustomerId());
            request.setSource(cartItem.getCartItemRequest().getSource());
            outputMap.put(program, roomProgramService.isProgramApplicable(request));
        });

        // return false if at least one program is ineligible
        return !outputMap.containsValue(Boolean.FALSE);
    }
}
