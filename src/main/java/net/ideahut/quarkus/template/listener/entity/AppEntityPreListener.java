package net.ideahut.quarkus.template.listener.entity;

import java.util.HashMap;
import java.util.Map;

import io.quarkus.runtime.Startup;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Named;
import net.ideahut.quarkus.bean.BeanInitialize;
import net.ideahut.quarkus.entity.EntityPreListener;
import net.ideahut.quarkus.helper.ObjectHelper;
import net.ideahut.quarkus.task.TaskHandler;
import net.ideahut.quarkus.template.app.AppConstants;

@Startup
@ApplicationScoped
class AppEntityPreListener implements EntityPreListener, BeanInitialize {
	
	private final TaskHandler taskHandler;
	
	private Map<Class<?>, EntityPreListener> listeners = new HashMap<>();
	
	AppEntityPreListener(
		@Named(AppConstants.Bean.Task.AUDIT)
		TaskHandler taskHandler
	) {
		this.taskHandler = taskHandler;
	}
	
	@Override
	public void afterPropertiesSet() throws Exception {
		listeners.clear();
	}
	
	@Override
	public void onPreInsert(Object entity) {
		taskHandler.execute(() -> {
			EntityPreListener listener = listeners.get(entity.getClass());
			ObjectHelper.runIf(listener != null, () -> listener.onPreInsert(entity));
		});
	}
	
	@Override
	public void onPreUpdate(Object entity) {
		taskHandler.execute(() -> {
			EntityPreListener listener = listeners.get(entity.getClass());
			ObjectHelper.runIf(listener != null, () -> listener.onPreUpdate(entity));
		});
	}

	@Override
	public void onPreDelete(Object entity) {
		taskHandler.execute(() -> {
			EntityPreListener listener = listeners.get(entity.getClass());
			ObjectHelper.runIf(listener != null, () -> listener.onPreDelete(entity));
		});
	}
	
}
