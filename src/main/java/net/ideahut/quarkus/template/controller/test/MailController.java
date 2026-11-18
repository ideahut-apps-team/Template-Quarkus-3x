package net.ideahut.quarkus.template.controller.test;


import java.nio.file.Files;
import java.util.List;

import org.jboss.resteasy.reactive.RestForm;
import org.jboss.resteasy.reactive.multipart.FileUpload;

import jakarta.inject.Inject;
import jakarta.mail.internet.InternetAddress;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
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
	
	@POST
	@Path("/send/sync")
	public Result sendSync(
		@RestForm("from") String from,
		@RestForm("to") List<String> to,
		@RestForm("cc") List<String> cc,
		@RestForm("bcc") List<String> bcc,
		@RestForm("subject") String subject,
		@RestForm("content") String content,
		@RestForm("attachment") FileUpload file	
	) throws Exception {
		MailObject mail = createMail(from, to, cc, bcc, subject, content, file);
		mailHandler.send(mail, false);
		return Result.success("sync");
	}
	
	@POST
	@Path("/send/async")
	public Result sendAsync(
		@RestForm("from") String from,
		@RestForm("to") List<String> to,
		@RestForm("cc") List<String> cc,
		@RestForm("bcc") List<String> bcc,
		@RestForm("subject") String subject,
		@RestForm("content") String content,
		@RestForm("attachment") FileUpload file
	) throws Exception {
		MailObject mail = createMail(from, to, cc, bcc, subject, content, file);
		mailHandler.send(mail, true);
		return Result.success("async");
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
	
	private MailObject createMail(
		String from,
		List<String> to,
		List<String> cc,
		List<String> bcc,
		String subject,
		String content,
		FileUpload file
	) throws Exception {
		MailObject mail = new MailObject();
		mail.setSubject(ObjectHelper.useOrElse(!StringHelper.isBlank(subject), subject, "Test-Mail"));
		mail.setHtmlText(ObjectHelper.useOrElse(!StringHelper.isBlank(content), content, "Ini adalah contoh email"));
		if (!StringHelper.isBlank(from)) {
			mail.setFrom(toInternetAddress(from, from));
		}
		if (to != null && !to.isEmpty()) {
			List<InternetAddress> lto = to
			.stream()
			.filter(email -> !StringHelper.isBlank(email))
			.map(email -> toInternetAddress(email, email))
			.toList();
			mail.setTo(lto.toArray(new InternetAddress[0]));
		}
		if (cc != null && !cc.isEmpty()) {
			List<InternetAddress> lcc = cc
			.stream()
			.filter(email -> !StringHelper.isBlank(email))
			.map(email -> toInternetAddress(email, email))
			.toList();
			mail.setCc(lcc.toArray(new InternetAddress[0]));
		}
		if (bcc != null && !bcc.isEmpty()) {
			List<InternetAddress> lbcc = bcc
			.stream()
			.filter(email -> !StringHelper.isBlank(email))
			.map(email -> toInternetAddress(email, email))
			.toList();
			mail.setBcc(lbcc.toArray(new InternetAddress[0]));
		}
		if (file != null) {
			java.nio.file.Path path = file.uploadedFile();
			byte[] bytes = Files.readAllBytes(path);
			Files.deleteIfExists(path); // delete attachment from storage
			Attachment attachment = Attachment.of("Attachment", bytes, file.contentType());
			 mail
	        .setMultipart(true)
	        .setAttachment(attachment);
		}
		return mail;
	}
	
}
