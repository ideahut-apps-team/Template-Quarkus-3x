package net.ideahut.quarkus.template.controller.test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

import org.jboss.resteasy.reactive.RestForm;
import org.jboss.resteasy.reactive.multipart.FileUpload;

import jakarta.validation.constraints.NotBlank;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.StreamingOutput;
import lombok.extern.slf4j.Slf4j;
import net.ideahut.quarkus.annotation.Body;
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
		return StringHelper.toBytes("BYTES-" + System.nanoTime());
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
	
	@Body // <- tambahkan annotation ini agar output dikirim sesuai dengan yang disusun di method ini
	@GET
    @Path("/outstream")
    public Response outstream() {
        StreamingOutput body = output -> {
            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream() {
                private final byte[] bytes = StringHelper.toBytes("Haloooo-" + System.nanoTime());
                @Override
                public synchronized void writeTo(OutputStream outputStream) throws IOException {
                	outputStream.write(bytes);
                }
            };
            try {
            	byteArrayOutputStream.writeTo(output);
                output.flush();
            } catch (IOException e) {
                throw ErrorHelper.exception(e);
            }
        };
        return Response.ok(body).build();
    }
	
	@Body // <- tambahkan annotation ini agar output tidak diubah sesuai dengan format framework
	@GET
	@Path("/download")
	public Response download() {
		StreamingOutput body = output -> {
			try {
				for (int i = 0; i < 10; i++) {
					
					// simulasi proses write yang lambat
					TimeUnit.SECONDS.sleep(1);
					
					output.write(StringHelper.toBytes("DATA-" + i + "\n"));
					output.flush();
				}
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
			}
		};
		return Response.ok(body)
		.header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + System.nanoTime() + ".txt\"")
		.header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_OCTET_STREAM)
		.build();
	}
	
	// contoh streaming baca request dan kirim response
	@Body // <- tambahkan annotation ini agar output tidak diubah sesuai dengan format framework
	@POST
	@Path("/streaming")
	public Response streaming(
		@QueryParam("buffer") Integer buffer, 
		@QueryParam("delay") Integer delay,
		InputStream inputStream // <- diambil dari httpRequest
	) {
		StreamingOutput body = output -> {
			try (inputStream) { // Otomatis menutup inputStream setelah selesai
				int sleep = (delay != null && delay > 0) ? delay : 100;
				int bufferSize = (buffer != null && buffer > 0) ? buffer : 16;
				byte[] bytesBuffer = new byte[bufferSize];
				long totalBytes = 0;
				int bytesRead;
				while ((bytesRead = inputStream.read(bytesBuffer)) != -1) {
					
					// simulasi proses write yang lambat
					TimeUnit.MILLISECONDS.sleep(sleep);

					output.write(bytesBuffer, 0, bytesRead);
					output.flush();
					totalBytes += bytesRead;
				}
				byte[] endStreaming = StringHelper.toBytes("\n\nTotal Bytes: " + totalBytes);
				output.write(endStreaming, 0, endStreaming.length);
				output.flush();
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
			} catch (Exception e) {
				throw new WebApplicationException(e);
			}
		};
		return Response.ok(body)
		.header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_OCTET_STREAM)
		.build();
	}
	
	@POST
	@Path("/multipart")
	public Result multipart(
		@NotBlank @RestForm("name") String name,
		@RestForm("file") FileUpload file
	) throws Exception {
		Result result = Result.success()
		.setInfo("name", name);
		if (file != null) {
			byte[] bytes = Files.readAllBytes(file.uploadedFile());
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
