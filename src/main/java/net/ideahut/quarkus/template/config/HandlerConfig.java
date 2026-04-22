package net.ideahut.quarkus.template.config;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.TimeUnit;

import org.quartz.impl.StdSchedulerFactory;

import io.quarkus.redis.datasource.RedisDataSource;
import io.quarkus.runtime.Startup;
import jakarta.inject.Named;
import jakarta.inject.Singleton;
import net.ideahut.quarkus.audit.AuditHandler;
import net.ideahut.quarkus.audit.DatabaseAuditProperties;
import net.ideahut.quarkus.audit.DatabaseMultiAuditHandler;
import net.ideahut.quarkus.audit.DatabaseSingleAuditHandler;
import net.ideahut.quarkus.cache.CacheGroupHandler;
import net.ideahut.quarkus.cache.CacheGroupProperties;
import net.ideahut.quarkus.cache.CacheHandler;
import net.ideahut.quarkus.cache.MemoryCacheGroupHandler;
import net.ideahut.quarkus.cache.MemoryCacheHandler;
import net.ideahut.quarkus.cache.RedisCacheGroupHandler;
import net.ideahut.quarkus.cache.RedisCacheHandler;
import net.ideahut.quarkus.definition.CacheGroupDefinition;
import net.ideahut.quarkus.definition.DatabaseAuditDefinition;
import net.ideahut.quarkus.definition.GridDefinition;
import net.ideahut.quarkus.definition.KafkaDefinition;
import net.ideahut.quarkus.definition.MailDefinition;
import net.ideahut.quarkus.definition.RestDefinition;
import net.ideahut.quarkus.definition.StorageKeyDefinition;
import net.ideahut.quarkus.entity.EntityTrxManager;
import net.ideahut.quarkus.grid.GridHandler;
import net.ideahut.quarkus.grid.GridHandlerImpl;
import net.ideahut.quarkus.helper.FrameworkHelper;
import net.ideahut.quarkus.helper.ObjectHelper;
import net.ideahut.quarkus.init.InitHandler;
import net.ideahut.quarkus.init.InitHandlerImpl;
import net.ideahut.quarkus.job.SchedulerHandler;
import net.ideahut.quarkus.job.SchedulerHandlerImpl;
import net.ideahut.quarkus.kafka.KafkaHandler;
import net.ideahut.quarkus.kafka.KafkaHandlerImpl;
import net.ideahut.quarkus.mail.MailHandler;
import net.ideahut.quarkus.mail.MailHandlerImpl;
import net.ideahut.quarkus.mapper.DataMapper;
import net.ideahut.quarkus.object.TimeValue;
import net.ideahut.quarkus.redis.RedisParam;
import net.ideahut.quarkus.rest.OkHttpRestHandler;
import net.ideahut.quarkus.rest.RestHandler;
import net.ideahut.quarkus.serializer.BinarySerializer;
import net.ideahut.quarkus.sysparam.SysParamHandler;
import net.ideahut.quarkus.sysparam.SysParamHandlerImpl;
import net.ideahut.quarkus.task.TaskHandler;
import net.ideahut.quarkus.template.Application;
import net.ideahut.quarkus.template.app.AppConstants;
import net.ideahut.quarkus.template.app.AppProperties;
import net.ideahut.quarkus.template.support.GridSupport;

/*
 * Konfigurasi handler:
 * - AuditHandler
 * - CacheGroupHandler
 * - CacheHandler
 * - GridHandler
 * - InitHandler
 * - MailHandler
 * - RestHandler
 * - SysParamHandler
 * - SchedulerHandler
 * - KafkaHandler
 */
class HandlerConfig {
	
	/*
	 * INIT
	 */
	@Singleton
	InitHandler initHandler() {
		return new InitHandlerImpl()
		
		// Endpoint untuk inisialisasi DispatcherServlet
		.setEndpoint(() -> "http://localhost:" + FrameworkHelper.getPort() + "/init")
		
		// Custom jika mekanisme berbeda
		.setCall(null);
	}

	
	/*
	 * AUDIT
	 */
	@Startup
	@Singleton
	AuditHandler auditHandler(
		AppProperties appProperties,
		EntityTrxManager entityTrxManager,
		@Named(AppConstants.Bean.Task.AUDIT)
		TaskHandler taskHandler
	) {
		DatabaseAuditDefinition audit = appProperties.audit().orElseThrow();
		DatabaseAuditProperties properties = DatabaseAuditDefinition.Properties.toDatabaseAuditProperties(audit.properties().orElse(null));
		DatabaseAuditProperties.Table table = ObjectHelper.useOrDefault(properties.getTable(), DatabaseAuditProperties.Table::new);
		String prefix = ObjectHelper.useOrDefault(table.getPrefix(), "audit_");
		table.setPrefix(prefix);
		properties.setTable(table);
		if (Boolean.FALSE.equals(audit.useMultiTable().orElse(null))) {
			return new DatabaseSingleAuditHandler()
			.setEntityTrxManager(entityTrxManager)
			.setProperties(properties)
			.setRejectNonAuditEntity(!Boolean.FALSE.equals(audit.rejectNonAuditEntity().orElse(null)))
			.setTaskHandler(taskHandler);
		} else {
			return new DatabaseMultiAuditHandler()
			.setEntityTrxManager(entityTrxManager)
			.setProperties(properties)
			.setRejectNonAuditEntity(!Boolean.FALSE.equals(audit.rejectNonAuditEntity().orElse(null)))
			.setTaskHandler(taskHandler);
		}
	}
	
	/*
	 * CACHE GROUP
	 */
	@Singleton
	@Named("cacheGroupHandler")
	CacheGroupHandler cacheGroupHandler(
		AppProperties appProperties,
		BinarySerializer binarySerializer,
		@Named(AppConstants.Bean.Redis.PRIMARY)
		RedisDataSource redisDataSource,
		@Named(AppConstants.Bean.Task.PRIMARY) 
		TaskHandler taskHandler
	) {
		CacheGroupDefinition cache = appProperties.cache().orElseThrow();
		List<CacheGroupProperties> groups = new ArrayList<>();
		cache.groups().orElse(Collections.emptyList())
		.forEach(group -> groups.add(CacheGroupDefinition.Group.convert(group)));
		if (Boolean.TRUE.equals(cache.useLocalMemory().orElse(null))) {
			return new MemoryCacheGroupHandler()
			.setBinarySerializer(binarySerializer)
			.setGroups(groups)
			.setTaskHandler(taskHandler);
		} else {
			return new RedisCacheGroupHandler()
			.setBinarySerializer(binarySerializer)
			.setGroups(groups)
			.setRedisParam(
				new RedisParam(StorageKeyDefinition.convert(cache.redisParam().orElseThrow()))
				.setDataSource(redisDataSource)
			)
			.setTaskHandler(taskHandler);
		}
	}
	
	
	/*
	 * CACHE
	 */
	@Singleton
	CacheHandler cacheHandler(
		AppProperties appProperties,
		BinarySerializer binarySerializer,
		@Named(AppConstants.Bean.Redis.PRIMARY) 
		RedisDataSource redisDataSource,
		@Named(AppConstants.Bean.Task.PRIMARY) 
		TaskHandler taskHandler
	) {
		CacheGroupDefinition cache = appProperties.cache().orElseThrow();
		if (Boolean.TRUE.equals(cache.useLocalMemory().orElse(null))) {
			return new MemoryCacheHandler()
			.setBinarySerializer(binarySerializer)
			.setExpiryCheckInterval(TimeValue.of(TimeUnit.SECONDS, 30L))
			.setLimit(100)
			.setNullable(true)
			.setTaskHandler(taskHandler);
		} else {
			return new RedisCacheHandler()
			.setBinarySerializer(binarySerializer)
			.setLimit(100)
			.setNullable(true)
			.setRedisParam(
				new RedisParam()
				.setPrefix("_test")
				.setDataSource(redisDataSource)
			)
			.setTaskHandler(taskHandler);
		}
	}
	
	
	/*
	 * MAIL
	 */
	@Singleton
	MailHandler mailHandler(
		AppProperties appProperties,
		@Named(AppConstants.Bean.Task.PRIMARY)
		TaskHandler taskHandler
    ) {
		return new MailHandlerImpl()
		.setTaskHandler(taskHandler)
		.setMailProperties(MailDefinition.convert(appProperties.mail().orElseThrow()));
	}
	
	
	/*
	 * GRID
	 */
	@Singleton
	GridHandler gridHandler(
		AppProperties appProperties,
		BinarySerializer binarySerializer,
		@Named(AppConstants.Bean.Redis.PRIMARY)
		RedisDataSource redisDataSource
	) {
		GridDefinition grid = appProperties.grid().orElseThrow();
		return new GridHandlerImpl()
				
		// Daftar array yang digunakan di template grid, contoh: DAYS, MONTHS, dll		
		.setAdditionals(GridSupport.getAdditionals())
		
		// Serialize & deserialize byte array ke redis
		.setBinarySerializer(binarySerializer)
		
		// File definisi order, title, dll yang akan ditampilakan di UI
		.setDefinition(grid.definition().orElse(null))
		
		// Directory lokasi file-file template
		.setLocation(grid.location().orElse(null))
		
		// Untuk menerjemahkan judul, label, deskripsi, dll yang ada di template grid
		.setMessageHandler(null)
		
		// Daftar option select ynag digunakan di template grid, contoh: GENDER, BOOLEAN, dll
		.setOptions(GridSupport.getOptions())
		
		// RedisDataSource (jika null akan digunakan local memory)
		.setRedisDataSource(!Boolean.TRUE.equals(grid.useLocalMemory().orElse(null)) ? redisDataSource : null)
		
		// Mekanisme penyimpanan key di storage (redis / local memory)
		.setStorageKeyParam(StorageKeyDefinition.convert(grid.storageKeyParam().orElse(null)));
	}
	
	/*
	 * SYSPARAM
	 */
	@Singleton
	SysParamHandler sysParamHandler(
		BinarySerializer binarySerializer,
		EntityTrxManager entityTrxManager,
		@Named(AppConstants.Bean.Redis.PRIMARY)
		RedisDataSource redisDataSource
	) {
		return new SysParamHandlerImpl()
				
		// Serialize & deserialize byte array ke redis		
		.setBinarySerializer(binarySerializer)
		
		// Daftar Entity class dan nama trxManager yang terkait dengan SysParamHandler
		// default semua class di package 'net.ideahut.quarkus.sysparam.entity'
		.setEntityClass(null)
		
		// EntityTrxManager
		.setEntityTrxManager(entityTrxManager)
		
		// RedisDataSource dan definisi penyimpanan key-nya
		.setRedisParam(
			new RedisParam()	
			.setAppIdEnabled(true)
			.setEncryptEnabled(true)
			.setPrefix("SYS-PARAM")
			.setDataSource(redisDataSource)
		);
	}
	
	/*
	 * SCHEDULER
	 */
	@Startup
	@Singleton
	SchedulerHandler schedulerHandler(
		DataMapper dataMapper,
		EntityTrxManager entityTrxManager,
		@Named(AppConstants.Bean.Task.PRIMARY) 
		TaskHandler taskHandler
	) throws Exception {
		String vFalse = "false";
		// manual properties, agar SchedulerFactory tidak membuat RMI registry
		// RMI registry di native image akan menyebabkan error
		Properties properties = new Properties();
		properties.setProperty(StdSchedulerFactory.PROP_SCHED_RMI_EXPORT, vFalse);
		properties.setProperty(StdSchedulerFactory.PROP_SCHED_RMI_CREATE_REGISTRY, vFalse);
		properties.setProperty(StdSchedulerFactory.PROP_SCHED_RMI_PROXY, vFalse);
		properties.setProperty("org.quartz.threadPool.threadCount", "10");
		properties.setProperty("org.quartz.threadPool.threadPriority", "5");
		properties.setProperty("org.quartz.threadPool.threadsInheritContextClassLoaderOfInitializingThread", "true");
		StdSchedulerFactory schedulerFactory = new StdSchedulerFactory(properties);
		
		return new SchedulerHandlerImpl()
		
		// DataMapper		
		.setDataMapper(dataMapper)
		
		// Daftar Entity class dan nama trxManager yang terkait dengan SchedulerHandler
		// default semua class di package 'net.ideahut.quarkus.job.entity'
		//.setEntityClass(null)
		
		// EntityTrxManager
		.setEntityTrxManager(entityTrxManager)
		
		// Untuk membagi job yang dieksekusi berdasarkan instance (lihat Entity JobTrigger)
		// Jika tidak diset akan digunakan ID dari application context atau dari property 'spring.application.name'
		//.setInstanceId(null)
		
		// Daftar package class-class job, sehingga di database bisa disimpan menggunakan SimpleClassName
		.setJobPackages(Application.Package.APPLICATION + ".job")
		
		// Service untuk menghandle fungsi-fungsi job, seperti mengambil trigger, menyimpan hasil, dll
		// Secara default sudah ada, hanya diperlukan jika custom
		//.setJobService(null)
		
		// SchedulerFactory
		// Secara default sudah ada, hanya diperlukan jika custom
		.setSchedulerFactory(schedulerFactory)
		
		// TaskHandler
		.setTaskHandler(taskHandler)
		
		// Terkait dengan logger, nama key untuk id log yang digenerate random, default: 'traceId'
		//.setTraceKey(null)
		;
	}
	
	/*
	 * REST
	 */
	@Singleton
	RestHandler restHandler(
		AppProperties appProperties,
		DataMapper dataMapper,
		@Named(AppConstants.Bean.Task.REST)
		TaskHandler taskHandler
	) {
		RestDefinition rest = appProperties.rest().orElseThrow();
		
		return new OkHttpRestHandler()
		
		// Destroy Http client setelah mendapatkan respon
		.setClientDestroyable(rest.clientDestroyable().orElse(null))
		
		// DataMapper
		.setDataMapper(dataMapper)
		
		// DefaultRestClient, untuk mensetting beberapa variable seperti proxy
		.setDefaultRestClient(RestDefinition.Client.convert(rest.defaultRestClient().orElse(null)))
		
		// Menambahkan waktu eksekusi di RestResponse (mulai dari request sampai mendapatkan response / error)
		.setEnableExecutionTime(rest.enableExecutionTime().orElse(null))
		
		// Membatasi jumlah request secara bersamaan, ini terkait dengan jumlah port lokal yang digunakan untuk request ke server
		// Jika true maka TaskHandler tidak boleh null, jumlah pool di TaskHandler menjadi batas jumlah request bersamaan
		.setEnableRequestLimit(rest.enableRequestLimit().orElse(null))
		
		// TaskHandler, jika enableRequestLimit=true
		.setTaskHandler(taskHandler);
	}
	
	
	/*
	 * KAFKA
	 */
	@Singleton
	KafkaHandler kafkaHandler(
		AppProperties appProperties,
		BinarySerializer binarySerializer,
		DataMapper dataMapper
	) {
		KafkaDefinition kafka = appProperties.kafka().orElseThrow();
		if (Boolean.FALSE.equals(kafka.kafkaEnabled().orElse(null))) {
			return KafkaHandler.empty();
		} else {
			return new KafkaHandlerImpl()
			.setBinarySerializer(binarySerializer)
			.setBroadcastEnabled(kafka.broadcastEnabled().orElse(null))
			.setBroadcastTopic(kafka.broadcastTopic().orElse(null))
			.setConfigurationFile(kafka.configurationFile().orElse(null))
			.setDataMapper(dataMapper)
			.setDeleteUnusedGroupAndTopic(kafka.deleteUnusedGroupAndTopic().orElse(null))
			.setProperties(KafkaDefinition.Properties.convert(kafka.properties().orElse(null)))
			.setReloadEnabled(kafka.reloadEnabled().orElse(null));
		}
	}
	
}
