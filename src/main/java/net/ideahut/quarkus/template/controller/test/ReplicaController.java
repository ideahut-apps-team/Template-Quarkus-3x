package net.ideahut.quarkus.template.controller.test;


import java.util.List;

import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import net.ideahut.quarkus.annotation.Public;
import net.ideahut.quarkus.entity.EntityInfo;
import net.ideahut.quarkus.entity.EntityReplica;
import net.ideahut.quarkus.entity.EntityTrxManager;
import net.ideahut.quarkus.entity.TrxManagerInfo;
import net.ideahut.quarkus.entity.replica.ReplicaInfo;
import net.ideahut.quarkus.object.Result;
import net.ideahut.quarkus.template.entity.Information;
import net.ideahut.quarkus.template.entity.InformationLink;

/*
 * Contoh untuk EntityReplica
 */
@Public
@Path("/test/replica")
class ReplicaController {
	
	private final EntityTrxManager entityTrxManager;
	
	@Inject
	ReplicaController(
		EntityTrxManager entityTrxManager	
	) {
		this.entityTrxManager = entityTrxManager;
	}

	@POST
	@Path("/create/information")
	public Result createInformationReplica() {
		TrxManagerInfo trxManagerInfo = entityTrxManager.getDefaultTrxManagerInfo();
		EntityInfo entityInfo = trxManagerInfo.getEntityInfo(Information.class);
		List<EntityReplica.Creation> creations = EntityReplica.create(entityInfo, 2);
		return Result.success(creations);
	}
	
	@GET
	@Path("/sql/information")
	public Result sqlInformationReplica() {
		TrxManagerInfo trxManagerInfo = entityTrxManager.getDefaultTrxManagerInfo();
		EntityInfo entityInfo = trxManagerInfo.getEntityInfo(Information.class);
		List<String> sqls = EntityReplica.getSQL(entityInfo, 2);
		return Result.success(sqls);
	}
	
	@POST
	@Path("/create/information/link")
	public Result createInformationLinkReplica() {
		TrxManagerInfo trxManagerInfo = entityTrxManager.getDefaultTrxManagerInfo();
		EntityInfo entityInfo = trxManagerInfo.getEntityInfo(InformationLink.class);
		EntityInfo refEntityInfo = trxManagerInfo.getEntityInfo(Information.class);
		List<EntityReplica.Creation> creations = EntityReplica.create(entityInfo, 2, refEntityInfo);
		return Result.success(creations);
	}
	
	@GET
	@Path(value = "/sql/information/link")
	public Result sqlInformationLinkReplica() {
		TrxManagerInfo trxManagerInfo = entityTrxManager.getDefaultTrxManagerInfo();
		EntityInfo entityInfo = trxManagerInfo.getEntityInfo(InformationLink.class);
		EntityInfo refEntityInfo = trxManagerInfo.getEntityInfo(Information.class);
		List<String> sqls = EntityReplica.getSQL(entityInfo, 2, refEntityInfo);
		return Result.success(sqls);
	}
	
	@GET
	@Path("/latest")
	public Integer latest() {
		return ReplicaInfo.getLatestReplica(entityTrxManager.getDefaultTrxManagerInfo(), Information.class);
	}
	
}
