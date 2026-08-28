package net.ideahut.quarkus.template.controller;


import com.fasterxml.jackson.databind.JsonNode;

import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.core.Context;
import net.ideahut.quarkus.annotation.Public;
import net.ideahut.quarkus.helper.FrameworkHelper;
import net.ideahut.quarkus.helper.WebHelper;
import net.ideahut.quarkus.object.StringList;
import net.ideahut.quarkus.object.StringMap;
import net.ideahut.quarkus.object.StringSet;
import net.ideahut.quarkus.template.service.MessageService;

/*
 * API untuk mendapatkan message yang tersimpan di resource & database
 */
@Public
@Path("/message")
class MessageController {

	private final MessageService messageService;
	
	@Inject
	MessageController(
		MessageService messageService		
	) {
		this.messageService = messageService;
	}

	@GET
	@Path("/mobile")
	public JsonNode mobile() {
		return messageService.getResource("mobile");
	}
	
	@GET
	@Path("/portal")
	public JsonNode portal() {
		return messageService.getResource("portal");
	}
	
	@POST
	@Path("/translate/map")
	public StringMap translateMap(
		@Context ContainerRequestContext httpRequest
	) {
		byte[] bytes = WebHelper.getBodyAsBytes(httpRequest);
		StringSet codes = FrameworkHelper.defaultDataMapper().read(bytes, StringSet.class);
		return messageService.getMap(codes.toArray(new String[0]));
	}
	
	@POST
	@Path("/translate/list")
	public StringList translateList(
		@Context ContainerRequestContext httpRequest
	) {
		byte[] bytes = WebHelper.getBodyAsBytes(httpRequest);
		StringList codes = FrameworkHelper.defaultDataMapper().read(bytes, StringList.class);
		return messageService.getList(codes.toArray(new String[0]));
	}
	
}
