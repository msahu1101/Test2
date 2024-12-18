package com.mgm.services.booking.room.model;

import javax.annotation.Nonnull;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

/**
 * The Pojo class to keep the name and value of the user attribute.
 */
@Getter
@Setter
public @ToString class UserAttribute {

    private String name;
    private String value;

    /**
     * Default Constructor.
     */
    public UserAttribute() {
        super();
    }

    /**
     * The constructor class.
     *
     * @param nameParam
     *              The name of attribute.
     * @param valueParam
     *              The value of attribute.
     */
    public UserAttribute(@Nonnull String nameParam, @Nonnull String valueParam) {
        name = nameParam;
        value = valueParam;
    }
}
