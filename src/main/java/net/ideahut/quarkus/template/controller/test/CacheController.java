package net.ideahut.quarkus.template.controller.test;


import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import jakarta.inject.Inject;
import jakarta.validation.constraints.NotBlank;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.core.Context;
import lombok.extern.slf4j.Slf4j;
import net.ideahut.quarkus.annotation.Public;
import net.ideahut.quarkus.bean.BeanConfigure;
import net.ideahut.quarkus.cache.CacheGroupHandler;
import net.ideahut.quarkus.cache.CacheGroupProperties;
import net.ideahut.quarkus.cache.CacheHandler;
import net.ideahut.quarkus.definition.CacheGroupDefinition;
import net.ideahut.quarkus.helper.ObjectHelper;
import net.ideahut.quarkus.helper.StringHelper;
import net.ideahut.quarkus.helper.TimeHelper;
import net.ideahut.quarkus.helper.WebHelper;
import net.ideahut.quarkus.mapper.DataMapper;
import net.ideahut.quarkus.object.Result;
import net.ideahut.quarkus.task.TaskListExecutor;
import net.ideahut.quarkus.template.app.AppProperties;
import net.ideahut.quarkus.template.object.CacheData;

/*
 * Contoh penggunaan CacheHandler
 */
@Slf4j
@Public
@Path("/test/cache")
class CacheController implements BeanConfigure {
	
	private static final String GROUP = "group";
	private static final String KEY = "key";
	
	private static final String DEFAULT_GROUP = "GROUP-01";

	private final AppProperties appProperties;
	private final DataMapper dataMapper;
	private final CacheGroupHandler cacheGroupHandler;
	private final CacheHandler cacheSingleHandler;
	
	@Inject
	CacheController(
		AppProperties appProperties,
		DataMapper dataMapper,
		CacheGroupHandler cacheGroupHandler,
		CacheHandler cacheSingleHandler
	) {
		this.appProperties = appProperties;
		this.dataMapper = dataMapper;
		this.cacheGroupHandler = cacheGroupHandler;
		this.cacheSingleHandler = cacheSingleHandler;
	}
	
	private List<CacheGroupProperties> groups = Collections.emptyList();
	
	@Override
	public void onConfigureBean() throws Exception {
		groups = CacheGroupDefinition.convert(appProperties.cache().orElseThrow().groups().orElseThrow());
	}
	
	@GET
	@Path("/groups")
	public ArrayNode groups() {
		ArrayNode items = dataMapper.createArrayNode();
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
		@QueryParam(GROUP) String inGroup,
		@NotBlank @QueryParam(KEY) String key
	) {
		String group = getCacheGroup(inGroup);
		Boolean[] cached = { Boolean.TRUE };
		CacheData data = cacheGroupHandler.get(
			CacheData.class, 
			group, 
			key, 
			() -> {
				cached[0] = Boolean.FALSE;
				return createCacheData(group, key);
			}
		);
		return Result.success(data)
		.setInfo(GROUP, group)
		.setInfo("cached", cached[0]);
	}
	
	@GET
	@Path("/size")
	public Result size(
		@QueryParam(GROUP) String inGroup
	) {
		String group = getCacheGroup(inGroup);
		Long size = cacheGroupHandler.size(group);
		return Result.success()
		.setInfo(GROUP, group)
		.setInfo("size", size);
	}
	
	@GET
	@Path("/keys")
	public Result keys(
		@QueryParam(GROUP) String inGroup
	) {
		String group = getCacheGroup(inGroup);
		List<String> keys = cacheGroupHandler.keys(group);
		return Result.success(keys)
		.setInfo(GROUP, group);
	}
	
	@DELETE
	@Path("/delete")
	public Result delete(
		@QueryParam(GROUP) String inGroup,
		@NotBlank @QueryParam(KEY) String key
	) {
		String group = getCacheGroup(inGroup);
		cacheGroupHandler.delete(group, key);
		return Result.success()
		.setInfo(GROUP, group)
		.setInfo(KEY, key);
	}
	
	@DELETE
	@Path("/clear")
	public Result clear(
		@QueryParam(GROUP) String inGroup
	) {
		String group = getCacheGroup(inGroup);
		cacheGroupHandler.clear(group);
		return Result.success()
		.setInfo(GROUP, group);
	}
	
	@GET
	@Path("/any")
	public CacheData any(
		@QueryParam(GROUP) String group, 
		@NotBlank @QueryParam(KEY) String key
	) {
		return ObjectHelper.callOrElse(
			!StringHelper.isBlank(group),
			() -> cacheGroupHandler.get(CacheData.class, group, key, () -> {
				log.info("--- Group-Callable, group: {}, key: {}", group, key);
				return createCacheData(group, key);
			}), 
			() -> cacheSingleHandler.get(CacheData.class, key, () -> {
				log.info("--- Single-Callable, key: {}", key);
				return createCacheData(group, key);
			})
		);
	}
	
	@GET
	@Path("/list")
	public List<CacheData> list(
		@QueryParam(GROUP) String group, 
		@Context ContainerRequestContext httpRequest
	) {
		List<String> keys = WebHelper.getParameters(httpRequest, KEY);
		return ObjectHelper.callOrElse(
			!StringHelper.isBlank(group),
			() -> cacheGroupHandler.multiList(CacheData.class, group, keys), 
			() -> cacheSingleHandler.multiList(CacheData.class, keys)
		);
	}
	
	@GET
	@Path("/map")
	public Map<String, CacheData> map(
		@QueryParam(GROUP) String group,
		@Context ContainerRequestContext httpRequest
	) {
		List<String> keys = WebHelper.getParameters(httpRequest, KEY);
		return ObjectHelper.callOrElse(
			!StringHelper.isBlank(group),
			() -> cacheGroupHandler.multiMap(CacheData.class, group, keys), 
			() -> cacheSingleHandler.multiMap(CacheData.class, keys)
		);
	}
	
	/*
	 * Simulasi jika permintaan data cache secara bersamaan dalam banyak request
	 * Diharapkan tidak semua request akan melakukan input data ke cache
	 */
	@GET
	@Path("/concurrent")
	public List<Object> concurrent(
		@QueryParam(GROUP) String group,
		@QueryParam("concurrency") Integer concurrency
	) {
		String key = UUID.randomUUID().toString();
		int threads = ObjectHelper.useOrElse(concurrency != null && concurrency > 0, concurrency, 100);
		TaskListExecutor executor = TaskListExecutor.of(threads);
		for (int i = 0; i < threads; i++) {
			int fi = i;
			executor.add(() -> getCacheData(fi, group, key));
		}
		return executor.getObjects();
	}
	
	
	private String getCacheGroup(String inGroup) {
		return ObjectHelper.callOrElse(!StringHelper.isBlank(inGroup), () -> inGroup, () -> DEFAULT_GROUP);
	}
	
	private CacheData getCacheData(int index, String group, String key) {
		return ObjectHelper.callOrElse(
			!StringHelper.isBlank(group),
			() -> cacheGroupHandler.get(CacheData.class, group, key, () -> {
				TimeUnit.SECONDS.sleep(2);
				log.info("--- Group-Concurrent, group: {}, key: {}, index: {}", group, key, index);
				return createCacheData(group, key);
			}), 
			() -> cacheSingleHandler.get(CacheData.class, key, () -> {
				TimeUnit.SECONDS.sleep(2);
				log.info("--- Single-Concurrent: key: {}, index: {}", key, index);
				return createCacheData(group, key);
			})
		);
	}
	
	private CacheData createCacheData(String group, String key) {
		CacheData data = new CacheData();
		data.setContent("Contoh cache - " + UUID.randomUUID());
		data.setGroup(group);
		data.setKey(key);
		data.setTimestamp(TimeHelper.currentEpochMillis());
		return data;
	}
	
}
