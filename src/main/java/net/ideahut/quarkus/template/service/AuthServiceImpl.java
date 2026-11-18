package net.ideahut.quarkus.template.service;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import io.quarkus.redis.datasource.RedisDataSource;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import net.ideahut.quarkus.api.ApiAccess;
import net.ideahut.quarkus.api.ApiAuth;
import net.ideahut.quarkus.api.ApiHeaderValue;
import net.ideahut.quarkus.api.ApiParameter;
import net.ideahut.quarkus.api.ApiRequest;
import net.ideahut.quarkus.api.ApiRoleGate;
import net.ideahut.quarkus.api.ApiService;
import net.ideahut.quarkus.api.ApiSource;
import net.ideahut.quarkus.api.ApiUser;
import net.ideahut.quarkus.api.gate.role.AgentHostJwtApiRoleGate;
import net.ideahut.quarkus.api.gate.role.AgentJwtApiRoleGate;
import net.ideahut.quarkus.api.gate.role.HostJwtApiRoleGate;
import net.ideahut.quarkus.api.gate.role.StandardJwtApiRoleGate;
import net.ideahut.quarkus.bean.BeanConfigure;
import net.ideahut.quarkus.helper.ErrorHelper;
import net.ideahut.quarkus.helper.FrameworkHelper;
import net.ideahut.quarkus.helper.StringHelper;
import net.ideahut.quarkus.helper.TimeHelper;
import net.ideahut.quarkus.object.MapString;
import net.ideahut.quarkus.object.TimeValue;
import net.ideahut.quarkus.redis.RedisCommand;
import net.ideahut.quarkus.serializer.BinarySerializer;
import net.ideahut.quarkus.template.app.AppConstant;
import net.ideahut.quarkus.template.object.UserData;

@ApplicationScoped
class AuthServiceImpl implements AuthService, BeanConfigure {
	
	private static final String AUTH_PREFIX = "AUTH-";
	private static final TimeValue AUTH_EXPIRY = TimeValue.of(TimeUnit.MINUTES, 60L); // 1 jam
	private static final TimeValue ACCESS_EXPIRY = TimeValue.of(TimeUnit.HOURS, 24L); // 1 hari
	private static final List<String> JWT_ROLE_GATES = Arrays.asList(
		StandardJwtApiRoleGate.API_TYPE,
		AgentJwtApiRoleGate.API_TYPE,
		HostJwtApiRoleGate.API_TYPE,
		AgentHostJwtApiRoleGate.API_TYPE
	);
	private static final String API_ROLE = "USER-QUARKUS";
	private static final Map<String, UserData> users;
	static {
		Map<String, UserData> usersEi = new HashMap<>();
		usersEi.put("admin", new UserData().setRoleCode("APP-ADMIN").setPassword("admin123").setUserId("1").setUsername("admin"));
		usersEi.put("user", new UserData().setRoleCode("APP-USER").setPassword("user123").setUserId("1").setUsername("user"));
		users = usersEi;
	}
	
	private final BinarySerializer binarySerializer;
	private final RedisCommand<String, byte[]> redisCommand;
	
	private ApiService apiService;
	private boolean configured = false;
	
	@Inject
	AuthServiceImpl(
		BinarySerializer binarySerializer,
		@Named(AppConstant.Bean.Redis.ACCESS)
		RedisDataSource redisDataSource
	) {
		this.binarySerializer = binarySerializer;
		this.redisCommand = RedisCommand.of(redisDataSource, String.class, byte[].class);
	}
	
	@Override
	public void onConfigureBean() throws Exception {
		this.apiService = FrameworkHelper.getBean(ApiService.class);
		configured = true;
	}

	@Override
	public boolean isBeanConfigured() {
		return configured;
	}

	@Override
	public ApiAuth login(
		ApiRequest apiRequest, 
		String username, 
		String password
	) throws Exception {
		UserData user = users.get(username);
		ErrorHelper.throwNull(user, () -> "User not found");
		ErrorHelper.throwIf(!user.getPassword().equals(password), () -> "Invalid password");
		
		String apiType = apiRequest.getHeader(apiService.getApiHeaderName().getType(), StandardJwtApiRoleGate.API_TYPE);
		ApiRoleGate apiRoleGate = apiService.getApiRoleGate(apiType);
		ErrorHelper.throwNull(apiRoleGate, () -> "ApiRoleGate not found, ApiType: " + apiType);
		boolean isJwtCheck = apiRequest.getHeader(boolean.class, "Jwt-Check", false);
		boolean isJwtType = JWT_ROLE_GATES.contains(apiType);
		
		Long createdOn = TimeHelper.currentEpochMillis();
		ApiAccess apiAccess = new ApiAccess()
		.setCreatedOn(createdOn)
		.setExpiredOn(createdOn + ACCESS_EXPIRY.toMillis())
		.setApiUser(new ApiUser()
			.setId(user.getUserId())
			.setUsername(user.getUsername())
			.setAttribute(ApiUser.Attribute.ROLE, user.getRoleCode())
		)
		// set appid
		.setAttribute(ApiAccess.Attribute.APP_ID, apiService.getApiName());
		if (isJwtType && isJwtCheck) {
			apiAccess.setAttribute("check", "true");
		}
		
		ApiParameter apiParameter = new ApiParameter()
		.setApiType(apiType)
		.setApiName(apiService.getApiName())
		.setApiRequest(apiRequest);
		
		ApiAuth apiAuth = apiRoleGate.createApiAuth(apiParameter, apiAccess);
		byte[] bytes = binarySerializer.serialize(ApiAuth.class, apiAuth);
		redisCommand.valueSet(AUTH_PREFIX + apiAuth.getApiKey(), bytes, AUTH_EXPIRY);
		return apiAuth;
	}

	@Override
	public ApiAccess logout(
		ApiRequest apiRequest
	) {
		ApiAccess apiAccess = apiService.getApiAccess(apiRequest);
		if (apiAccess != null) {
			ErrorHelper.throwIf(!isInternalApiAccess(apiAccess), () -> "Invalid ApiPublisher");
			redisCommand.keyDelete(AUTH_PREFIX + apiAccess.getApiKey());
			apiService.removeApiAccess(null, apiAccess.getApiKey());
		}
		return apiAccess;
	}

	@Override
	public ApiAccess info(
		ApiRequest apiRequest
	) {
		ApiParameter apiParameter = apiService.getApiParameter(apiRequest);
		String apiKey = apiParameter != null ? apiParameter.getApiKey() : null;
		if (!StringHelper.isBlank(apiKey)) {
			byte[] bytes = redisCommand.valueGet(AUTH_PREFIX + apiKey);
			if (bytes != null) {
				ApiAuth apiAuth = binarySerializer.deserialize(ApiAuth.class, bytes);
				ErrorHelper.throwIf(!isInternalApiAccess(apiAuth.getApiAccess()), () -> "Invalid ApiPublisher");
				return apiAuth.getApiAccess();
			}
		}
		return null;
	}

	/*
	 * ApiAccess untuk request dari ApiProvider lain
	 */
	@Override
	public ApiAccess getApiAccessForExternal(
		ApiRequest apiRequest
	) {
		ApiParameter apiParameter = apiService.getApiParameter(apiRequest);
		String apiKey = apiParameter.getApiKey() != null ? apiParameter.getApiKey() : "";
		ErrorHelper.throwBlank(apiKey, () -> "ApiKey required");
		ApiHeaderValue apiHeaderValue = apiRequest.getApiHeaderValue();
		String source = apiHeaderValue.getSource() != null ? apiHeaderValue.getSource() : "";
		ErrorHelper.throwBlank(source, () -> StringHelper.format( "Header '{}' required", apiHeaderValue.getApiHeaderName().getSource()));
		ApiSource apiSource = apiService.getApiSource(source);
		ErrorHelper.throwNull(apiSource, () -> "ApiSource not found");
		MapString gateConfig = apiService.getApiGateConfig(apiSource);
		apiService.getApiSupport().validateSignature(apiSource, apiRequest, gateConfig);
		byte[] bytes = redisCommand.valueGet(AUTH_PREFIX + apiKey);
		if (bytes != null) {
			ApiAccess apiAccess = binarySerializer.deserialize(ApiAuth.class, bytes).getApiAccess();
			return apiAccess.setApiRole(API_ROLE);
		}
		return null;
	}

	/*
	 * ApiAccess untuk request dari internal service
	 */
	@Override
	public ApiAccess getApiAccessForInternal(ApiParameter apiParameter) {
		String apiKey = apiParameter != null && apiParameter.getApiKey() != null ? apiParameter.getApiKey() : "";
		ErrorHelper.throwBlank(apiKey, () -> "ApiKey required");
		byte[] bytes = redisCommand.valueGet(AUTH_PREFIX + apiKey);
		if (bytes != null) {
			ApiAccess apiAccess = binarySerializer.deserialize(ApiAuth.class, bytes).getApiAccess();
			return apiAccess.setApiRole(API_ROLE);
		}
		return null;
	}
	
	@Override
	public String createConsumerToken(ApiRequest apiRequest) {
		ApiHeaderValue apiHeaderValue = apiRequest.getApiHeaderValue();
		String source = apiHeaderValue.getSource() != null ? apiHeaderValue.getSource() : "";
		ErrorHelper.throwBlank(source, () -> StringHelper.format( "Header '{}' required", apiHeaderValue.getApiHeaderName().getSource()));
		ApiSource apiSource = apiService.getApiSource(source);
		ErrorHelper.throwNull(apiSource, () -> "ApiSource not found");
		return apiService.getApiSupport().createConsumerApiToken(apiSource, apiRequest);
	}
	
	private boolean isInternalApiAccess(ApiAccess apiAccess) {
		if (apiAccess != null) {
			return apiService.getApiName().equals(apiAccess.getAttribute(ApiAccess.Attribute.APP_ID));
		}
		return false;
	}

}
