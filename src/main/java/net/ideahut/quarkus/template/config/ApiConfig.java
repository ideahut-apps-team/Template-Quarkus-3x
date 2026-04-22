package net.ideahut.quarkus.template.config;

import io.quarkus.arc.DefaultBean;
import io.quarkus.redis.datasource.RedisDataSource;
import jakarta.inject.Named;
import jakarta.inject.Singleton;
import net.ideahut.quarkus.api.ApiAccessInternalService;
import net.ideahut.quarkus.api.ApiHandler;
import net.ideahut.quarkus.api.ApiHandlerImpl;
import net.ideahut.quarkus.api.ApiService;
import net.ideahut.quarkus.api.ApiServiceImpl;
import net.ideahut.quarkus.api.ApiTokenService;
import net.ideahut.quarkus.api.ApiTokenServiceImpl;
import net.ideahut.quarkus.collector.RequestInfoCollector;
import net.ideahut.quarkus.definition.ApiDefinition;
import net.ideahut.quarkus.definition.StorageKeyDefinition;
import net.ideahut.quarkus.definition.TimeValueDefinition;
import net.ideahut.quarkus.entity.EntityTrxManager;
import net.ideahut.quarkus.mapper.DataMapper;
import net.ideahut.quarkus.module.ModuleApi;
import net.ideahut.quarkus.redis.RedisParam;
import net.ideahut.quarkus.rest.RestHandler;
import net.ideahut.quarkus.serializer.BinarySerializer;
import net.ideahut.quarkus.sysparam.SysParamHandler;
import net.ideahut.quarkus.task.TaskHandler;
import net.ideahut.quarkus.template.app.AppConstants;
import net.ideahut.quarkus.template.app.AppProperties;

class ApiConfig {
	
	/*
	 * API HANDLER (CRUD & REQUEST)
	 */
	@Singleton
	@DefaultBean
	ApiHandler apiHandler(
		AppProperties appProperties,
		EntityTrxManager entityTrxManager,
		BinarySerializer binarySerializer,
		@Named(AppConstants.Bean.Redis.ACCESS)
		RedisDataSource redisDataSource,
		@Named(AppConstants.Bean.Task.PRIMARY)
		TaskHandler taskHandler,
		RequestInfoCollector requestInfoCollector
	) {
		ApiDefinition.Handler handler = appProperties.api().orElseThrow().handler().orElseThrow();
		
		return new ApiHandlerImpl()
		
		// Serialize & deserialize byte array ke redis
		.setBinarySerializer(binarySerializer)
		
		// Jumlah thread pada saat reload bean (menyiapkan data crud & request mapping), default: 3 thread
		.setConfigureThreads(handler.configureThreads().orElse(null))
		
		// Flag apakah bisa di-consume oleh Api Service / Provider lain
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
		
		// RedisTemplate dan definisi penamaan key-nya
		.setRedisParam(
			new RedisParam(StorageKeyDefinition.convert(handler.redisParam().orElse(null)))
			.setDataSource(redisDataSource)
		)
		
		// Untuk mendapatkan daftar RequestInfo / endpoint yang tersedia
		.setRequestInfoCollector(requestInfoCollector)
		
		// TaskHandler, untuk asinkronus
		.setTaskHandler(taskHandler);
	}
	
	
	/*
	 * API SERVICE
	 */
	@Singleton
	@DefaultBean
	ApiService apiService(
		AppProperties appProperties,
		BinarySerializer binarySerializer,
		@Named(AppConstants.Bean.Redis.ACCESS)
		RedisDataSource redisDataSource,
		ApiHandler apiHandler,
		RestHandler restHandler,
		ApiTokenService apiTokenService,
		ApiAccessInternalService apiAccessInternalService
	) {
		ApiDefinition.Service service = appProperties.api().orElseThrow().service().orElseThrow();
		return new ApiServiceImpl()
				
		// Untuk mendapatkan ApiAccess, jika token diterbitkan oleh service ini sendiri
		.setApiAccessInternalService(apiAccessInternalService)
		
		// Untuk mendapatkan ApiAccess ke service yang menerbitkan token
		// Secara default sudah dihandle, fungsi ini diperlukan jika ada custom
		.setApiAccessRemoteService(null)
		
		// ApiHandler
		.setApiHandler(apiHandler)
		
		// ApiName, akan didaftarkan ke database di setiap service yang terhubung
		// Jika tidak diset akan digunakan ID dari application context (property 'spring.application.name')
		.setApiName(service.apiName().orElse(null))
		
		// Daftar ApiProcessor yang digunakan
		.setApiProcessors(ModuleApi.getDefaultProcessors())
		
		// Untuk custom RestClient, seperti menambahkan sertifikat, timeout, dll.
		// Secara default sudah ada, hanya diperlukan jika custom
		.setApiRestClientCreator(null)
		
		// Secara default sudah ada, hanya diperlukan untuk mendapatkan ApiSource custom
		// misalnya daftar ApiSource tersimpan di file
		.setApiSourceService(null)
		
		// Service untuk meng-handle token
		.setApiTokenService(apiTokenService)
		
		// Serialize & deserialize byte array ke redis
		.setBinarySerializer(binarySerializer)
		
		// Custom Header name
		.setHeaderName(null)
		
		// Custom redis expiry, untuk data access & consumer baik yang null maupun tidak
		.setRedisExpiry(ApiDefinition.RedisExpiry.convert(service.redisExpiry().orElse(null)))
		
		// RedisTemplate dan definisi penamaan key-nya
		.setRedisParam(
			new RedisParam(StorageKeyDefinition.convert(service.redisParam().orElse(null)))
			.setDataSource(redisDataSource)
		)
		
		// RestHandler
		.setRestHandler(restHandler);
	}
	
	
	/*
	 * API TOKEN SERVICE
	 */
	@Singleton
	@DefaultBean
	ApiTokenService apiTokenService(
		AppProperties appProperties,
		DataMapper dataMapper,
		SysParamHandler sysParamHandler
	) {
		ApiDefinition.Token token = appProperties.api().orElseThrow().token().orElseThrow();
		
		return new ApiTokenServiceImpl()
		
		// Secara default sudah ada, diperlukan untuk mendapatkan ApiToken custom		
		.setApiTokenRetriever(null)
		
		// Default Consumer (komunikasi antar service) secret, digest , & expiry, jika tidak diset di database
		.setConsumerJwtParam(ApiDefinition.JwtParam.convert(token.consumerJwtParam().orElse(null)))
		
		// index redisTemplate untuk meyimpan consumer token jika menggunakan MultipleRedisTemplate, default index 1
		.setConsumerTokenStorageIndex(token.consumerTokenStorageIndex().orElse(null))
		
		// DataMapper
		.setDataMapper(dataMapper)
		
		// Default JwtProcessor secret, digest , & expiry, jika tidak diset di database
		.setProcessorJwtParam(ApiDefinition.JwtParam.convert(token.processorJwtParam().orElse(null)))
		
		// Batas atas & bawah timestamp signature yang dikirim
		// Contoh: jika diset 1 menit, berarti signature yang dikirim valid jika timestamp client dalam range -+ 1 menit
		.setSignatureTimeSpan(TimeValueDefinition.convert(token.signatureTimeSpan().orElse(null)))
		
		// Untuk menyimpan ApiToken di SysParam, sysCode = API_TOKEN, paramCode = [ApiName]
		//.setSysParamHandler(sysParamHandler)
		
		// Pakai ApiToken di dalam ApiProcessor untuk request ke ApiSource lain, 
		// jika false atau ApiToken tidak ada, maka akan digunakan Signature (berdasarkan secret dan digest) 
		.setUseApiTokenInProcessor(token.useApiTokenInProcessor().orElse(null));
	}
	
	
	/*
	 * API ACCESS INTERNAL SERVICE
	 */
	@Singleton
	@DefaultBean
	ApiAccessInternalService apiAccessInternalService() {
		return apiParameter -> null;
	}
	/*
	ApiAccessInternalService apiAccessInternalService(
		AuthService authService
	) {
		return authService::getApiAccessForInternal;
	}
	*/
	
	
}

