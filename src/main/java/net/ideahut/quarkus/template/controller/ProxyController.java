package net.ideahut.quarkus.template.controller;

import java.util.List;

import jakarta.inject.Inject;
import jakarta.validation.constraints.NotBlank;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.HEAD;
import jakarta.ws.rs.PATCH;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerResponseContext;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.HttpHeaders;
import net.ideahut.quarkus.annotation.Public;
import net.ideahut.quarkus.api.ApiService;
import net.ideahut.quarkus.api.ApiTokenSysParam;
import net.ideahut.quarkus.helper.ObjectHelper;
import net.ideahut.quarkus.helper.StringHelper;
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
	public void token(
		@NotBlank @QueryParam("apiName") String apiName
	) {
		String token = apiService.getApiTokenService().retrieveApiToken(apiService, apiName);
		if (!StringHelper.isEmpty(token) && ObjectHelper.isInstance(ApiTokenSysParam.class, apiService.getApiTokenService())) {
			((ApiTokenSysParam) apiService.getApiTokenService()).updateSysParamApiToken(apiName, token);
		}
	}

	/*
	 * Proxy request ke ApiService lain
	 * - menggunakan ApiToken yang tersimpan di SysParam
	 * - semua http method diijinkan
	 * 
	 */
	private byte[] request(
		String apiName,
		ContainerRequestContext httpRequest,
		ContainerResponseContext httpResponse
	) {
		// replace prefix path, dan path sisanya akan diappend ke base url service yang dituju
		String path = httpRequest
		.getUriInfo()
		.getPath()
		.replace("/proxy/request/" + apiName, "");
		String apiToken = apiService.getApiTokenService().getSysParamApiToken(apiName);
		RestRequest restRequest = new RestRequest()
		.setPath(path)
		.setMethod(RestMethod.valueOf(httpRequest.getMethod().toUpperCase()))
		.setQueryString(WebHelper.getQueryString(httpRequest))
		.setHeaders(WebHelper.getHeaders(httpRequest));
		ObjectHelper.callOrElse(
			!RestMethod.GET.equals(restRequest.getMethod()), 
			() -> {
				byte[] requestBody = WebHelper.getBodyAsBytes(httpRequest);
				return restRequest.setBody(requestBody);
			}, 
			() -> restRequest.getHeaders().remove(HttpHeaders.CONTENT_LENGTH)
		);
		RestResponse restResponse = apiService.callApiEndpoint(apiName, restRequest, apiToken);
		httpResponse.setStatus(restResponse.getStatus());
		for (String restHeaderName : restResponse.getHeaderNames()) {
			List<String> restHeaderValues = restResponse.getHeaderValues(restHeaderName);
			for (String restHeaderValue : restHeaderValues) {
				httpResponse.getHeaders().add(restHeaderName, restHeaderValue);
			}
		}
		return restResponse.getBodyAsByteArray();
	}
	
	@GET
	@Path("/request")
	public byte[] requestGET(
		@NotBlank @QueryParam("apiName") String apiName,
		@Context ContainerRequestContext httpRequest,
		@Context ContainerResponseContext httpResponse
	) {
		return request(apiName, httpRequest, httpResponse);
	}
	
	@POST
	@Path("/request")
	public byte[] requestPOST(
		@NotBlank @QueryParam("apiName") String apiName,
		@Context ContainerRequestContext httpRequest,
		@Context ContainerResponseContext httpResponse
	) {
		return request(apiName, httpRequest, httpResponse);
	}
	
	@PUT
	@Path("/request")
	public byte[] requestPUT(
		@NotBlank @QueryParam("apiName") String apiName,
		@Context ContainerRequestContext httpRequest,
		@Context ContainerResponseContext httpResponse
	) {
		return request(apiName, httpRequest, httpResponse);
	}
	
	@DELETE
	@Path("/request")
	public byte[] requestDELETE(
		@NotBlank @QueryParam("apiName") String apiName,
		@Context ContainerRequestContext httpRequest,
		@Context ContainerResponseContext httpResponse
	) {
		return request(apiName, httpRequest, httpResponse);
	}
	
	@HEAD
	@Path("/request")
	public byte[] requestHEAD(
		@NotBlank @QueryParam("apiName") String apiName,
		@Context ContainerRequestContext httpRequest,
		@Context ContainerResponseContext httpResponse
	) {
		return request(apiName, httpRequest, httpResponse);
	}
	
	@PATCH
	@Path("/request")
	public byte[] requestPATCH(
		@NotBlank @QueryParam("apiName") String apiName,
		@Context ContainerRequestContext httpRequest,
		@Context ContainerResponseContext httpResponse
	) {
		return request(apiName, httpRequest, httpResponse);
	}
	
}
