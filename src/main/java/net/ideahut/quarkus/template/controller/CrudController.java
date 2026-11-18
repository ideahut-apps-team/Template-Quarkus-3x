package net.ideahut.quarkus.template.controller;


import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import jakarta.inject.Inject;
import jakarta.validation.constraints.NotBlank;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.HttpMethod;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.core.Context;
import net.ideahut.quarkus.annotation.Public;
import net.ideahut.quarkus.crud.CrudAction;
import net.ideahut.quarkus.crud.CrudControllerBase;
import net.ideahut.quarkus.crud.CrudHandler;
import net.ideahut.quarkus.crud.CrudPermission;
import net.ideahut.quarkus.crud.CrudResource;
import net.ideahut.quarkus.helper.ErrorHelper;
import net.ideahut.quarkus.helper.StringHelper;
import net.ideahut.quarkus.object.Result;
import net.ideahut.quarkus.task.TaskHandler;

/*
 * CrudPermission dan CrudResource bisa di level Handler ataupun di lever Controller.
 * Untuk di level Handler akan berlaku disetiap penggunaan CrudHandler
 * Untuk di level Controller hanya akan berlaku di setiap pemanggilan endpoint Crud
 */

@Public
@Path("/crud")
class CrudController extends CrudControllerBase {
	
	private static final Map<String, Collection<CrudAction>> METHOD_CRUD_ACTIONS;
	static {
		METHOD_CRUD_ACTIONS = new HashMap<>();
		METHOD_CRUD_ACTIONS.put(HttpMethod.GET, Arrays.asList(
			CrudAction.UNIQUE, CrudAction.SINGLE, CrudAction.PAGE, CrudAction.LIST, CrudAction.MAP
		));
		METHOD_CRUD_ACTIONS.put(HttpMethod.POST, Arrays.asList(
			CrudAction.CREATE, CrudAction.CREATES, CrudAction.SAVE, CrudAction.SAVES
		));
		METHOD_CRUD_ACTIONS.put(HttpMethod.PUT, Arrays.asList(
			CrudAction.UPDATE, CrudAction.UPDATES, CrudAction.SAVE, CrudAction.SAVES
		));
		METHOD_CRUD_ACTIONS.put(HttpMethod.DELETE, Arrays.asList(
			CrudAction.DELETE, CrudAction.DELETES
		));
	}
	
	private final CrudHandler crudHandler;
	private final CrudResource crudResource;
	private final CrudPermission crudPermission;
	
	@Inject
	CrudController(
		CrudHandler crudHandler,
		CrudResource crudResource,
		CrudPermission crudPermission
	) {
		this.crudHandler = crudHandler;
		this.crudResource = crudResource;
		this.crudPermission = crudPermission;
	}
	
	@Override
	protected CrudHandler crudHandler() {
		return crudHandler;
	}
	
	@Override
	protected CrudResource crudResource() {
		return crudResource;
	}
	
	@Override
	protected CrudPermission crudPermission() {
		return crudPermission;
	}
	
	@Override
	protected TaskHandler taskHandler() {
		return null;
	}
	
	/*
	 * CONSTANT
	 */
	@Override
	@GET
	@Path(value = "/constant")
	public Result constant() {
		return super.constant();
	}
	
	/*
	 * BULK LIST
	 */
	@Override
	@POST
	@Path("/bulk/list")
	public List<Result> bulkList(
		@Context ContainerRequestContext httpRequest
	) {
		return super.bulkList(httpRequest);
	}
	
	/*
	 * BULK MAP
	 */
	@Override
	@POST
	@Path("/bulk/map")
	public Map<String, Result> bulkMap(
		@Context ContainerRequestContext httpRequest
	) {
		return super.bulkMap(httpRequest);
	}
	
	/*
	 * ACTION
	 */
	@POST
	@Path("/action/{action}")
	public Result action(
		@NotBlank @PathParam("action") String action,
		@Context ContainerRequestContext httpRequest
	) {
		return super.body(CrudAction.of(action), httpRequest);
	}
	
	/*
	 * PARAMETER
	 */
	private Result parameter(
		String action, 
		ContainerRequestContext httpRequest
	) {
		String httpMethod = httpRequest.getMethod();
		CrudAction crudAction = CrudAction.of(action);
		Collection<CrudAction> crudActions = METHOD_CRUD_ACTIONS.get(httpMethod);
		ErrorHelper.throwEmpty(crudActions, () -> StringHelper.format("Unsupported method: {}", httpMethod));
		ErrorHelper.throwIf(!crudActions.contains(crudAction), () -> StringHelper.format("Invald action: {} for method: {}", crudAction, httpMethod));
		return super.parameter(crudAction, httpRequest);
	}
	
	@GET
	@Path("/parameter/{action}")
	public Result parameterGET(
		@NotBlank @PathParam("action") String action,
		@Context ContainerRequestContext httpRequest
	) {
		return parameter(action, httpRequest);
	}
	
	@POST
	@Path("/parameter/{action}")
	public Result parameterPOST(
		@NotBlank @PathParam("action") String action,
		@Context ContainerRequestContext httpRequest
	) {
		return parameter(action, httpRequest);		
	}
	
	@PUT
	@Path("/parameter/{action}")
	public Result parameterPUT(
		@NotBlank @PathParam("action") String action,
		@Context ContainerRequestContext httpRequest
	) {
		return parameter(action, httpRequest);	
	}
	
	@DELETE
	@Path("/parameter/{action}")
	public Result parameterDELETE(
		@NotBlank @PathParam("action") String action,
		@Context ContainerRequestContext httpRequest
	) {
		return parameter(action, httpRequest);	
	}
	
	/*
	 * PAGE
	 */
	@Override
	@GET
	@Path("/rest/page")
	public Result page(
		@Context ContainerRequestContext httpRequest		
	) {
		return super.page(httpRequest);
	}
	
	/*
	 * SINGLE
	 */
	@Override
	@GET
	@Path("/rest")
	public Result single(
		@Context ContainerRequestContext httpRequest
	) {
		return super.single(httpRequest);
	}
	
	/*
	 * CREATE
	 */
	@Override
	@POST
	@Path("/rest")
	public Result create(
		@Context ContainerRequestContext httpRequest
	) {
		return super.create(httpRequest);
	}
	
	/*
	 * UPDATE
	 */
	@Override
	@PUT
	@Path("/rest")
	public Result update(
		@Context ContainerRequestContext httpRequest
	) {
		return super.update(httpRequest);
	}
	
	/*
	 * DELETE 
	 */
	@Override
	@DELETE
	@Path("/rest")
	public Result delete(
		@Context ContainerRequestContext httpRequest
	) {
		return super.delete(httpRequest);
	}
	
}
