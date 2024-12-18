package com.mgm.services.booking.room.model;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.Nonnull;

import com.fasterxml.jackson.annotation.JsonInclude;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

/**
 * The container POJO class for multiple user attributes.
 */
@Getter
@Setter
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public @ToString class UserAttributeList {

    private List<UserAttribute> userAttributes;
    private String userIdentifier;

    /**
     * The Default constructor.
     */
    public UserAttributeList() {
        userAttributes = new ArrayList<>();
    }

    /**
     * The constructer for user attributes.
     *
     * @param count
     *              The initial capacity of user attributes.
     */
    public UserAttributeList(int count) {
        userAttributes = new ArrayList<>(count);
    }

    /**
     * The method to add a user attribute
     *
     * @param name
     *              The attribute name.
     * @param value
     *              The attribute value
     * @return The reference to current user attribute.
     */
    public UserAttributeList add(@Nonnull String name, @Nonnull String value) {
        userAttributes.add(new UserAttribute(name, value));
        return this;
    }
}
