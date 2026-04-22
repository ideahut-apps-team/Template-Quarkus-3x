package net.ideahut.quarkus.template.controller;

import java.util.UUID;

import jakarta.validation.Valid;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import net.ideahut.quarkus.annotation.Public;
import net.ideahut.quarkus.init.InitRequest;

@Public
@Path("/")
class RootController {

	@GET
	@Path("/")
	public void get() {
		/**/
	}
	
	@GET
	@Path("/favicon.ico")
	public void favicon() {
		/**/
	}
	
	@POST
	@Path("/init")
	public String init(@Valid InitRequest initRequest) {
        return UUID.randomUUID().toString() + initRequest;
    }
	
}
