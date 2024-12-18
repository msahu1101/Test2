/**
 * 
 */
package com.mgm.services.booking.room.model.request;

import java.io.Serializable;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @author laknaray
 *
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class MyVegasUnavailableDate implements Serializable {
    
    private static final long serialVersionUID = 8526786482619068710L;
    
    private String beginDate;
    private String endDate;

}
