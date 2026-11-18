package net.ideahut.quarkus.template.config;

import io.quarkus.arc.DefaultBean;
import io.quarkus.redis.datasource.RedisDataSource;
import io.vertx.core.Vertx;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Named;
import net.ideahut.quarkus.definition.RedisDefinition;
import net.ideahut.quarkus.redis.RedisHelper;
import net.ideahut.quarkus.redis.RedisProperties;
import net.ideahut.quarkus.template.app.AppConstant;
import net.ideahut.quarkus.template.app.AppProperties;

/*
 * Konfigurasi Redis
 */
class RedisConfig {
	
	@ApplicationScoped
	@Named(AppConstant.Bean.Redis.PRIMARY)
	@DefaultBean
	RedisDataSource primaryRedis(
		AppProperties appProperties,
		Vertx vertx
	) {
		RedisProperties properties = RedisDefinition.convert(appProperties.redis().orElseThrow().primary().orElseThrow());
		return RedisHelper.createRedisDataSource(vertx, properties);
	}
	
	@ApplicationScoped
	@Named(AppConstant.Bean.Redis.ACCESS)
	RedisDataSource accessRedis(
		AppProperties appProperties,
		Vertx vertx
	) {
		RedisProperties properties = RedisDefinition.convert(appProperties.redis().orElseThrow().access().orElseThrow());
		return RedisHelper.createRedisDataSource(vertx, properties);
	}
	
}
