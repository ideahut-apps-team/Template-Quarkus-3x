package net.ideahut.quarkus.template.config;

import java.util.Set;

import jakarta.inject.Singleton;
import net.ideahut.quarkus.api.ApiAccess;
import net.ideahut.quarkus.api.ApiService;
import net.ideahut.quarkus.crud.CrudAction;
import net.ideahut.quarkus.crud.CrudHandler;
import net.ideahut.quarkus.crud.CrudHandlerImpl;
import net.ideahut.quarkus.crud.CrudPermission;
import net.ideahut.quarkus.crud.CrudProperties;
import net.ideahut.quarkus.crud.CrudResource;
import net.ideahut.quarkus.definition.CrudDefinition;
import net.ideahut.quarkus.entity.EntityInfo;
import net.ideahut.quarkus.entity.EntityTrxManager;
import net.ideahut.quarkus.entity.TrxManagerInfo;
import net.ideahut.quarkus.helper.ErrorHelper;
import net.ideahut.quarkus.helper.ObjectHelper;
import net.ideahut.quarkus.helper.StringHelper;
import net.ideahut.quarkus.mapper.DataMapper;
import net.ideahut.quarkus.template.Application;
import net.ideahut.quarkus.template.app.AppProperties;
import net.ideahut.quarkus.template.support.CrudSupport;

class CrudConfig {
	
	/*
	 * CRUD HANDLER
	 */
	@Singleton
	CrudHandler crudHandler(
		AppProperties appProperties,
		EntityTrxManager entityTrxManager,
		DataMapper dataMapper
	) {
		CrudDefinition crud = appProperties.crud().orElseThrow();
		
		return new CrudHandlerImpl()
				
		// semua query menggunakan sql (native) atau tidak
		.setAlwaysUseNative(crud.alwaysUseNative().orElse(null))
		
		// default maksimum jumlah data saat retrieve (PAGE, LIST, MAP)
		.setDefaultMaxLimit(crud.defaultMaxLimit().orElse(null))
		
		// EntityTrxManager
		.setEntityTrxManager(entityTrxManager)
		
		// Informasi aksi disertakan di respon atau tidak
		.setInfoEnabled(crud.infoEnabled().orElse(null))
		
		// Daftar filter specific yang akan disertakan saat query
		.setSpecificValueGetters(CrudSupport.getSpecificValueGetters())
		
		// Flag apakah bulk diaktifkan atau tidak
		.setBulkEnabled(crud.bulkEnabled().orElse(null))
		
		// Maksimum jumlah operasi / aksi CRUD yang dibolehkan, diisi 0 untuk tak terbatas
		.setMaxBulkSize(crud.maxBulkSize().orElse(null))
		
		// Maksimum jumlah dependensi / layer CRUD yang dibolehkan, diisi 0 untuk tak terbatas
		.setMaxBulkLayer(crud.maxBulkLayer().orElse(null));
		
	}
	
	/*
	 * CRUD RESOURCE
	 */
	@Singleton
	CrudResource crudResource(
		AppProperties appProperties,
		EntityTrxManager entityTrxManager,
		ApiService apiService
	) {
		CrudDefinition crud = appProperties.crud().orElseThrow();
		
		if (Boolean.TRUE.equals(crud.enableApiService().orElse(null))) {
			 // CrudResource  diambil menggunakan ApiService (PRODUCTION)
			 // - Parameter manager yang didefinisikan di CrudRequest tidak akan digunakan, karena sudah ada di table
			 // - Parameter name = crudCode
			return (manager, name) -> {
				ApiAccess apiAccess = ApiAccess.fromContext();
				CrudProperties properties = apiService.getApiCrudProperties(apiAccess, name);
				ErrorHelper.throwNull(properties, () -> StringHelper.format("CrudProperties not found, name: {}", name));
				return properties;
			};
		} else {
			// CrudResource berdasarkan nama class yang didefinisikan di CrudRequest (DEVELOPMENT)
			return (manager, name) -> {
				Class<?> clazz = ObjectHelper.safeClassOf(Application.Package.APPLICATION + ".entity." + name);
				ErrorHelper.throwNull(clazz, () -> StringHelper.format("Entity class not found, name: {}", name));
				TrxManagerInfo trxManagerInfo = TrxManagerInfo.getTrxManagerInfo(entityTrxManager, manager);
				ErrorHelper.throwNull(trxManagerInfo, () -> StringHelper.format("TrxManagerInfo not found, manager: {}", manager));
				EntityInfo entityInfo = trxManagerInfo.getEntityInfo(clazz);
				return new CrudProperties()
				.setEntityInfo(entityInfo)
				.setMaxLimit(crud.defaultMaxLimit().orElse(100))
				.setUseNative(crud.alwaysUseNative().orElse(Boolean.FALSE));
			};
		}
		
	}
	
	/*
	 * CRUD PRERMISSION
	 */
	@Singleton
	CrudPermission crudPermission(
		AppProperties appProperties	
	) {
		CrudDefinition crud = appProperties.crud().orElseThrow();
		
		if (!Boolean.FALSE.equals(crud.enablePermission().orElse(null))) {
			// Cek berdasarkan action (CREATE, UPDATE, DELETE, dll)
			return (action, request) -> {
				CrudProperties properties = request.getProperties();
				Set<CrudAction> actions = properties.getActions();
				return actions != null && actions.contains(action);
			};
		} else {
			// Semua request diijinkan
			return (action, request) -> true;
		}
		
	}
	
}
