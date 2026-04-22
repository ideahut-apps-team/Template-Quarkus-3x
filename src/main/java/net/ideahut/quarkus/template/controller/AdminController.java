package net.ideahut.quarkus.template.controller;


import jakarta.ws.rs.Path;
import net.ideahut.quarkus.admin.AdminControllerBase;
import net.ideahut.quarkus.admin.AdminHandler;
import net.ideahut.quarkus.annotation.ApiExclude;
import net.ideahut.quarkus.mapper.DataMapper;
import net.ideahut.quarkus.security.WebSecurity;

@ApiExclude
@Path("/_/api")
class AdminController extends AdminControllerBase {
	
	private final DataMapper dataMapper;
	private final AdminHandler adminHandler;
	private final WebSecurity webSecurity;
	
	AdminController(
		DataMapper dataMapper,
		AdminHandler adminHandler,
		WebSecurity webSecurity
	) {
		this.dataMapper = dataMapper;
		this.adminHandler = adminHandler;
		this.webSecurity = webSecurity;
	}
	
	@Override
	protected DataMapper dataMapper() {
		return dataMapper;
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
