package net.ideahut.quarkus.template;

import io.quarkus.runtime.Quarkus;
import io.quarkus.runtime.ShutdownEvent;
import io.quarkus.runtime.StartupEvent;
import io.quarkus.runtime.annotations.QuarkusMain;
import io.vertx.ext.web.Router;
import jakarta.enterprise.event.Observes;
import lombok.extern.slf4j.Slf4j;
import net.ideahut.quarkus.admin.AdminHandler;
import net.ideahut.quarkus.definition.LauncherDefinition;
import net.ideahut.quarkus.helper.FrameworkHelper;
import net.ideahut.quarkus.helper.ObjectHelper;
import net.ideahut.quarkus.helper.WebHelper;
import net.ideahut.quarkus.job.SchedulerHandler;
import net.ideahut.quarkus.mapper.DataMapper;
import net.ideahut.quarkus.security.SecurityCredential;
import net.ideahut.quarkus.template.app.AppConstants;
import net.ideahut.quarkus.template.app.AppProperties;
import net.ideahut.quarkus.template.config.NativeConfig;
import net.ideahut.quarkus.web.WebLauncher;

/*
 * Main Class, untuk eksekusi aplikasi
 */

@Slf4j
@QuarkusMain
public class Application implements WebLauncher {
	
	/*
	 * PACKAGE
	 */
	public static class Package {
		private Package() {}
		public static final String LIBRARY		= FrameworkHelper.PACKAGE;
		public static final String APPLICATION	= "net.ideahut.quarkus.template";
	}
	
	private static boolean ready = false;
	private static void setReady(boolean b) { ready = b; }
	public static boolean isReady() { return ready; }
	
	/*
	 * MAIN
	 */
	public static void main(String... args) {
		WebLauncher.runApp(Application.class, args);
	}
	
	/*
	 * STARTUP
	 */
	@Override
	public void onStartup(@Observes StartupEvent event, Router router) {
		WebLauncher.super.onStartup(event, router);
	}
	
	/*
	 * SHUTDOWN
	 */
	@Override
	public void onShutdown(@Observes ShutdownEvent event) {
		WebLauncher.super.onShutdown(event);
	}
	
	/*
	 * DEFINITION
	 */
	@Override
	public LauncherDefinition onDefinition() {
		return FrameworkHelper.getBean(AppProperties.class).launcher().orElse(null);
	}
	
	/*
	 * READY
	 */
	@Override
	public void onReady() {
		setReady(true);
		AppProperties appProperties = FrameworkHelper.getBean(AppProperties.class);
		if (Boolean.TRUE.equals(appProperties.autoStartScheduler().orElse(null))) {
			try {
				FrameworkHelper.getBean(SchedulerHandler.class).start();
			} catch (Exception e) {
				log.error("Failed to start Scheduler");
			}
		}
		NativeConfig.registerToNativeImageAgent();
	}
	
	/*
	 * ERROR
	 */
	@Override
	public void onError(Throwable throwable) {
		log.error("Application", throwable);
		Quarkus.asyncExit();
		System.exit(0);
	}
	
	/*
	 * LOG
	 */
	@Override
	public void onLog(LauncherDefinition.Log.Type type, LauncherDefinition.Log.Level level, String message, Throwable throwable) {
		level = ObjectHelper.useOrDefault(level, () -> LauncherDefinition.Log.Level.DEBUG);
		log.atLevel(org.slf4j.event.Level.valueOf(level.name())).log(message, throwable);
	}
	
	/*
	 * ROUTER
	 */
	@Override
	public void onRouter(Router router) {
		AppProperties appProperties = FrameworkHelper.getBean(AppProperties.class);
		AdminHandler adminHandler = FrameworkHelper.getBean(AdminHandler.class);
		ObjectHelper.runIf(
			null != adminHandler, 
			() -> {
				SecurityCredential adminCredential = FrameworkHelper.getBean(AppConstants.Bean.Credential.ADMIN, SecurityCredential.class);
				WebHelper.Router.admin(router, adminHandler, adminCredential, appProperties.publicBaseUrl().orElse(null));
			}
		);
		DataMapper dataMapper = FrameworkHelper.getBean(DataMapper.class);
		WebHelper.Router.root(router, appProperties.filter().orElse(null), dataMapper, () -> FrameworkHelper.randomAlphanumeric(10), Application::isReady);
	}
	
}
