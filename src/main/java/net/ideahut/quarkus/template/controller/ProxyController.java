package net.ideahut.quarkus.template.controller;

import jakarta.inject.Inject;
import jakarta.validation.constraints.NotBlank;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.HEAD;
import jakarta.ws.rs.PATCH;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.ResponseBuilder;
import net.ideahut.quarkus.annotation.Body;
import net.ideahut.quarkus.annotation.Public;
import net.ideahut.quarkus.api.ApiService;
import net.ideahut.quarkus.api.ApiSource;
import net.ideahut.quarkus.helper.WebHelper;
import net.ideahut.quarkus.rest.RestMethod;
import net.ideahut.quarkus.rest.RestRequest;
import net.ideahut.quarkus.rest.RestResponse;

/*
 * API untuk request ke ApiService yang lain
 */
@Public
@Path("/proxy")
class ProxyController {
	
	private final ApiService apiService;
	
	@Inject
	ProxyController(
		ApiService apiService
	) {
		this.apiService = apiService;
	}
	
	/*
	 * Meminta token API consumer untuk melakukan request
	 * Token akan disimpan di SysParams, sysCode = "API_TOKEN", paramCode = <API_NAME>
	 */
	@POST
	@Path("/token")
	public String token(
		@NotBlank @QueryParam("apiName") String apiName
	) {
		ApiSource apiSource = apiService.getApiSource(apiName);
		return apiService.getApiSourceGate(apiSource).createApiToken(apiSource);
	}

	/*
	 * Proxy request ke ApiService lain
	 * - menggunakan ApiToken yang tersimpan di SysParam
	 * - semua http method diijinkan
	 * 
	 */
	private Response request(
		String apiName,
		String apiPath,
		ContainerRequestContext httpRequest
	) {
		RestRequest restRequest = new RestRequest()
		.setPath("/" + apiPath)
		.setMethod(RestMethod.valueOf(httpRequest.getMethod().toUpperCase()))
		.setQueryString(WebHelper.getQueryString(httpRequest))
		.setHeaders(WebHelper.getHeaders(httpRequest));
		if (!RestMethod.GET.equals(restRequest.getMethod())) {
			byte[] requestBody = WebHelper.getBodyAsBytes(httpRequest);
			restRequest.setBody(requestBody);
		} else {
			restRequest.getHeaders().remove(HttpHeaders.CONTENT_LENGTH);
		}
		RestResponse restResponse = apiService.callEndpoint(apiName, restRequest).throwError();
		ResponseBuilder responseBuilder = Response
		.status(restResponse.getStatus())
		.entity(restResponse.getBodyAsByteArray());
		for (String restHeaderName : restResponse.getHeaderNames()) {
			String restHeaderValue = restResponse.getHeaderValue(restHeaderName);
			if (restHeaderValue != null) {
				responseBuilder.header(restHeaderName, restHeaderValue);
			}
		}
		return responseBuilder.build();
	}
	
	@GET
	@Body
	@Path("/request/{apiName}/{apiPath:.*}")
	public Response requestGET(
		@NotBlank @PathParam("apiName") String apiName,
		@PathParam("apiPath") String apiPath,
		@Context ContainerRequestContext httpRequest
	) {
		return request(apiName, apiPath, httpRequest);
	}
	
	@POST
	@Body
	@Path("/request/{apiName}/{apiPath:.*}")
	public Response requestPOST(
		@NotBlank @PathParam("apiName") String apiName,
		@PathParam("apiPath") String apiPath,
		@Context ContainerRequestContext httpRequest
	) {
		return request(apiName, apiPath, httpRequest);
	}
	
	@PUT
	@Body
	@Path("/request/{apiName}/{apiPath:.*}")
	public Response requestPUT(
		@NotBlank @PathParam("apiName") String apiName,
		@PathParam("apiPath") String apiPath,
		@Context ContainerRequestContext httpRequest
	) {
		return request(apiName, apiPath, httpRequest);
	}
	
	@DELETE
	@Body
	@Path("/request/{apiName}/{apiPath:.*}")
	public Response requestDELETE(
		@NotBlank @PathParam("apiName") String apiName,
		@PathParam("apiPath") String apiPath,
		@Context ContainerRequestContext httpRequest
	) {
		return request(apiName, apiPath, httpRequest);
	}
	
	@HEAD
	@Body
	@Path("/request/{apiName}/{apiPath:.*}")
	public Response requestHEAD(
		@NotBlank @PathParam("apiName") String apiName,
		@PathParam("apiPath") String apiPath,
		@Context ContainerRequestContext httpRequest
	) {
		return request(apiName, apiPath, httpRequest);
	}
	
	@PATCH
	@Body
	@Path("/request/{apiName}/{apiPath:.*}")
	public Response requestPATCH(
		@NotBlank @PathParam("apiName") String apiName,
		@PathParam("apiPath") String apiPath,
		@Context ContainerRequestContext httpRequest
	) {
		return request(apiName, apiPath, httpRequest);
	}
	
}
