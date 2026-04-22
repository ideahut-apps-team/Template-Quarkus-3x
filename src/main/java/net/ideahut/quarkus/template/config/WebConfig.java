package net.ideahut.quarkus.template.config;

import java.util.Arrays;

import io.vertx.ext.web.RoutingContext;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Named;
import jakarta.inject.Singleton;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.container.ContainerResponseFilter;
import jakarta.ws.rs.container.ResourceInfo;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;
import net.ideahut.quarkus.admin.AdminHandler;
import net.ideahut.quarkus.api.ApiAccess;
import net.ideahut.quarkus.api.ApiService;
import net.ideahut.quarkus.helper.ObjectHelper;
import net.ideahut.quarkus.security.SecurityCredential;
import net.ideahut.quarkus.security.WebSecurity;
import net.ideahut.quarkus.template.app.AppConstants;
import net.ideahut.quarkus.template.app.AppProperties;
import net.ideahut.quarkus.web.DefaultWebInterceptor;
import net.ideahut.quarkus.web.WebAdvice;
import net.ideahut.quarkus.web.WebInterceptor;
import net.ideahut.quarkus.web.WebRequestFilter;
import net.ideahut.quarkus.web.WebResponseFilter;

class WebConfig {
	
	/*
	 * REQUEST FILTER
	 */
	@Provider
	@ApplicationScoped
	static class RequestFilter extends WebRequestFilter implements ContainerRequestFilter {
		RequestFilter(
			WebInterceptor webInterceptor
		) {
			setAutoDetect(true)
			.setInterceptors(Arrays.asList(webInterceptor));
		}

		@Context
		private ResourceInfo resourceInfo;
		
		@Context
		private RoutingContext routingContext;

		@Override
		protected ResourceInfo resourceInfo() {
			return resourceInfo;
		}

		@Override
		protected RoutingContext routingContext() {
			return routingContext;
		}
		
	}
	
	
	/*
	 * RESPONSE FILTER
	 */
	@Provider
	@ApplicationScoped
	static class ResponseFilter extends WebResponseFilter implements ContainerResponseFilter {
		ResponseFilter() {
			//.setDataMapper(null)
			//.setPathMatcher(null)
			//.setSkipClasses(null)
			//.setSkipPaths(null); //-
		}
	}
	
	
	/*
	 * ADVICE
	 */
	@Provider
	@ApplicationScoped
	public static class Advice extends WebAdvice implements ExceptionMapper<Throwable> {
		@Context
		private ContainerRequestContext containerRequestContext;
		
		@Override
		protected ContainerRequestContext containerRequestContext() {
			return containerRequestContext;
		}
		
		@Override
		protected int exceptionStatus(Throwable exception) {
			if (ObjectHelper.isInstance(NotFoundException.class, exception)) {
				return Response.Status.NOT_FOUND.getStatusCode();
			}
			return Response.Status.OK.getStatusCode();
		}

		// Untuk debug
		/**
		@Override
		public Response toResponse(Throwable exception) {
			exception.printStackTrace();
			return super.toResponse(exception);
		}
		*/
		
	}
	
	/*
	 * WEB INTERCEPTOR
	 */
	@Singleton
	WebInterceptor webInterceptor(
		AppProperties appProperties,
		AdminHandler adminHandler,
		@Named(AppConstants.Bean.Security.ADMIN)
		WebSecurity adminSecurity,
		@Named(AppConstants.Bean.Credential.ADMIN)
		SecurityCredential adminCredential,
		ApiService apiService
	) {
		return new DefaultWebInterceptor()
		.setAdminCredential(adminCredential)
		.setAdminHandler(adminHandler)
		.setAdminSecurity(adminSecurity)
		.setApiService(apiService)
		.setPublicApiAccess(ApiAccess.unmodifiable(new ApiAccess().setApiRole(AppConstants.Default.API_ROLE)))
		.setPublicAuditor("PUBLIC");
	}
	
}
