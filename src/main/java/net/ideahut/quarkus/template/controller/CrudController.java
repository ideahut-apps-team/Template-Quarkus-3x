package net.ideahut.quarkus.template.controller;


import java.util.List;
import java.util.Map;

import jakarta.validation.constraints.NotBlank;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.core.Context;
import net.ideahut.quarkus.annotation.Public;
import net.ideahut.quarkus.crud.CrudAction;
import net.ideahut.quarkus.crud.CrudHandler;
import net.ideahut.quarkus.crud.CrudInput;
import net.ideahut.quarkus.crud.CrudPermission;
import net.ideahut.quarkus.helper.StringHelper;
import net.ideahut.quarkus.helper.WebHelper;
import net.ideahut.quarkus.object.Page;
import net.ideahut.quarkus.object.Result;
import net.ideahut.quarkus.task.TaskHandler;

@Public
@Path("/crud")
class CrudController extends net.ideahut.quarkus.crud.CrudController {
	
	private final CrudHandler crudHandler;
	private final CrudPermission crudPermission;
	
	CrudController(
		CrudHandler crudHandler, 
		CrudPermission crudPermission
	) {
		this.crudHandler = crudHandler;
		this.crudPermission = crudPermission;
	}
	
	@Override
	protected CrudHandler crudHandler() {
		return crudHandler;
	}
	
	@Override
	protected TaskHandler taskHandler() {
		return null;
	}
	
	@Override
	protected CrudPermission crudPermission() {
		return crudPermission;
	}
	
	
	/*
	 * INFO
	 */
	@GET
	@Path("/info/constant")
	public Result infoConstant() {
		return super.constant();
	}
	
	
	/*
	 * BULK LIST
	 */
	@POST
	@Path("/bulk/list")
	public List<Result> bulkList(@Context ContainerRequestContext request) {
		byte[] data = WebHelper.getBodyAsBytes(request);
		return super.bulkList(data);
	}
	
	
	/*
	 * BULK MAP
	 */
	@POST
	@Path("/bulk/map")
	public Map<String, Result> bulkMap(@Context ContainerRequestContext request) {
		byte[] data = WebHelper.getBodyAsBytes(request);
		return super.bulkMap(data);
	}
	
	
	/*
	 * BODY ACTION
	 */
	@POST
	@Path("/action/{action}")
	public Result action(
		@NotBlank @PathParam("action") String action,
		@Context ContainerRequestContext request
	) {
		byte[] data = WebHelper.getBodyAsBytes(request);
		return super.body(CrudAction.valueOf(action.toUpperCase()), data);
	}
	
	
	/*
	 * PARAMETER
	 */
	/*
	@GET
	@POST
	@PUT
	@DELETE
	@Path("/parameter/{action}")
	public Result parameter(
		@NotBlank @PathParam("action") String action,
		@Context ContainerRequestContext request
	) {
		return super.parameter(CrudAction.valueOf(action.toUpperCase()), request);		
	}
	*/
	
	
	/*
	 * OBJECT (CrudAction.SINGLE)
	 */
	@GET
	@Path("/rest/{name}/{id}")
	public Result object(
		@NotBlank @PathParam("name") String name, 
		@NotBlank @PathParam("id") String id,
		@QueryParam("manager") String manager
	) {
		CrudInput input = new CrudInput()
		.setManager(manager)
		.setName(name)
		.setId(id);
		return super.object(input);
	}
	
	
	/*
	 * COLLECTION (CrudAction.PAGE)
	 */
	@GET
	@Path("/rest/{name}/{index}/{size}")
	public Result collection(
		@PathParam("name") String name, 
		@PathParam("index") Integer index, 
		@PathParam("size") Integer size,
		@QueryParam("manager") String manager,
		@QueryParam("count") String count,
		@QueryParam("filters") String filters,
		@QueryParam("orders") String orders,
		@QueryParam("fields") String fields,
		@QueryParam("excludes") String excludes,
		@QueryParam("loads") String loads		
	) {
		Boolean pgcount = StringHelper.valueOf(Boolean.class, count, Boolean.FALSE);
		Page page = Page.of(index, size, pgcount);
		CrudInput input = new CrudInput()
		.setManager(manager)
		.setName(name)
		.setPage(page)
		.setFilters(filters)
		.setOrders(orders)
		.setFields(fields)
		.setExcludes(excludes)
		.setLoads(loads);
		return super.collection(input);
	}
	
	
	/*
	 * CREATE
	 */
	@POST
	@Path("/rest/{name}")
	public Result create(
		@NotBlank @PathParam("name") String name,
		@QueryParam("manager") String manager,
		@QueryParam("value") String value,
		@Context ContainerRequestContext request
	) {
		byte[] data = WebHelper.getBodyAsBytes(request);
		CrudInput input = new CrudInput()
		.setManager(manager)
		.setName(name)
		.setValue(value)
		.setData(data);
		return super.create(input);
	}
	
	
	/*
	 * UPDATE
	 */
	@PUT
	@Path("/rest/{name}/{id}")
	public Result update(
		@NotBlank @PathParam("name") String name,
		@NotBlank @PathParam("id") String id,
		@QueryParam("manager") String manager,
		@QueryParam("value") String value,
		@Context ContainerRequestContext request
	) {
		byte[] data = WebHelper.getBodyAsBytes(request);
		CrudInput input = new CrudInput()
		.setManager(manager)
		.setName(name)
		.setId(id)
		.setValue(value)
		.setData(data);
		return super.update(input);
	}
	
	
	/*
	 * DELETE 
	 */
	@DELETE
	@Path("/rest/{name}/{id}")
	public Result delete(
		@NotBlank @PathParam("name") String name,
		@NotBlank @PathParam("id") String id,
		@QueryParam("manager") String manager
	) {
		CrudInput input = new CrudInput()
		.setManager(manager)
		.setName(name)
		.setId(id);
		return super.delete(input);
	}
	
}
