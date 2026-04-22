package net.ideahut.quarkus.template.controller;


import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import jakarta.validation.constraints.NotBlank;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.QueryParam;
import net.ideahut.quarkus.annotation.Public;
import net.ideahut.quarkus.cache.CacheGroupHandler;
import net.ideahut.quarkus.cache.CacheGroupProperties;
import net.ideahut.quarkus.definition.CacheGroupDefinition;
import net.ideahut.quarkus.helper.TimeHelper;
import net.ideahut.quarkus.mapper.DataMapper;
import net.ideahut.quarkus.object.Result;
import net.ideahut.quarkus.template.app.AppProperties;
import net.ideahut.quarkus.template.object.CacheData;

/*
 * Contoh penggunaan CacheGroupHandler
 */
@Public
@Path("/cache")
class CacheController {
	
	private static final String GROUP = "TEST1";

	private final AppProperties appProperties;
	private final DataMapper dataMapper;
	private final CacheGroupHandler cacheGroupHandler;
	
	CacheController(
		AppProperties appProperties,
		DataMapper dataMapper,
		CacheGroupHandler cacheGroupHandler	
	) {
		this.appProperties = appProperties;
		this.dataMapper = dataMapper;
		this.cacheGroupHandler = cacheGroupHandler;
	}
	
	@GET
	@Path("/groups")
	public ArrayNode groups() {
		ArrayNode items = dataMapper.createArrayNode();
		List<CacheGroupProperties> groups = CacheGroupDefinition.convert(appProperties.cache().orElseThrow().groups().orElse(null));
		for (CacheGroupProperties group : groups) {
			if (0 != group.getLimit()) {
				Long size = cacheGroupHandler.size(group.getName());
				ObjectNode item = items.addObject();
				item.put("name", group.getName());
				item.put("limit", group.getLimit());
				item.put("size", size);
			}
		}
		return items;
	}
	
	@GET
	@Path("/get")
	public Result get(
		@QueryParam("group") String group,
		@NotBlank @QueryParam("key") String key
	) {
		List<Boolean> flag = new ArrayList<>();
		flag.add(Boolean.TRUE);
		CacheData result = cacheGroupHandler.get(CacheData.class, group(group), key, () -> {
			CacheData data = new CacheData();
			data.setContent("Contoh cache - " + UUID.randomUUID());
			data.setGroup(group(group));
			data.setKey(key);
			data.setTimestamp(TimeHelper.currentEpochMillis());
			flag.set(0, Boolean.FALSE);
			return data;
		});
		return Result.success(result).setInfo("cached", flag.remove(0));
	}
	
	@GET
	@Path("/size")
	public Long size(
		@QueryParam("group") String group
	) {
		return cacheGroupHandler.size(group(group));
	}
	
	@GET
	@Path("/keys")
	public List<String> keys(
		@QueryParam("group") String group
	) {
		return cacheGroupHandler.keys(group(group));
	}
	
	@DELETE
	@Path("/delete")
	public void delete(
		@QueryParam("group") String group,
		@NotBlank @QueryParam("key") String key
	) {
		cacheGroupHandler.delete(group(group), key);
	}
	
	@DELETE
	@Path("/clear")
	public void clear(
		@QueryParam("group") String group
	) {
		cacheGroupHandler.clear(group(group));
	}
	
	private String group(String input) {
		String group = input != null ? input.trim() : "";
		return !group.isEmpty() ? group : GROUP;
	}
	
}
