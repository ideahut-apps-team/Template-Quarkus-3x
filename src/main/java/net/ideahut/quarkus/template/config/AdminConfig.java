package net.ideahut.quarkus.template.config;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

import io.quarkus.arc.DefaultBean;
import io.quarkus.redis.datasource.RedisDataSource;
import jakarta.inject.Named;
import jakarta.inject.Singleton;
import net.ideahut.quarkus.admin.AdminHandler;
import net.ideahut.quarkus.admin.AdminHandlerImpl;
import net.ideahut.quarkus.admin.AdminSecurity;
import net.ideahut.quarkus.collector.RequestInfoCollector;
import net.ideahut.quarkus.definition.AdminDefinition;
import net.ideahut.quarkus.definition.StorageKeyDefinition;
import net.ideahut.quarkus.definition.TimeValueDefinition;
import net.ideahut.quarkus.helper.ObjectHelper;
import net.ideahut.quarkus.mapper.DataMapper;
import net.ideahut.quarkus.module.ModuleAdmin;
import net.ideahut.quarkus.object.StringMap;
import net.ideahut.quarkus.object.StringSet;
import net.ideahut.quarkus.redis.RedisParam;
import net.ideahut.quarkus.rest.RestHandler;
import net.ideahut.quarkus.security.LocalMemoryCredential;
import net.ideahut.quarkus.security.RedisMemoryCredential;
import net.ideahut.quarkus.security.SecurityCredential;
import net.ideahut.quarkus.security.SecurityUser;
import net.ideahut.quarkus.security.WebBasicAuthSecurity;
import net.ideahut.quarkus.security.WebSecurity;
import net.ideahut.quarkus.serializer.BinarySerializer;
import net.ideahut.quarkus.template.app.AppConstants;
import net.ideahut.quarkus.template.app.AppProperties;
import net.ideahut.quarkus.template.support.GridSupport;

/*
 * Konfigurasi Admin
 * Dapat diakses menggunakan browser
 * http://<host>:<port>/_/web
 */
class AdminConfig {
	
	/*
	 * ADMIN HANDLER
	 */
	@Singleton
	@DefaultBean
	AdminHandler adminHandler(
		AppProperties appProperties,
		BinarySerializer binarySerializer,
		@Named(AppConstants.Bean.Redis.PRIMARY)
		RedisDataSource redisDataSource,
		RestHandler restHandler,
		RequestInfoCollector requestInfoCollector
	) {
		AdminDefinition.Handler handler = appProperties.admin().orElseThrow().handler().orElseThrow();
		
		return new AdminHandlerImpl()
				
		// Memperbaharui data-data di javascript & html resource admin sesuai dengan konfigurasi, seperti: judul, timeout, dll
		.setAfterReload(h -> ModuleAdmin.afterReload(h, ""))
		
		// Path untuk mengakses API Admin
		.setApiPath(handler.apiPath().orElse(null))
		
		// Serialize & deserialize byte array ke redis
		.setBinarySerializer(binarySerializer)
		
		// Custom cek token ke central, secara default sudah tersedia
		//.setCheckTokenCentral(null)
		
		// Lokasi konfigurasi file untuk fitur admin
		.setConfigurationFile(handler.configurationFile().orElse(null))
		
		// Daftar array yang digunakan di template grid, contoh: DAYS, MONTHS, dll
		.setGridAdditionals(GridSupport.getAdditionals())
		
		// Daftar option select ynag digunakan di template grid, contoh: GENDER, BOOLEAN, dll
		.setGridOptions(GridSupport.getOptions())
		
		// Untuk menerjemahkan judul, deskripsi, dll yang ada di template grid
		.setMessageHandler(null)
		
		// Opsi AdminProperties jika configuration file tidak di-set
		.setProperties(null)
		
		// Untuk menyimpan data-data admin, seperti: template grid, authorization, dll
		.setRedisDataSource(redisDataSource)
		
		// Menggunakan nama bean, jika RequestMappingHandlerMapping di application context lebih dari satu
		//.setRequestMappingHandlerMappingBeanName(null)
		
		// Untuk sinkronisasi ke central
		.setRestHandler(restHandler)
		
		// Mekanisme penyimpanan key di storage (redis / local memory)
		.setStorageKeyParam(StorageKeyDefinition.convert(handler.storageKeyParam().orElse(null)))
		
		// Custom sinkronisasi ke central, secara default sudah tersedia 
		//.setSyncToCentral(null)
		
		// Umur cache resource Admin UI
		.setWebCacheMaxAge(TimeValueDefinition.convert(handler.webCacheMaxAge().orElse(null)))
		
		// Flag Admin UI bisa diakses atau tidak
		.setWebEnabled(handler.webEnabled().orElse(null))
		
		// Lokasi resource Admin UI
		.setWebLocation(handler.webLocation().orElse(null))
		
		// Context Path untuk mengakses Admin UI
		.setWebPath(handler.webPath().orElse(null))
		
		// Flag resource chain atau tidak
		.setWebResourceChain(handler.webResourceChain().orElse(null))
		
		// Untuk mendapatkan RequestInfo yang tersedia
		.setRequestInfoCollector(requestInfoCollector);
		
	}
	
	
	/*
	 * ADMIN CREDENTIAL
	 */
	@Singleton
	@Named(AppConstants.Bean.Credential.ADMIN)
	SecurityCredential adminCredential(
		AppProperties appProperties,
		BinarySerializer binarySerializer,
		@Named(AppConstants.Bean.Redis.PRIMARY)
		RedisDataSource redisDataSource
	) {
		AdminDefinition.Credential credential = appProperties.admin().orElseThrow().credential().orElseThrow();
		Set<SecurityUser> users = new LinkedHashSet<>();
		credential.users().orElse(Collections.emptyList())
		.forEach(user ->
			users.add(new SecurityUser()
				.setAttributes(ObjectHelper.callIf(null != user.attributes(), () -> new StringMap(user.attributes())))
				.setHosts(ObjectHelper.callIf(null != user.hosts().orElse(null), () -> new StringSet(user.hosts().orElse(null))))
				.setPassword(user.password().orElse(null))
				.setRole(user.role().orElse(null))
				.setUsername(user.username().orElse(null))
			)
		);
		
		if (Boolean.TRUE.equals(credential.useLocalMemory().orElse(null))) {
			
			// Local Memory
			return new LocalMemoryCredential()
					
			// Serialize & deserialize byte array di local memory
			.setBinarySerializer(binarySerializer)
					
			// Cek yang sudah kadaluarsa
			.setCheckInterval(TimeValueDefinition.convert(credential.checkInterval().orElse(null)))
			
			// Lokasi file kredensial
			.setCredentialFile(credential.credentialFile().orElse(null))
			
			// Optional, jika tidak didefinisikan di credential file
			.setExpiry(TimeValueDefinition.convert(credential.expiry().orElse(null)))
			
			// Optional, jika tidak didefinisikan di credential file
			.setPasswordType(credential.passwordType().orElse(null))
			
			// Optional, jika tidak didefinisikan di credential file
			.setUsers(users);
			
			
		} else {
			
			// Redis memory
			return new RedisMemoryCredential()
			
			// Serialize & deserialize byte array di redis	
			.setBinarySerializer(binarySerializer)
			
			// Lokasi file kredensial
			.setCredentialFile(credential.credentialFile().orElse(null))
			
			// Optional, jika tidak didefinisikan di credential file
			.setExpiry(TimeValueDefinition.convert(credential.expiry().orElse(null)))
			
			// Optional, jika tidak didefinisikan di credential file
			.setPasswordType(credential.passwordType().orElse(null))
			
			// RedisTemplate dan definisi penyimpanan key-nya
			.setRedisParam(
				new RedisParam(StorageKeyDefinition.convert(credential.storageKeyParam().orElseThrow()))
				.setDataSource(redisDataSource)
			)
			
			// Optional, jika tidak didefinisikan di credential file
			.setUsers(users);
		}
	}
	
	
	/*
	 * ADMIN SECURITY
	 */
	@Singleton
	@Named(AppConstants.Bean.Security.ADMIN)
	WebSecurity adminSecurity(
		AppProperties appProperties,
		DataMapper dataMapper,
		AdminHandler adminHandler,
		@Named(AppConstants.Bean.Credential.ADMIN)
		SecurityCredential credential
	) {
		AdminDefinition.Security security = appProperties.admin().orElseThrow().security().orElseThrow();
		if (Boolean.TRUE.equals(security.useBasicAuth().orElse(null))) {
			
			// Basic Auth
			return new WebBasicAuthSecurity()
					
			// Credential
			.setCredential(credential)
			
			// Realm, ditampilkan di-popup browser
			.setRealm(security.realm().orElse(null));
			
		} else {
			
			// Berdasarkan AdminHandler
			return new AdminSecurity()
					
			// Admin handler		
			.setAdminHandler(adminHandler)
			
			// Credential
			.setCredential(credential)
			
			// DataMapper
			.setDataMapper(dataMapper)
			
			// Pengecekan host yang diperoleh pada saat login
			.setEnableRemoteHost(security.enableRemoteHost().orElse(null))
			
			// Pengecekan User-Agent yang diperoleh pada saat login
			.setEnableUserAgent(security.enableUserAgent().orElse(null))
			
			// Http header untuk menyimpan token, default: 'Authorization'
			.setHeaderKey(security.headerKey().orElse(null));
		}
	}
	
}
