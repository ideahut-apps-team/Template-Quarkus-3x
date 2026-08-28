package net.ideahut.quarkus.template.controller.test;


import java.nio.file.Files;
import java.util.List;

import org.jboss.resteasy.reactive.RestForm;
import org.jboss.resteasy.reactive.multipart.FileUpload;

import jakarta.inject.Inject;
import jakarta.mail.internet.InternetAddress;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import lombok.Getter;
import lombok.Setter;
import net.ideahut.quarkus.annotation.Public;
import net.ideahut.quarkus.helper.ErrorHelper;
import net.ideahut.quarkus.helper.ObjectHelper;
import net.ideahut.quarkus.helper.StringHelper;
import net.ideahut.quarkus.mail.MailHandler;
import net.ideahut.quarkus.mail.MailObject;
import net.ideahut.quarkus.mail.MailObject.Attachment;
import net.ideahut.quarkus.object.Result;

/*
 * Contoh penggunaan MailHandler
 */
@Public
@Path("/test/mail")
class MailController {

	private final MailHandler mailHandler;
	
	@Inject
	MailController(
		MailHandler mailHandler	
	) {
		this.mailHandler = mailHandler;
	}
	
	@Setter
	@Getter
	static class Form {
		@RestForm("from")
		private String from;
		@RestForm("to")
		private List<String> to;
		@RestForm("cc")
		private List<String> cc;
		@RestForm("bcc")
		private List<String> bcc;
		@RestForm("subject")
		private String subject;
		@RestForm("content")
		private String content;
		@RestForm("attachment")
		private FileUpload attachment;
	}
	
	@POST
	@Path("/send/sync")
	public Result sendSync(Form form) {
		return sendMail(form, false);
	}
	
	@POST
	@Path("/send/async")
	public Result sendAsync(Form form) {
		return sendMail(form, true);
	}
	
	private InternetAddress toInternetAddress(
		String address, 
		String personal
	) {
		try {
			return new InternetAddress(address, personal);
		} catch (Exception e) {
			throw ErrorHelper.exception(e);
		}
	}
	
	private Result sendMail(Form form, boolean async) {
		MailObject mail = new MailObject();
		mail.setSubject(ObjectHelper.useOrElse(!StringHelper.isBlank(form.getSubject()), form.getSubject(), "Test-Mail"));
		mail.setHtmlText(ObjectHelper.useOrElse(!StringHelper.isBlank(form.getContent()), form.getContent(), "Ini adalah contoh email"));
		ObjectHelper.callIf(!StringHelper.isBlank(form.getFrom()), () -> mail.setFrom(toInternetAddress(form.getFrom(), form.getFrom())));
		ObjectHelper.callIf(
			form.getTo() != null && !form.getTo().isEmpty(), 
			() -> {
				List<InternetAddress> lto = form.getTo()
				.stream()
				.filter(email -> !StringHelper.isBlank(email))
				.map(email -> toInternetAddress(email, email))
				.toList();
				return mail.setTo(lto.toArray(new InternetAddress[0]));
			}
		);
		ObjectHelper.callIf(
			form.getCc() != null && !form.getCc().isEmpty(), 
			() -> {
				List<InternetAddress> lcc = form.getCc()
				.stream()
				.filter(email -> !StringHelper.isBlank(email))
				.map(email -> toInternetAddress(email, email))
				.toList();
				return mail.setCc(lcc.toArray(new InternetAddress[0]));
			}
		);
		ObjectHelper.callIf(
			form.getBcc() != null && !form.getBcc().isEmpty(), 
			() -> {
				List<InternetAddress> lbcc = form.getBcc()
				.stream()
				.filter(email -> !StringHelper.isBlank(email))
				.map(email -> toInternetAddress(email, email))
				.toList();
				return mail.setBcc(lbcc.toArray(new InternetAddress[0]));
			}
		);
		return ObjectHelper.callOrElse(
			form.getAttachment() == null, 
			() -> {
				mailHandler.send(mail, async);
				return Result.success();
			},
			() -> {
				java.nio.file.Path path = form.getAttachment().uploadedFile();
				byte[] bytes = Files.readAllBytes(path);
				Attachment attachment = Attachment.of("Attachment", bytes, form.getAttachment().contentType());
				mail
				.setMultipart(true)
				.setAttachment(attachment);
				mailHandler.send(mail, async);
				Result result = Result.success()
				.setInfo("name", form.getAttachment().name())
				.setInfo("fileName", form.getAttachment().fileName())
				.setInfo("contentType", form.getAttachment().contentType())
				.setInfo("contentLength", form.getAttachment().size());
				Files.deleteIfExists(path); // delete attachment from storage
				return result;
			}
		);
	}
	
}
