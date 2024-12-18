package com.mgm.services.booking.room.model.loyalty;

import java.util.ArrayList;
import java.util.List;

import lombok.Data;

@Data
public class PlayerPromotionResponse {

    private List<CustomerPromotion> customerPromotions = new ArrayList<>();
}
