package net.ideahut.quarkus.template.service;

import net.ideahut.quarkus.api.ApiAccess;
import net.ideahut.quarkus.api.ApiAuth;
import net.ideahut.quarkus.api.ApiParameter;
import net.ideahut.quarkus.api.ApiRequest;

public interface AuthService {
	
	ApiAuth login(ApiRequest apiRequest, String username, String password) throws Exception;
	ApiAccess logout(ApiRequest apiRequest);
	ApiAccess info(ApiRequest apiRequest);
	
	ApiAccess getApiAccessForExternal(ApiRequest apiRequest);
	ApiAccess getApiAccessForInternal(ApiParameter apiParameter);
	
	String createConsumerToken(ApiRequest apiRequest);
	
}
