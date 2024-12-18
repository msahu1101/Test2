package com.mgm.services.booking.room.model.ocrs;

import lombok.Data;

@Data
public class SelectedMembership {
    private String accountID;
    private String programCode;
    private String levelCode;
    private Long expireDate;
    private Long startDate;
    private String nameOnCard;
    private String displaySequence;
    private boolean pointIndicator;
    private String enrollmentSource;
    private String enrolledAt;
}
