package net.ideahut.quarkus.template.controller;

import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.core.Context;
import net.ideahut.quarkus.annotation.Public;
import net.ideahut.quarkus.api.ApiAccess;
import net.ideahut.quarkus.api.ApiRequest;
import net.ideahut.quarkus.api.ApiService;
import net.ideahut.quarkus.template.service.AuthService;

/*
 * Komunikasi antar ApiService
 * - Membuat token access untuk consumer (Api Provider)
 * - Mendapatkan object Api Access (jika service ini yang menerbitkan token user)
 */
@Public
@Path("/api")
class ApiController {
	
	private final ApiService apiService;
	private final AuthService authService;
	
	@Inject
	ApiController(
		ApiService apiService,
		AuthService authService
	) {
		this.apiService = apiService;
		this.authService = authService;
	}
	
	
	/*
	 * Membuat token untuk API Consumer
	 */
	private String token(
		ContainerRequestContext httpRequest
	) {
		ApiRequest apiRequest = apiService.getApiRequest(httpRequest, true);
		return authService.createConsumerToken(apiRequest);
	}
	
	@GET
	@Path("/token")
	public String tokenGET(
		@Context ContainerRequestContext httpRequest	
	) {
		return token(httpRequest);
	}
	
	@POST
	@Path("/token")
	public String tokenPOST(
		@Context ContainerRequestContext httpRequest	
	) {
		return token(httpRequest);
	}
	
	
	/*
	 * Mendapatkan ApiAccess Object berdasarkan token API user
	 */
	private ApiAccess access(
		ContainerRequestContext httpRequest
	) {
		ApiRequest apiRequest = apiService.getApiRequest(httpRequest, true);
		return authService.getApiAccessForExternal(apiRequest);
	}
	
	@GET
	@Path("/access")
	public ApiAccess accessGET(
		@Context ContainerRequestContext httpRequest	
	) {
		return access(httpRequest);
	}
	
	@POST
	@Path("/access")
	public ApiAccess accessPOST(
		@Context ContainerRequestContext httpRequest	
	) {
		return access(httpRequest);
	}
	
}
