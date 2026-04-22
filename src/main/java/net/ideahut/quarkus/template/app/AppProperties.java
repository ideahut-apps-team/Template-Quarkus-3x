package net.ideahut.quarkus.template.app;

import java.util.Optional;

import io.smallrye.config.ConfigMapping;
import io.smallrye.config.ConfigMapping.NamingStrategy;
import net.ideahut.quarkus.definition.AdminDefinition;
import net.ideahut.quarkus.definition.AgroalDefinition;
import net.ideahut.quarkus.definition.ApiDefinition;
import net.ideahut.quarkus.definition.CacheGroupDefinition;
import net.ideahut.quarkus.definition.CrudDefinition;
import net.ideahut.quarkus.definition.DatabaseAuditDefinition;
import net.ideahut.quarkus.definition.DatasourceDefinition;
import net.ideahut.quarkus.definition.DbcpDefinition;
import net.ideahut.quarkus.definition.FilterDefinition;
import net.ideahut.quarkus.definition.ForeignKeyDefinition;
import net.ideahut.quarkus.definition.GridDefinition;
import net.ideahut.quarkus.definition.HibernateDefinition;
import net.ideahut.quarkus.definition.HikariDefinition;
import net.ideahut.quarkus.definition.KafkaDefinition;
import net.ideahut.quarkus.definition.LauncherDefinition;
import net.ideahut.quarkus.definition.MailDefinition;
import net.ideahut.quarkus.definition.RedisDefinition;
import net.ideahut.quarkus.definition.RequestInfoDefinition;
import net.ideahut.quarkus.definition.RestDefinition;
import net.ideahut.quarkus.definition.TaskDefinition;
import net.ideahut.quarkus.helper.ObjectHelper;

/*
 * Class properties yang definisinya sama dengan application.yaml
 */
@ConfigMapping(prefix = "config", namingStrategy = NamingStrategy.VERBATIM)
public interface AppProperties {
	
	// Base URL untuk diakses public
	Optional<String> publicBaseUrl();
	
	// Log semua error yang terjadi
	Optional<Boolean> logAllError();
	
	// Binary serializer
	Optional<String> binarySerializer();
	
	// Start scheduler pada saat startup
	Optional<Boolean> autoStartScheduler();
	
	// Direktori file message berdasarkan bahasa
	Optional<String> messagePath();
	
	Optional<RequestInfoDefinition> request();
	
	// Launcher log, bean configure, & init
	Optional<LauncherDefinition> launcher();
	
	// Definisi headers (cors), result time, & trace (log)
	Optional<FilterDefinition> filter();
	
	// Parameter untuk menghandle anotasi @ForeignKeyEntity
	// Ini solusi jika terjadi error saat membuat native image dimana entity memiliki @ManyToOne & @OneToMany
	// tapi package-nya berbeda dengan package project (error ByteCodeProvider saat runtime)
	Optional<ForeignKeyDefinition> foreignKey();
	
	// CRUD
	Optional<CrudDefinition> crud();
	
	// Audit
	Optional<DatabaseAuditDefinition> audit();
	
	// Task Handler
	Optional<Task> task();
	
	// Redis
	Optional<Redis> redis();
		
	// Admin
	Optional<AdminDefinition> admin();
	
	// API
	Optional<ApiDefinition> api();
	
	// Rest
	Optional<RestDefinition> rest();
	
	// Cache
	Optional<CacheGroupDefinition> cache();
	
	// Grid
	Optional<GridDefinition> grid();
	
	// Mail
	Optional<MailDefinition> mail();
	
	// Kafka
	Optional<KafkaDefinition> kafka();
	
	// TrxManager
	Optional<TrxManager> trxManager();
	
	
	/*
	 * REDIS
	 */
	public static interface Redis {
		Optional<RedisDefinition> primary();
		Optional<RedisDefinition> access();
	}
	
	/*
	 * TASK
	 */
	public static interface Task {
		Optional<TaskDefinition> primary();
		Optional<TaskDefinition> audit();
		Optional<TaskDefinition> rest();
		Optional<TaskDefinition> webAsync();
	}
	
	/*
	 * TRX MANAGER
	 */
	public static interface TrxManager {
		Optional<TrxMain> primary();
		Optional<TrxMain> secondary();
	}
	
	public static interface TrxDatasource {
		public enum Type {
			BASIC,
			HIKARI,
			AGROAL,
			DBCP
		}
		Optional<Type> type();
		Optional<DatasourceDefinition> basic();
		Optional<HikariDefinition> hikari();
		Optional<AgroalDefinition> agroal();
		Optional<DbcpDefinition> dbcp();
		public static DatasourceDefinition getDefinition(TrxDatasource datasource) {
			if (datasource != null) {
				Type type = ObjectHelper.useOrDefault(datasource.type().orElse(null), () -> Type.BASIC);
				switch (type) {
				case HIKARI:
					return datasource.hikari().orElse(null);
				case AGROAL:
					return datasource.agroal().orElse(null);
				case DBCP:
					return datasource.dbcp().orElse(null);
				default:
					return datasource.basic().orElse(null);
				}
			}
			return null;
		}
	}
	
	public static interface TrxAudit extends HibernateDefinition {
		Optional<String> id();
		Optional<TrxDatasource> datasource();
	}
	
	public static interface TrxMain extends HibernateDefinition {
		Optional<TrxDatasource> datasource();
		Optional<TrxAudit> audit();
	}
	
}
