package net.ideahut.quarkus.template.controller.test;


import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import jakarta.inject.Inject;
import jakarta.validation.constraints.NotBlank;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.QueryParam;
import net.ideahut.quarkus.annotation.Public;
import net.ideahut.quarkus.crud.CrudAction;
import net.ideahut.quarkus.grid.GridHandler;
import net.ideahut.quarkus.object.Result;

/*
 * Contoh penggunaan GridHandler
 */
@Path("/test/grid")
class GridController {
	
	private final GridHandler gridHandler;
	
	@Inject
	GridController(
		GridHandler gridHandler	
	) {
		this.gridHandler = gridHandler;
	}

	@Public
	@GET
	public Result get(
		@NotBlank @QueryParam("name") String name,
		@NotBlank @QueryParam("parent") String parent
	) {
		ObjectNode grid = (ObjectNode) gridHandler.getGrid(parent, name);
		if (grid != null) {
			ArrayNode actions = grid.putArray("actions");
			for (CrudAction crudAction : CrudAction.values()) {
				actions.add(crudAction.name());
			}
		}
		return Result.success(grid);
	}
	
}
