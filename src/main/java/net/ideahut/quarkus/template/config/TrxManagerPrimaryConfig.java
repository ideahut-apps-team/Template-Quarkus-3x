package net.ideahut.quarkus.template.config;

import io.quarkus.arc.DefaultBean;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Named;
import jakarta.persistence.EntityManagerFactory;
import net.ideahut.quarkus.entity.PlatformTransactionManager;
import net.ideahut.quarkus.helper.HibernateHelper;
import net.ideahut.quarkus.template.app.AppProperties;
import net.ideahut.quarkus.template.app.AppProperties.TrxDatasource;

class TrxManagerPrimaryConfig {
	
	static final String PREFIX = "primary";
	
	@DefaultBean
	@ApplicationScoped
	@Named(PREFIX + "EntityManagerFactory")
	EntityManagerFactory entityManagerFactory(
		AppProperties appProperties
	) {
		AppProperties.TrxMain trxMain = appProperties.trxManager().orElseThrow().primary().orElseThrow();
		AppProperties.TrxAudit trxAudit = trxMain.audit().orElse(null);
		return HibernateHelper.createEntityManagerFactory(
			TrxDatasource.getDefinition(trxMain.datasource().orElse(null)), 
			trxMain, 
			trxAudit != null ? trxAudit.id().orElse(null) : null, 
			trxAudit != null ? AppProperties.TrxDatasource.getDefinition(trxAudit.datasource().orElse(null)) : null,
			trxAudit
		);
	}
	
	@DefaultBean
	@ApplicationScoped
	@Named(PREFIX + "TransactionManager")
	PlatformTransactionManager transactionManager(
		@Named(PREFIX + "EntityManagerFactory")	
		EntityManagerFactory entityManagerFactory	
	) {
		return PlatformTransactionManager.create(entityManagerFactory);
	}
	
}
