package net.ideahut.quarkus.template.job;

import java.net.URL;

import org.quartz.JobExecutionContext;

import net.ideahut.quarkus.helper.FrameworkHelper;
import net.ideahut.quarkus.helper.StringHelper;
import net.ideahut.quarkus.job.JobBase;

public class PageScrapperJob extends JobBase {

	@Override
	protected void run(JobExecutionContext context) throws Exception {
		String source = getConfigValue(context, String.class, "URL", "https://google.com");
		URL url = new URL(source);
		byte[] bytes = FrameworkHelper.toByteArray(url.openStream());
		String content = StringHelper.toString(bytes);
		logger().info(content);
	}

}
