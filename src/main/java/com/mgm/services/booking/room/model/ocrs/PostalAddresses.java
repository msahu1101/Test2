package com.mgm.services.booking.room.model.ocrs;

import java.util.ArrayList;
import java.util.List;

import lombok.Data;

@Data
public class PostalAddresses {
    private List<PostalAddress> postalAddress = new ArrayList<>();

}
