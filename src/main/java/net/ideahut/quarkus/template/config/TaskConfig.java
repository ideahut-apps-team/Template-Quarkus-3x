package net.ideahut.quarkus.template.config;

import io.quarkus.arc.DefaultBean;
import jakarta.inject.Named;
import jakarta.inject.Singleton;
import net.ideahut.quarkus.definition.TaskDefinition;
import net.ideahut.quarkus.task.TaskHandler;
import net.ideahut.quarkus.task.TaskHandlerImpl;
import net.ideahut.quarkus.task.TaskProperties;
import net.ideahut.quarkus.template.app.AppConstants;
import net.ideahut.quarkus.template.app.AppProperties;

/*
 * Konfigurasi TaskHandler
 * Untuk proses asynchronous
 */
class TaskConfig {
	
	@Singleton
	@Named(AppConstants.Bean.Task.PRIMARY)
	@DefaultBean
    TaskHandler commonTask(
    	AppProperties appProperties		
    ) {
		TaskProperties properties = TaskDefinition.convert(appProperties.task().orElseThrow().primary().orElseThrow());
		return new TaskHandlerImpl().setTaskProperties(properties);
    }
	
	@Singleton
	@Named(AppConstants.Bean.Task.AUDIT)
	TaskHandler auditTask(
		AppProperties appProperties		
	) {
		TaskProperties properties = TaskDefinition.convert(appProperties.task().orElseThrow().audit().orElseThrow());
		return new TaskHandlerImpl().setTaskProperties(properties);
    }
	
	@Singleton
	@Named(AppConstants.Bean.Task.REST)
	TaskHandler restTask(
		AppProperties appProperties		
	) {
		TaskProperties properties = TaskDefinition.convert(appProperties.task().orElseThrow().rest().orElseThrow());
		return new TaskHandlerImpl().setTaskProperties(properties);
    }
	
	@Singleton
	@Named(AppConstants.Bean.Task.WEB_ASYNC)
	TaskHandler webAsyncTask(
		AppProperties appProperties		
	) {
		TaskProperties properties = TaskDefinition.convert(appProperties.task().orElseThrow().webAsync().orElseThrow());
		return new TaskHandlerImpl().setTaskProperties(properties);
    }
	
}
