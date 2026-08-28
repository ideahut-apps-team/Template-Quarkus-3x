package net.ideahut.quarkus.template.service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import io.quarkus.redis.datasource.RedisDataSource;
import jakarta.enterprise.context.ApplicationScoped;
import net.ideahut.quarkus.bean.BeanConfigure;
import net.ideahut.quarkus.bean.BeanReload;
import net.ideahut.quarkus.context.RequestContext;
import net.ideahut.quarkus.entity.EntityTrxManager;
import net.ideahut.quarkus.helper.FrameworkHelper;
import net.ideahut.quarkus.helper.ObjectHelper;
import net.ideahut.quarkus.helper.StringHelper;
import net.ideahut.quarkus.io.Resource;
import net.ideahut.quarkus.mapper.DataMapper;
import net.ideahut.quarkus.message.MessageHandler;
import net.ideahut.quarkus.message.RedisMessageHandler;
import net.ideahut.quarkus.message.dto.LanguageDto;
import net.ideahut.quarkus.object.Message;
import net.ideahut.quarkus.object.Option;
import net.ideahut.quarkus.object.StringList;
import net.ideahut.quarkus.object.StringMap;
import net.ideahut.quarkus.object.StringSet;
import net.ideahut.quarkus.redis.RedisCommand;
import net.ideahut.quarkus.redis.RedisParam;
import net.ideahut.quarkus.template.app.AppConstant;
import net.ideahut.quarkus.template.app.AppProperties;

@ApplicationScoped
class MessageServiceImpl implements MessageService, BeanReload, BeanConfigure {
	
	private static final class Keys {
		private Keys() {}
		private static String resources(String prefix) {
			return prefix + "RESOURCES";
		}
		private static String resource(String prefix, String type, String language) {
			return prefix + "RESOURCE-" + type + "-" + language;
		}
	}

	private static final String DEFAULT_LANGUAGE = "id";
	
	private boolean configured = false;
	private RedisCommand<String, byte[]> redisCommand;
	private String redisPrefix;
	private RedisMessageHandler messageHandler;
	
	private List<Option> activeLanguages;
	private AppProperties appProperties;
	private DataMapper dataMapper;
	
	
	@Override
	public void onConfigureBean() throws Exception {
		appProperties = FrameworkHelper.getBean(AppProperties.class);
		dataMapper = FrameworkHelper.getBean(DataMapper.class);
		EntityTrxManager entityTrxManager = FrameworkHelper.getBean(EntityTrxManager.class);
		RedisDataSource redisDataSource = FrameworkHelper.getBean(AppConstant.Bean.Redis.PRIMARY, RedisDataSource.class);
		redisCommand = RedisCommand.of(redisDataSource, String.class, byte[].class);
		RedisParam redisParam = new RedisParam()
		.setAppIdEnabled(true)
		.setEncryptEnabled(true)
		.setPrefix("MESSAGE")
		.setDataSource(redisDataSource)
		.prepareDefault();
		redisPrefix = FrameworkHelper.createStorageKeyPrefix(redisParam);
		messageHandler = new RedisMessageHandler()
		.setDefaultLanguage(DEFAULT_LANGUAGE)
		.setEntityTrxManager(entityTrxManager)
		.setLimitReloadData(100)
		.setMaxReloadThread(3)
		.setRedisParam(redisParam);
		messageHandler.afterPropertiesSet();
		messageHandler.onConfigureBean();
		onReloadBean();
		configured = true;
	}

	@Override
	public boolean isBeanConfigured() {
		return configured;
	}

	@Override
	public boolean onReloadBean() throws Exception {
		return ObjectHelper.callOrElse(
			configured && !messageHandler.onReloadBean(), 
			() -> false, 
			() -> {
				activeLanguages = new ArrayList<>();
				for (LanguageDto language : messageHandler.getActiveLanguages().values()) {
					activeLanguages.add(new Option(language.getLanguageCode(), language.getName()));
				}
				clearResources();
				loadResources();
				return true;
			}
		);
	}
	
	@Override
	public List<Option> getActiveLanguages() {
		return activeLanguages;
	}

	@Override
	public String getDefaultLanguage() {
		return DEFAULT_LANGUAGE;
	}

	@Override
	public JsonNode getResource(String type) {
		String language = getRequestLanguage();
		ObjectNode node = dataMapper.createObjectNode();
		node.putArray("languages").addAll(dataMapper.convert(activeLanguages, ArrayNode.class));
		node.put("active", language);
		String ckey = Keys.resource(redisPrefix, type, language);
		byte[] bytes = redisCommand.valueGet(ckey);
		ObjectHelper.callIf(bytes != null, () -> node.set("message", dataMapper.read(bytes, JsonNode.class)));
		return node;
	}
	
	@Override
	public String getText(String code, boolean checkArgs, String... args) {
		getRequestLanguage();
		return messageHandler.getText(code, checkArgs, args);
	}

	@Override
	public String getText(String code, String... args) {
		return messageHandler.getText(code, args);
	}

	@Override
	public String getText(String code) {
		return messageHandler.getText(code);
	}

	@Override
	public Message getMessage(String code, boolean checkArgs, String... args) {
		return messageHandler.getMessage(code, checkArgs, args);
	}

	@Override
	public Message getMessage(String code, String... args) {
		return messageHandler.getMessage(code, args);
	}

	@Override
	public Message getMessage(String code) {
		return messageHandler.getMessage(code);
	}

	@Override
	public StringMap getMap(String... codes) {
		return messageHandler.getMap(codes);
	}

	@Override
	public StringList getList(String... codes) {
		getRequestLanguage();
		return messageHandler.getList(codes);
	}

	private String getRequestLanguage() {
		String language = RequestContext.currentContext().getAttribute(MessageHandler.Attribute.LANGUAGE + "_REQ");
		return ObjectHelper.callOrElse(
			!StringHelper.isBlank(language), 
			() -> language, 
			() -> {
				String acceptLang = RequestContext.currentContext().getAttribute(MessageHandler.Attribute.LANGUAGE);
				if (!isValidLanguage(acceptLang)) {
					acceptLang = DEFAULT_LANGUAGE;
				}
				RequestContext.currentContext().setAttribute(MessageHandler.Attribute.LANGUAGE + "_REQ", acceptLang);
				return acceptLang;
			}
		);
	}
	
	private boolean isValidLanguage(String language) {
		return ObjectHelper.callOrElse(
			StringHelper.isBlank(language), 
			() -> false, 
			() -> {
				Option option = getActiveLanguages()
				.stream()
				.filter(o -> language.equals(o.getValue()))
				.findAny()
				.orElse(null);
				return option != null;
			}
		);
	}
	
	private Map<String, byte[]> getResources(String type) {
		Map<String, byte[]> map = new HashMap<>();
		String path = FrameworkHelper.replacePath(StringHelper.removeEnd(appProperties.messagePath().orElseThrow(), "/"));
		Resource[] resources = FrameworkHelper.getResources(path + "/" + type + "/*.json");
		for (Resource resource : resources) {
			String filename = ObjectHelper.useOrDefault(resource.getFilename(), "");
			ObjectHelper.callIf(
				!filename.isEmpty(), 
				() -> {
					String language = filename.replace(".json", "");
					return ObjectHelper.callIf(
						isValidLanguage(language), 
						() -> {
							byte[] bytes = FrameworkHelper.toByteArray(resource);
							String ckey = Keys.resource(redisPrefix, type, language);
							// support format yaml, json, & xml
							// validasi format sebelum disimpan ke redis dalam format json
							JsonNode node = FrameworkHelper.loadConfiguration(bytes, JsonNode.class);
							bytes = dataMapper.writeAsBytes(node, DataMapper.JSON);
							map.put(ckey, bytes);
							return null;
						}
					);
				}
			);
		}
		return map;
	}
	
	private void loadResources() {
		Map<String, byte[]> cvalues = new HashMap<>();
		cvalues.putAll(getResources("mobile"));
		cvalues.putAll(getResources("portal"));
		Set<byte[]> bkeys = new LinkedHashSet<>();
		for (String key : cvalues.keySet()) {
			bkeys.add(key.getBytes());
		}
		ObjectHelper.callIf(
			!bkeys.isEmpty(), 
			() -> {
				redisCommand.listRightPush(Keys.resources(redisPrefix), bkeys);
				return redisCommand.valueMultiSet(cvalues);
			}
		);
	}
	
	private void clearResources() {
		String key = Keys.resources(redisPrefix);
		Long size = redisCommand.listSize(key);
		ObjectHelper.callIf(
			size != null && size > 0, 
			() -> {
				List<byte[]> bkeys = redisCommand.listLeftPop(key, ObjectHelper.useOrDefault(size, 0L));
				return ObjectHelper.callIf(
					bkeys != null && !bkeys.isEmpty(), 
					() -> {
						List<byte[]> tkeys = ObjectHelper.useOrDefault(bkeys, Collections::emptyList);
						StringSet skeys = new StringSet();
						while (!tkeys.isEmpty()) {
							skeys.add(new String(tkeys.remove(0)));
						}
						redisCommand.keyDelete(skeys);
						skeys.clear();
						return null;
					}
				);
			}
		);
	}

}
