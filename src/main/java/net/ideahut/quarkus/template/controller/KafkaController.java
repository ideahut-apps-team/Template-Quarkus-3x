package net.ideahut.quarkus.template.controller;

import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;

import io.smallrye.common.constraint.NotNull;
import jakarta.inject.Inject;
import jakarta.validation.constraints.NotBlank;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.QueryParam;
import net.ideahut.quarkus.annotation.Public;
import net.ideahut.quarkus.kafka.KafkaHandler;
import net.ideahut.quarkus.kafka.KafkaSender;
import net.ideahut.quarkus.kafka.KafkaSenderReceiver;
import net.ideahut.quarkus.object.Result;
import net.ideahut.quarkus.sysparam.dto.SysParamDto;
import net.ideahut.quarkus.task.TaskListExecutor;
import net.ideahut.quarkus.task.TaskResult;

@Public
@Path("/kafka")
class KafkaController {
	
	private final KafkaSenderReceiver<String, String, SysParamDto> senderReceiver;
	private final KafkaSender<String, String> stringSender;
	private final KafkaSender<String, byte[]> bytesSender;
	
	@Inject
	KafkaController(
		KafkaHandler kafkaHandler	
	) {
		this.senderReceiver = kafkaHandler.createDynamicSenderReceiver("SAMPLE.REPLY");
		this.stringSender = kafkaHandler.createDynamicSender("SAMPLE.STRING");
		this.bytesSender = kafkaHandler.getStaticSender("SAMPLE.BYTES");
	}
	
	@GET
	@Path("/reply")
	public Result reply(
		@NotBlank @QueryParam("text") String text,
		@NotNull @QueryParam("total") Integer total
	) {
		int threads = total > 100 ? 100 : total;
		TaskListExecutor executor = TaskListExecutor.of(threads);
		for (int i = 0; i < total; i++) {
			int fi = i;
			executor.add(new Callable<SysParamDto>() {
				@Override
				public SysParamDto call() throws Exception {
					return senderReceiver.sendAndReceive(fi + "::" + text).get(10, TimeUnit.SECONDS).value();
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
		@NotNull @QueryParam("total") Integer total
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
		@NotNull @QueryParam("total") Integer total
	) {
		for (int i = 0; i < total; i++) {
			bytesSender.send((text + "::BYTES::" + System.nanoTime()).getBytes());
		}
		return Result.success();
	}
	
}
