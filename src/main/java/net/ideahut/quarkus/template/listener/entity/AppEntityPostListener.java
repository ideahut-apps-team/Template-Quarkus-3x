package net.ideahut.quarkus.template.listener.entity;

import java.util.HashMap;
import java.util.Map;

import io.quarkus.runtime.Startup;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Named;
import net.ideahut.quarkus.api.ApiHandler;
import net.ideahut.quarkus.audit.AuditHandler;
import net.ideahut.quarkus.bean.BeanConfigure;
import net.ideahut.quarkus.entity.EntityPostListener;
import net.ideahut.quarkus.helper.ObjectHelper;
import net.ideahut.quarkus.module.ModuleApi;
import net.ideahut.quarkus.module.ModuleSysParam;
import net.ideahut.quarkus.sysparam.SysParamHandler;
import net.ideahut.quarkus.task.TaskHandler;
import net.ideahut.quarkus.template.app.AppConstants;

@Startup
@ApplicationScoped
class AppEntityPostListener implements EntityPostListener, BeanConfigure {
	
	private final AuditHandler auditHandler;
	private final TaskHandler taskHandler;
	private final ApiHandler apiHandler;
	private final SysParamHandler sysParamHandler;
	
	private Map<Class<?>, EntityPostListener> listeners = new HashMap<>();
	
	AppEntityPostListener(
		AuditHandler auditHandler,
		@Named(AppConstants.Bean.Task.AUDIT)
		TaskHandler taskHandler,
		ApiHandler apiHandler,
		SysParamHandler sysParamHandler
	) {
		this.auditHandler = auditHandler;
		this.taskHandler = taskHandler;
		this.apiHandler = apiHandler;
		this.sysParamHandler = sysParamHandler;
	}
	
	@Override
	public void onConfigureBean() throws Exception {
		listeners.clear();
		
		// SysParam
		listeners.putAll(ModuleSysParam.getEntityPostListeners(sysParamHandler));
		
		// Api
		listeners.putAll(ModuleApi.getEntityPostListeners(apiHandler));
		
	}

	@Override
	public void onPostInsert(Object entity) {
		auditHandler.save("INSERT", entity);
		taskHandler.execute(() -> {
			EntityPostListener listener = listeners.get(entity.getClass());
			ObjectHelper.runIf(listener != null, () -> listener.onPostInsert(entity));
		});
	}

	@Override
	public void onPostUpdate(Object entity) {
		auditHandler.save("UPDATE", entity);
		taskHandler.execute(() -> {
			EntityPostListener listener = listeners.get(entity.getClass());
			ObjectHelper.runIf(listener != null, () -> listener.onPostUpdate(entity));
		});
	}
	
	@Override
	public void onPostDelete(Object entity) {
		auditHandler.save("DELETE", entity);
		taskHandler.execute(() -> {
			EntityPostListener listener = listeners.get(entity.getClass());
			ObjectHelper.runIf(listener != null, () -> listener.onPostDelete(entity));
		});
	}
	
}
