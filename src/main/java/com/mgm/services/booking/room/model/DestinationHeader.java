package com.mgm.services.booking.room.model;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Data;

public @Data class DestinationHeader {
	@JsonProperty("x-authorization")
	private String xAuthorization;
	@JsonProperty("Accept")
	private String accept;
	@JsonProperty("Content-Type")
	private String contentType;
	@JsonProperty("Ama-Reservation-Owner")
	private String amaReservationOwner;
	@JsonProperty("Ama-Pos")
	private String amaPos;
	@JsonProperty("Ama-Channel-Identifiers")
	private String amaChannelIdentifiers;
	@JsonProperty("HttpMethod")
	private String httpMethod;
	@JsonProperty("Ama-Client-Ref")
	private String amaClientRef;
	
}
