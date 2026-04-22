package net.ideahut.quarkus.template.config;

import io.quarkus.arc.DefaultBean;
import jakarta.inject.Singleton;
import net.ideahut.quarkus.collector.RequestInfoCollector;
import net.ideahut.quarkus.collector.RequestInfoCollectorImpl;
import net.ideahut.quarkus.definition.ForeignKeyDefinition;
import net.ideahut.quarkus.entity.EntityApiExcludeParam;
import net.ideahut.quarkus.entity.EntityAuditParam;
import net.ideahut.quarkus.entity.EntityTrxManager;
import net.ideahut.quarkus.entity.EntityTrxManagerImpl;
import net.ideahut.quarkus.helper.FrameworkHelper;
import net.ideahut.quarkus.helper.ObjectHelper;
import net.ideahut.quarkus.mapper.DataMapper;
import net.ideahut.quarkus.mapper.DataMapperImpl;
import net.ideahut.quarkus.message.entity.Language;
import net.ideahut.quarkus.module.ModuleApi;
import net.ideahut.quarkus.module.ModuleJob;
import net.ideahut.quarkus.object.ClassScannerInput;
import net.ideahut.quarkus.object.Message;
import net.ideahut.quarkus.serializer.BinarySerializer;
import net.ideahut.quarkus.serializer.DataMapperBinarySerializer;
import net.ideahut.quarkus.serializer.HessianBinarySerializer;
import net.ideahut.quarkus.serializer.JdkBinarySerializer;
import net.ideahut.quarkus.sysparam.entity.SysParam;
import net.ideahut.quarkus.template.app.AppProperties;

class CommonConfig {
	
	/*
	 * DATA MAPPER
	 */
	@Singleton
	@DefaultBean
	DataMapper dataMapper() {
		DataMapper dataMapper = new DataMapperImpl();
		FrameworkHelper.setDefaultDataMapper(dataMapper);
		return dataMapper;
	}
	
	
	/*
	 * BINARY SERIALIZER
	 */
	@Singleton
	@DefaultBean
	BinarySerializer binarySerializer(
		AppProperties appProperties,
		DataMapper dataMapper
	) {
		BinarySerializer binarySerializer;
		String code = appProperties.binarySerializer().orElse("").trim().toLowerCase();
		if ("xml".equals(code)) {
			binarySerializer = new DataMapperBinarySerializer().setMapper(dataMapper).setFormat(DataMapper.XML);
		}
		else if ("jdk".equals(code)) {
			binarySerializer = new JdkBinarySerializer();
		}
		else if ("hessian1".equals(code)) {
			binarySerializer = new HessianBinarySerializer().setVersion(1);
		}
		else if ("hessian2".equals(code)) {
			binarySerializer = new HessianBinarySerializer().setVersion(2);
		}
		/**
		else if ("fory".equals(code)) {
			binarySerializer = new ForyBinarySerializer().setFory(ForyInstance.getInstance());
		}
		else if ("kryo".equals(code)) {
			binarySerializer = new KryoBinarySerializer().setReferences(true);
		}
		*/
		else {
			binarySerializer = new DataMapperBinarySerializer().setMapper(dataMapper).setFormat(DataMapper.JSON);
		}
		FrameworkHelper.setDefaultBinarySerializer(binarySerializer);
		return binarySerializer;
	}
	
	
	/*
	 * ENTITY TRX MANAGER
	 */
	@Singleton
	@DefaultBean
	EntityTrxManager entityTrxManager(
		AppProperties appProperties
	) {
		return new EntityTrxManagerImpl()
		
		// Entity / Model yang tidak memiliki anotasi @ApiExclude, dan tidak ingin dipublikasikan oleh ApiService
		.setApiExcludeParams(
			new EntityApiExcludeParam()
			.addEntityClasses(ModuleApi.getApiExcludeEntities())
			.addEntityClasses(ModuleJob.getApiExcludeEntities())
			.addEntityClasses(
				SysParam.class,
				Language.class,
				Message.class
			)
		)
		
		// Entity / Model yang tidak memiliki anotasi @Audit, dan ingin setiap perubahannya disimpan
		.setAuditParams(
			new EntityAuditParam()
			.addEntityClasses(ModuleApi.getAuditEntities())
			.addEntityClasses(ModuleJob.getAuditEntities())
			.addEntityClasses(
				SysParam.class,
				Language.class,
				Message.class
			)
		)
		
		// Daftar EntityPreListener & EntityPostListener, default autoDetect = true
		.setEntityListenerParam(null)
		
		// Parameter untuk menghandle anotasi @ForeignKeyEntity
		// Ini solusi jika terjadi error saat membuat native image dimana entity memiliki @ManyToOne & @OneToMany
		// tapi package-nya berbeda dengan package project (error ByteCodeProvider saat runtime)
		.setForeignKeyParam(ForeignKeyDefinition.convert(appProperties.foreignKey().orElse(null)));
	}
	
	
	/*
	 * 
	 */
	@Singleton
	@DefaultBean
	RequestInfoCollector requestInfoCollector(
		AppProperties appProperties,
		DataMapper dataMapper
	) {
		RequestInfoCollectorImpl collector = new RequestInfoCollectorImpl()
		.setDataMapper(dataMapper);
		appProperties.request().ifPresent(definition -> {
			Integer loaderType = definition.loader().orElse(null);
			ClassLoader classLoader = ObjectHelper.callIf(loaderType != null, () -> ObjectHelper.getClassLoader(loaderType));
			ClassScannerInput classScannerInput = new ClassScannerInput();
			classScannerInput.setClassLoader(classLoader);
			definition.packages().ifPresent(classScannerInput::setPackages);
			definition.classes().ifPresent(classScannerInput::setClasses);
			definition.reflection().ifPresent(classScannerInput::setReflection);
			definition.file().ifPresent(collector::setFile);
		});
		return collector;
	}
	
}
