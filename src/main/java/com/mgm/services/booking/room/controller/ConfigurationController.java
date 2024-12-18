package com.mgm.services.booking.room.controller;
import com.mgm.services.booking.room.annotations.V2Controller;
import com.mgm.services.booking.room.model.crs.searchoffers.TaxDefinition;
import com.mgm.services.booking.room.service.cache.rediscache.service.impl.RedisCacheReadServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import java.util.List;

/**
 * This class contains the services responsible for returning the room related
 * details like available component etc.
 * 
 * @author laknaray
 *
 */
@RestController
@RequestMapping("/v2")
@V2Controller
public class ConfigurationController extends ExtendedBaseV2Controller {
    @Autowired
    private RedisCacheReadServiceImpl redisReadService;
     @GetMapping("/configuration/taxAndFees/{propertyCode}")
    public List<TaxDefinition> getPropertyTaxAndFeesConfiguration(@RequestHeader String source,
                                                                  @PathVariable String propertyCode){
        return redisReadService.getPropertyTaxAndFees(propertyCode);
    }
}
