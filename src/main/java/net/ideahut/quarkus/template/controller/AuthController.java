package net.ideahut.quarkus.template.controller;


import jakarta.inject.Inject;
import jakarta.validation.constraints.NotBlank;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.core.Context;
import net.ideahut.quarkus.annotation.Public;
import net.ideahut.quarkus.api.ApiAccess;
import net.ideahut.quarkus.api.ApiAuth;
import net.ideahut.quarkus.api.ApiRequest;
import net.ideahut.quarkus.api.ApiService;
import net.ideahut.quarkus.template.service.AuthService;

/*
 * API untuk login & logout
 */
@Public
@Path("/auth")
class AuthController {
	
	private final AuthService authService;
	private final ApiService apiService;
	
	@Inject
	AuthController(
		AuthService authService,
		ApiService apiService
	) {
		this.authService = authService;
		this.apiService = apiService;
	}
	
	@POST
	@Path("/login")
	public ApiAuth login(
		@Context ContainerRequestContext httpRequest,
		@NotBlank @QueryParam("username") String username,
		@NotBlank @QueryParam("password") String password
	) throws Exception {
		ApiRequest apiRequest = apiService.getApiRequest(httpRequest, true);
		ApiAuth apiAuth = authService.login(apiRequest, username, password);
		return apiAuth != null ? apiAuth.setApiAccess(null).setApiKey(null) : null;
	}
	
	private ApiAccess logout(
		ContainerRequestContext httpRequest
	) {
		ApiRequest apiRequest = apiService.getApiRequest(httpRequest, true);
		return authService.logout(apiRequest);
	}
	
	@GET
	@Path("/logout")
	public ApiAccess logoutGET(
		@Context ContainerRequestContext httpRequest	
	) {
		return logout(httpRequest);
	}
	
	@POST
	@Path("/logout")
	public ApiAccess logoutPOST(
		@Context ContainerRequestContext httpRequest	
	) {
		return logout(httpRequest);
	}
	
	private ApiAccess info(
		ContainerRequestContext httpRequest	
	) {
		ApiRequest apiRequest = apiService.getApiRequest(httpRequest, true);
		return authService.info(apiRequest);
	}
	
	@GET
	@Path("/info")
	public ApiAccess infoGET(
		@Context ContainerRequestContext httpRequest	
	) {
		return info(httpRequest);
	}
	
	@POST
	@Path("/info")
	public ApiAccess infoPOST(
		@Context ContainerRequestContext httpRequest	
	) {
		return info(httpRequest);
	}
	
}
