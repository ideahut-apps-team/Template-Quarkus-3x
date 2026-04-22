package net.ideahut.quarkus.template.config;

import jakarta.inject.Named;
import jakarta.inject.Singleton;
import jakarta.persistence.EntityManagerFactory;
import net.ideahut.quarkus.entity.PlatformTransactionManager;
import net.ideahut.quarkus.helper.HibernateHelper;
import net.ideahut.quarkus.helper.ObjectHelper;
import net.ideahut.quarkus.template.app.AppProperties;
import net.ideahut.quarkus.template.app.AppProperties.TrxDatasource;

class TrxManagerSecondaryConfig {
	
	private static final String PREFIX = "secondary";
	
	@Singleton
	@Named(PREFIX + "EntityManagerFactory")
	EntityManagerFactory entityManagerFactory(
		AppProperties appProperties
	) throws Exception {
		AppProperties.TrxMain trxMain = appProperties.trxManager().orElseThrow().secondary().orElseThrow();
		AppProperties.TrxAudit trxAudit = trxMain.audit().orElse(null);
		return HibernateHelper.createEntityManagerFactory(
			TrxDatasource.getDefinition(trxMain.datasource().orElse(null)), 
			trxMain, 
			ObjectHelper.callIf(trxAudit != null, () -> trxAudit.id().orElse(null)), 
			ObjectHelper.callIf(trxAudit != null, () -> AppProperties.TrxDatasource.getDefinition(trxAudit.datasource().orElse(null))),
			trxAudit
		);
	}

	@Singleton
	@Named(PREFIX + "TransactionManager")
	PlatformTransactionManager transactionManager(
		@Named(PREFIX + "EntityManagerFactory")	
		EntityManagerFactory entityManagerFactory	
	) {
		return PlatformTransactionManager.create(entityManagerFactory);
	}
	
}
