package net.ideahut.quarkus.template.controller;


import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.ws.rs.Path;
import net.ideahut.quarkus.admin.AdminControllerBase;
import net.ideahut.quarkus.admin.AdminHandler;
import net.ideahut.quarkus.annotation.ApiExclude;
import net.ideahut.quarkus.security.WebSecurity;
import net.ideahut.quarkus.template.app.AppConstant;

/*
 * API untuk Admin UI
 */
@ApiExclude
@Path("/_/api")
class AdminController extends AdminControllerBase {
	
	private final AdminHandler adminHandler;
	private final WebSecurity webSecurity;
	
	@Inject
	AdminController(
		AdminHandler adminHandler,
		@Named(AppConstant.Bean.Security.ADMIN)
		WebSecurity webSecurity
	) {
		this.adminHandler = adminHandler;
		this.webSecurity = webSecurity;
	}
	
	@Override
	protected AdminHandler adminHandler() {
		return adminHandler;
	}
	
	@Override
	protected WebSecurity webSecurity() {
		return webSecurity;
	}
	
}
