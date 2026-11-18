package net.ideahut.quarkus.template;

import io.quarkus.runtime.Quarkus;
import io.quarkus.runtime.ShutdownEvent;
import io.quarkus.runtime.StartupEvent;
import io.quarkus.runtime.annotations.QuarkusMain;
import io.smallrye.common.annotation.RunOnVirtualThread;
import io.vertx.ext.web.Router;
import jakarta.enterprise.event.Observes;
import jakarta.ws.rs.ApplicationPath;
import lombok.extern.slf4j.Slf4j;
import net.ideahut.quarkus.admin.AdminHandler;
import net.ideahut.quarkus.definition.LauncherDefinition;
import net.ideahut.quarkus.helper.FrameworkHelper;
import net.ideahut.quarkus.helper.ObjectHelper;
import net.ideahut.quarkus.helper.StringHelper;
import net.ideahut.quarkus.helper.WebHelper;
import net.ideahut.quarkus.job.SchedulerHandler;
import net.ideahut.quarkus.mapper.DataMapper;
import net.ideahut.quarkus.security.SecurityCredential;
import net.ideahut.quarkus.template.app.AppConstant;
import net.ideahut.quarkus.template.app.AppProperties;
import net.ideahut.quarkus.template.config.NativeConfig;
import net.ideahut.quarkus.web.WebLauncher;

/*
 * Main Class, untuk eksekusi aplikasi
 * 
 * Agar semua request bisa jalan di Virtual Thread tambahkan annotation:
 *    @ApplicationPath("/")
 *    @RunOnVirtualThread
 * Main Class juga harus extends jakarta.ws.rs.core.Application
 * Dan tambahkan properties:
 *    quarkus.virtual-threads.enabled: true
 *    quarkus.virtual-threads.name-prefix: template-quarkus-vt-
 */

@Slf4j
@QuarkusMain
@ApplicationPath("/")
@RunOnVirtualThread
public class Application extends jakarta.ws.rs.core.Application implements WebLauncher {
	
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
			FrameworkHelper.getMapBean(SchedulerHandler.class)
			.forEach((name, scheduler) -> {
				try {
					scheduler.start();
				} catch (Exception e) {
					log.error(StringHelper.format("Failed to start scheduler: {}", name), e);
				}
			});
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
	public void onLog(
		LauncherDefinition.Log.Type type, 
		LauncherDefinition.Log.Level level, 
		String message, 
		Throwable throwable
	) {
		level = ObjectHelper.useOrDefault(level, LauncherDefinition.Log.Level.DEBUG);
		log.atLevel(org.slf4j.event.Level.valueOf(level.name())).log(message, throwable);
	}
	
	/*
	 * ROUTER
	 */
	@Override
	public void onRouter(Router router) {
		AppProperties appProperties = FrameworkHelper.getBean(AppProperties.class);
		AdminHandler adminHandler = FrameworkHelper.getBean(AdminHandler.class);
		if (adminHandler != null) {
			SecurityCredential adminCredential = FrameworkHelper.getBean(AppConstant.Bean.Credential.ADMIN, SecurityCredential.class);
			WebHelper.Router.admin(router, adminHandler, adminCredential, appProperties.publicBaseUrl().orElse(null));
		}
		DataMapper dataMapper = FrameworkHelper.getBean(DataMapper.class);
		WebHelper.Router.root(router, appProperties.filter().orElse(null), dataMapper, () -> FrameworkHelper.randomAlphanumeric(10), Application::isReady);
	}
	
}
