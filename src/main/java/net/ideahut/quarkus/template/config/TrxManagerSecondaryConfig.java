package net.ideahut.quarkus.template.config;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Named;
import jakarta.persistence.EntityManagerFactory;
import net.ideahut.quarkus.entity.PlatformTransactionManager;
import net.ideahut.quarkus.helper.HibernateHelper;
import net.ideahut.quarkus.template.app.AppProperties;
import net.ideahut.quarkus.template.app.AppProperties.TrxDatasource;

class TrxManagerSecondaryConfig {
	
	static final String PREFIX = "secondary";
	
	@ApplicationScoped
	@Named(PREFIX + "EntityManagerFactory")
	EntityManagerFactory entityManagerFactory(
		AppProperties appProperties
	) {
		AppProperties.TrxMain trxMain = appProperties.trxManager().orElseThrow().secondary().orElseThrow();
		AppProperties.TrxAudit trxAudit = trxMain.audit().orElse(null);
		return HibernateHelper.createEntityManagerFactory(
			TrxDatasource.getDefinition(trxMain.datasource().orElse(null)), 
			trxMain, 
			trxAudit != null ? trxAudit.id().orElse(null) : null, 
			trxAudit != null ? AppProperties.TrxDatasource.getDefinition(trxAudit.datasource().orElse(null)) : null,
			trxAudit
		);
	}

	@ApplicationScoped
	@Named(PREFIX + "TransactionManager")
	PlatformTransactionManager transactionManager(
		@Named(PREFIX + "EntityManagerFactory")	
		EntityManagerFactory entityManagerFactory	
	) {
		return PlatformTransactionManager.create(entityManagerFactory);
	}
	
}
