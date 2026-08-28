package net.ideahut.quarkus.template.controller.test;

import java.nio.file.Files;
import java.util.function.Supplier;

import org.jboss.resteasy.reactive.RestForm;
import org.jboss.resteasy.reactive.multipart.FileUpload;

import jakarta.validation.constraints.NotBlank;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.Response;
import lombok.extern.slf4j.Slf4j;
import net.ideahut.quarkus.annotation.Public;
import net.ideahut.quarkus.helper.ErrorHelper;
import net.ideahut.quarkus.helper.StringHelper;
import net.ideahut.quarkus.helper.ThreadHelper;
import net.ideahut.quarkus.object.Message;
import net.ideahut.quarkus.object.Result;

/*
 * Contoh API untuk fungsi dasar http
 */
@Slf4j
@Public
@Path("/test/basic")
class BasicController {

	@GET
	@Path("/exception")
	public void exception() {
		throw ErrorHelper.exception(() -> StringHelper.format("ERROR-{}", System.nanoTime()));
	}
	
	@GET
	@Path("/virtualThread")
	public Result virtualThread() {
		Thread thread = Thread.currentThread();
		boolean isVt = ThreadHelper.isThreadVirtual(thread);
		return Result.success(isVt).setInfo("thread", thread.getName());
	}
	
	@GET
	@Path("/bytes")
	public byte[] bytes() {
		return ("BYTES-" + System.nanoTime()).getBytes();
	}
	
	@GET
	@Path("/string")
	public String string() {
		return "STRING-" + System.nanoTime();
	}
	
	@GET
	@Path("/responseEntity")
	public Response responseEntity() {
		return Response.ok()
		.header("Test-Strre", "string")
		.entity("STRRE-" + System.nanoTime())
		.build();
	}

	/**
	@GET
	@Path("/send")
	public void send(
		HttpServletRequest request,
		HttpServletResponse response
	) {
		//WebMvcHelper.sendResponse(request, response, null, false, "SEND-" + System.nanoTime()); //-
		WebMvcHelper.sendResponse(request, response, "SEND-" + System.nanoTime()); //-
		//WebMvcHelper.sendResponse(request, response, null, false, System.nanoTime()); //-
		//WebMvcHelper.sendResponse(request, response, System.nanoTime()); //-
		//WebMvcHelper.sendResponse(request, response, new Exception("ERROR-SEND-" + System.nanoTime())); //-
		
		/**
		String hval = System.nanoTime() + "";
		response.setHeader("xxx1", hval);
		response.setHeader("xxx2", hval);
		ResponseEntity<Message> re = ResponseEntity.ok()
		.header("xxx2", "KEREN", "LAGI")
		.header("yyyy", "NONE")
		.body(Message.of("YYY", "VALUE"));
		WebMvcHelper.sendResponse(request, response, re);
		*/
	//} //-
	
	@GET
	@Path("/result")
	public Result result() {
		return Result.success("RESULT-" + System.nanoTime());
	}
	
	@GET
	@Path("/message")
	public Message message() {
		return Message.of("MSG", "MESSAGE-{}", System.nanoTime());
	}
	
	/**
	@GET
	@Path("/outstream")
	public void outstream(HttpServletResponse response) {
		ByteArrayOutputStream out = new ByteArrayOutputStream() {
			private byte[] bytes = ("Haloooo-" + System.nanoTime()).getBytes();
			@Override
			public synchronized void writeTo(OutputStream out) throws IOException {
				out.write(bytes);
			}
		};
		try {
			out.writeTo(response.getOutputStream());
		} catch (IOException e) {
			throw ErrorHelper.exception(e);
		}
	}
	*/
	
	@POST
	@Path("/multipart")
	public Result multipart(
		@NotBlank @RestForm("name") String name,
		@RestForm("file") FileUpload file
	) throws Exception {
		Result result = Result.success()
		.setInfo("name", name);
		if (file != null) {
			byte[] bytes = Files.readAllBytes(file.uploadedFile()); // coba baca file
			result
			.setInfo("length", file.size())
			.setInfo("bytes", bytes.length)
			.setInfo("uploadedFile", file.uploadedFile())
			.setInfo("filePath", file.filePath())
			.setInfo("fileName", file.fileName());
		}
		return result;
	}
	
	@GET
	@Path("/logger")
	public void logger() {
		Throwable throwable = new Exception(StringHelper.format("EXCEPTION: {}", System.nanoTime() + ""));
		log.debug("{}", message(() -> "DEBUG"), throwable);
		log.trace("{}", message(() -> "TRACE"), throwable);
		log.info("{}", message(() -> "INFO-" + System.nanoTime()), throwable);
		log.warn("{}", message(() -> "WARN"), throwable);
		log.error("{}", message(() -> "ERROR"), throwable);
	}
	
	private Object message(Supplier<CharSequence> message) {
		return new Object() {
			@Override
			public String toString() {
				CharSequence charSequence = message != null ? message.get() : null;
				return charSequence != null ? charSequence.toString() : "";
			}
		};
	}
	
}
