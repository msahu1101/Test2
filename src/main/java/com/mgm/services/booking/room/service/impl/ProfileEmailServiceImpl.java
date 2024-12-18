package com.mgm.services.booking.room.service.impl;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.mgm.services.booking.room.constant.ServiceConstant;
import com.mgm.services.booking.room.dao.EmailDAO;
import com.mgm.services.booking.room.model.Email;
import com.mgm.services.booking.room.model.request.CreateCustomerRequest;
import com.mgm.services.booking.room.properties.DomainProperties;
import com.mgm.services.booking.room.service.ProfileEmailService;
import com.mgm.services.booking.room.service.cache.SignupEmailCacheService;
import com.mgm.services.common.model.Customer;
import com.mgm.services.common.util.BaseCommonUtil;

import freemarker.template.TemplateException;
import lombok.extern.log4j.Log4j2;

/**
 * Implementation class for ProfileEmailService
 */
@Component
@Log4j2
public class ProfileEmailServiceImpl implements ProfileEmailService{

    @Autowired
    private SignupEmailCacheService signupEmailCacheService;

    @Autowired
    private DomainProperties domainProperties;

    @Autowired
    private EmailDAO emailDao;

    @Override
    public void sendAccountCreationMail(CreateCustomerRequest input, Customer customer) {

        Map<String, Object> actualContent = new HashMap<>();
        actualContent.put(ServiceConstant.EMAIL_HOSTURL, domainProperties.getAem());
        actualContent.put(ServiceConstant.EMAIL_URI_SCHEME, "https");
        if (null != customer) {
            actualContent.put(ServiceConstant.EMAIL_CUSTOMERFNAME, customer.getFirstName());
        }

        // Sending the signup confirmation template available for mgmresorts every time
        Email emailTemplate = signupEmailCacheService.getSignupEmailTemplate("mgmresorts");
        emailTemplate.setTo(input.getCustomerEmail());

        sendEmail(emailTemplate, actualContent);
    }
        
    private void sendEmail(Email emailTemplate, Map<String, Object> actualContent) {
        try {
            log.info(actualContent);
            emailTemplate.setBody(BaseCommonUtil.getFTLTransformedContent(emailTemplate.getBody(), actualContent));

        } catch (IOException | TemplateException e) {
            log.error("Error populating confirmation email: {}", e);
        }

        emailDao.sendEmail(emailTemplate);
    }
    
}
