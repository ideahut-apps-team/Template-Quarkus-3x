package net.ideahut.quarkus.template.controller.test;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.Callable;

import jakarta.inject.Inject;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.QueryParam;
import net.ideahut.quarkus.annotation.Public;
import net.ideahut.quarkus.helper.ThreadHelper;
import net.ideahut.quarkus.kafka.KafkaHandler;
import net.ideahut.quarkus.kafka.KafkaSender;
import net.ideahut.quarkus.kafka.KafkaSenderReceiver;
import net.ideahut.quarkus.object.Result;
import net.ideahut.quarkus.sysparam.dto.SysParamDto;
import net.ideahut.quarkus.task.TaskListExecutor;
import net.ideahut.quarkus.task.TaskResult;

/*
 * Contoh penggunaan KafkaHandler
 */
@Public
@Path("/test/kafka")
class KafkaController {

	private final KafkaHandler kafkaHandler;
	private final KafkaSenderReceiver<String, String, SysParamDto> kafkaSenderReceiver;
	private final KafkaSender<String, String> stringSender;
	
	@Inject
	KafkaController(
		KafkaHandler kafkaHandler	
	) {
		this.kafkaHandler = kafkaHandler;
		this.kafkaSenderReceiver = kafkaHandler.createDynamicSenderReceiver("SAMPLE.REPLY");
		this.stringSender = kafkaHandler.createDynamicSender("SAMPLE.STRING");
	}
	
	@GET
	@Path("/reply")
	public Result reply(
		@NotBlank @QueryParam("text") String text,
		@Positive @QueryParam("total") Integer total
	) {
		int threads = total > 100 ? 100 : total;
		TaskListExecutor executor = TaskListExecutor.of(threads);
		for (int i = 0; i < total; i++) {
			int fi = i;
			executor.add(new Callable<SysParamDto>() {
				@Override
				public SysParamDto call() throws Exception {
					return ThreadHelper.get(kafkaSenderReceiver.sendAndReceive(fi + "::" + text, Duration.ofSeconds(10))).value();
				}
			});
		}
		List<TaskResult> data = executor.getResults();
		return Result.success(data).setInfo("text", text).setInfo("total", total);
	}
	
	@GET
	@Path("/send/string")
	public Result sendString(
		@NotBlank @QueryParam("text") String text,
		@Positive @QueryParam("total") Integer total
	) {
		for (int i = 0; i < total; i++) {
			stringSender.send(text + "::STRING::" + System.nanoTime());
		}
		return Result.success();
	}
	
	@GET
	@Path("/send/bytes")
	public Result sendBytes(
		@NotBlank @QueryParam("text") String text,
		@Positive @QueryParam("total") Integer total
	) {
		KafkaSender<String, byte[]> bytesSender = kafkaHandler.getStaticSender("SAMPLE.BYTES");
		for (int i = 0; i < total; i++) {
			bytesSender.send((text + "::BYTES::" + System.nanoTime()).getBytes());
		}
		return Result.success();
	}
	
}
