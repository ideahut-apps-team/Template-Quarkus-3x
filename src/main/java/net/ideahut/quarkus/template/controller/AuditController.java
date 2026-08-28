package net.ideahut.quarkus.template.controller;


import jakarta.inject.Inject;
import jakarta.validation.constraints.NotBlank;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.core.Context;
import net.ideahut.quarkus.audit.AuditHandler;
import net.ideahut.quarkus.audit.AuditRequest;
import net.ideahut.quarkus.helper.ErrorHelper;
import net.ideahut.quarkus.helper.ObjectHelper;
import net.ideahut.quarkus.helper.StringHelper;
import net.ideahut.quarkus.helper.WebHelper;
import net.ideahut.quarkus.object.Page;
import net.ideahut.quarkus.object.Result;
import net.ideahut.quarkus.template.Application;

/*
 * API untuk melihat data audit
 */
@Path("/audit")
class AuditController {
	
	private final AuditHandler auditHandler;
	
	@Inject
	AuditController(
		AuditHandler auditHandler
	) {
		this.auditHandler = auditHandler;
	}
	
	
	@POST
	@Path("/list")
	public Result list(
		@Context ContainerRequestContext httpRequest
	) {
		byte[] data = WebHelper.getBodyAsBytes(httpRequest);
		AuditRequest auditRequest = auditHandler.getRequest(data);
		String entity = ObjectHelper.useOrDefault(auditRequest.getEntity(), "").trim();
		ObjectHelper.callIf(
			!StringHelper.isEmpty(entity) && auditRequest.getClassOfEntity() == null, 
			() -> {
				Class<?> classOfEntity = ObjectHelper.useOrDefault(
					ObjectHelper.safeClassOf(entity), 
					() -> ObjectHelper.safeClassOf(Application.Package.APPLICATION + ".entity." + entity)
				);
				ErrorHelper.throwNull(classOfEntity, () -> "Entity not found: " + entity);
				return auditRequest.setClassOfEntity(classOfEntity);
			}
		);			
		Page page = auditHandler.getList(auditRequest);
		return Result.success(page);
	}
	
	@GET
	@Path("/bytes")
	public Result bytes(
		@QueryParam("manager") String manager,
		@NotBlank @QueryParam("id") String id
	) {
		byte[] bytes = auditHandler.getBytes(manager, id);
		return Result.success(bytes);
	}
	
}
