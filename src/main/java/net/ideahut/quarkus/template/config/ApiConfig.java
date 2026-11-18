package net.ideahut.quarkus.template.config;

import io.quarkus.arc.DefaultBean;
import io.quarkus.redis.datasource.RedisDataSource;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Named;
import net.ideahut.quarkus.api.ApiAccessInternal;
import net.ideahut.quarkus.api.ApiHandler;
import net.ideahut.quarkus.api.ApiHandlerImpl;
import net.ideahut.quarkus.api.ApiService;
import net.ideahut.quarkus.api.ApiServiceImpl;
import net.ideahut.quarkus.api.ApiSupport;
import net.ideahut.quarkus.api.ApiSupportImpl;
import net.ideahut.quarkus.api.ApiTokenSysParam;
import net.ideahut.quarkus.api.ApiTokenSysParamImpl;
import net.ideahut.quarkus.definition.ApiDefinition;
import net.ideahut.quarkus.definition.RestDefinition;
import net.ideahut.quarkus.definition.StorageKeyDefinition;
import net.ideahut.quarkus.entity.EntityTrxManager;
import net.ideahut.quarkus.helper.ObjectHelper;
import net.ideahut.quarkus.module.ModuleApi;
import net.ideahut.quarkus.redis.RedisParam;
import net.ideahut.quarkus.rest.RestHandler;
import net.ideahut.quarkus.serializer.BinarySerializer;
import net.ideahut.quarkus.sysparam.SysParamHandler;
import net.ideahut.quarkus.task.TaskHandler;
import net.ideahut.quarkus.template.app.AppConstant;
import net.ideahut.quarkus.template.app.AppProperties;
import net.ideahut.quarkus.template.service.AuthService;

class ApiConfig {
	
	/*
	 * API HANDLER (CRUD & REQUEST)
	 */
	@ApplicationScoped
	@DefaultBean
	ApiHandler apiHandler(
		AppProperties appProperties,
		EntityTrxManager entityTrxManager,
		BinarySerializer binarySerializer,
		@Named(AppConstant.Bean.Redis.PRIMARY)
		RedisDataSource redisDataSource,
		@Named(AppConstant.Bean.Task.PRIMARY)
		TaskHandler taskHandler,
		ApiTokenSysParam apiTokenSysParam,
		RestHandler restHandler
	) {
		ApiDefinition.Handler handler = appProperties.api().orElseThrow().handler().orElseThrow();
		
		ApiHandlerImpl bean = new ApiHandlerImpl()
		
		// Serialize & deserialize byte array ke redis
		.setBinarySerializer(binarySerializer)
		
		// Jumlah thread pada saat reload bean (menyiapkan data crud & request mapping), default: 3 thread
		.setConfigureThreads(handler.configureThreads().orElse(null))
		
		// Default rest timeout
		.setDefaultRestTimeout(RestDefinition.Timeout.convert(handler.defaultRestTimeout().orElse(null)))
		
		// Flag apakah bisa menerima request dari Api Source / Provider lain
		.setEnableConsumer(handler.enableConsumer().orElse(null))
		
		// Flag apakah Api Crud aktif atau tidak
		.setEnableCrud(handler.enableCrud().orElse(null))
		
		// Flag apakah data Crud & Request Mapping di database dihapus jika tidak tersedia di aplikasi
		.setEnableSync(handler.enableSync().orElse(null))
		
		// Daftar Entity class dan nama trxManager yang terkait dengan ApiHandler
		// default semua class di package net.ideahut.quarkus.api.entity
		.setEntityClass(null)
		
		// EntityTrxManager
		.setEntityTrxManager(entityTrxManager)
		
		// Custom function yang akan dieksekusi jika enableSync = true
		// secara default sudah dihandle
		.setOnSyncEntities(null)
		
		// RedisTemplate dan definisi penamaan key-nya
		.setRedisParam(
			new RedisParam(StorageKeyDefinition.convert(handler.cacheKeyParam().orElse(null)))
			.setDataSource(redisDataSource)
		)
		
		// Untuk mendapatkan daftar RequestInfo / endpoint yang tersedia
		.setRequestInfoCollector(null)
		
		// RestHandler yang akan mengirim rest request
		.setRestHandler(restHandler)
		
		// TaskHandler, untuk asinkronus
		.setTaskHandler(taskHandler);
		
		// Simpan ApiToken ke SysParam, jika defaultApiTokenStorage tidak diset
		// systemCode = API_TOKEN dan ParamCode harus diisi terlebih dahulu dengan ApiName yang tokennya akan disimpan
		Class<?> apiTokenSysParamClass = handler.apiTokenSysParamClass().orElse(null);
		ObjectHelper.callOrElse(
			apiTokenSysParamClass != null, 
			() -> bean.setApiTokenSysParamClass(apiTokenSysParamClass), 
			() -> bean.setApiTokenSysParamObject(apiTokenSysParam)
		);
		
		// Untuk menyimpan ApiToken, jika null akan disimpan ke SysParam
		bean
		.setDefaultApiTokenStorageClass(handler.defaultApiTokenStorageClass().orElse(null))
		.setDefaultApiTokenStorageObject(null);
		
		// Untuk custom RestClient, seperti menambahkan sertifikat, timeout, pakai proxy, dll.
		// Secara default sudah ada, hanya diperlukan jika custom
		bean
		.setDefaultRestClientCreatorClass(handler.defaultRestClientCreatorClass().orElse(null))
		.setDefaultRestClientCreatorObject(null);
		
		return bean;
	}
	
	/*
	 * API SERVICE
	 */
	@ApplicationScoped
	@DefaultBean
	ApiService apiService(
		AppProperties appProperties,
		BinarySerializer binarySerializer,
		ApiHandler apiHandler,
		@Named(AppConstant.Bean.Redis.ACCESS)
		RedisDataSource redisDataSource,
		ApiSupport apiSupport,
		ApiAccessInternal apiAccessInternal
	) {
		ApiDefinition.Service service = appProperties.api().orElseThrow().service().orElseThrow();
		
		return new ApiServiceImpl()
				
		// Untuk mendapatkan ApiAccess, jika token diterbitkan oleh service ini sendiri
		.setApiAccessInternal(apiAccessInternal)
		
		// Untuk mendapatkan ApiAccess ke service yang menerbitkan token
		// Secara default sudah dihandle, fungsi ini diperlukan jika ada custom
		.setApiAccessRemote(null)
		
		// ApiHandler
		.setApiHandler(apiHandler)
		
		// ApiName, akan didaftarkan ke database di setiap service yang terhubung
		// Jika tidak diset akan digunakan ID dari application context (property 'quarkus.application.name')
		.setApiName(service.apiName().orElse(null))
		
		// Daftar ApiRoleGate yang digunakan
		.setApiRoleGates(ModuleApi.getDefaultApiRoleGates())
		
		// Secara default sudah ada, hanya diperlukan untuk mendapatkan ApiSource custom
		// misalnya daftar ApiSource tersimpan di file
		.setApiSourceLoader(null)
		
		// Untuk menangani token, jwt, signature, dan validasi
		.setApiSupport(apiSupport)
		
		// Serialize & deserialize byte array ke redis
		.setBinarySerializer(binarySerializer)
		
		// Cache expiry, untuk data access & consumer baik yang null maupun tidak
		.setCacheExpiry(ApiDefinition.CacheExpiry.convert(service.cacheExpiry().orElse(null)))
		
		// Flag untuk debug saat mendapatkan ApiAccess dan call endpoint
		.setDebug(service.debug().orElse(null))
		
		// Custom Header name
		.setHeaderName(null)
		
		// RedisDataSource dan definisi penamaan key-nya
		// Untuk menyimpan ApiAccess & ApiToken
		.setRedisParam(
			new RedisParam(StorageKeyDefinition.convert(service.cacheKeyParam().orElse(null)))
			.setDataSource(redisDataSource)
		)
		
		// Flag apakah error di ApiAccess dikembalikan null atau di-throw error
		.setThrowApiAccessError(service.throwApiAccessError().orElse(null));
	}
	
	/*
	 * API SUPPORT
	 */
	@ApplicationScoped
	ApiSupport apiSupport(
		AppProperties appProperties
	) {
		ApiDefinition.Support support = appProperties.api().orElseThrow().support().orElseThrow();
		
		return new ApiSupportImpl()
		
		// Untuk membuat ApiToken dari sisi server di ApiProviderGate.
		.setApiConsumerJwtParam(ApiDefinition.JwtParam.convert(support.apiConsumerJwtParam().orElse(null)))
		
		// Untuk membuat token di ApiRoleGate
		.setApiRoleGateJwtParam(ApiDefinition.JwtParam.convert(support.apiRoleGateJwtParam().orElse(null)))
		
		// Batas atas & bawah timestamp yang dikirim oleh klien
		// Contoh: jika diset 1 menit, berarti request yang dikirim valid jika timestamp client dalam range -+ 1 menit
		.setApiSourceGateTimeSpan(ApiDefinition.TimeSpan.convert(support.apiSourceGateTimeSpan().orElse(null)))
		
		// index redisDataSource untuk meyimpan consumer token jika menggunakan MultipleRedisDataSource, default index 1
		.setApiSourceGateTokenStorageIndex(support.apiSourceGateTokenStorageIndex().orElse(null));
	}
	
	/*
	 * API TOKEN SYSPARAM
	 */
	@ApplicationScoped
	ApiTokenSysParam apiTokenSysParam(
		SysParamHandler sysParamHandler
	) {
		return new ApiTokenSysParamImpl()
		.setSysParamHandler(sysParamHandler);
	}
	
	/*
	 * API ACCESS INTERNAL
	 */
	@ApplicationScoped
	ApiAccessInternal apiAccessInternal(
		AuthService authService
	) {
		return authService::getApiAccessForInternal;
	}
	
}

