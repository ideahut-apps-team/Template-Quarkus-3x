package net.ideahut.quarkus.template.controller.test;

import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import jakarta.inject.Inject;
import jakarta.validation.constraints.NotBlank;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.StreamingOutput;
import net.ideahut.quarkus.annotation.Body;
import net.ideahut.quarkus.annotation.Public;
import net.ideahut.quarkus.bean.BeanInitialize;
import net.ideahut.quarkus.helper.ErrorHelper;
import net.ideahut.quarkus.helper.FrameworkHelper;
import net.ideahut.quarkus.helper.StringHelper;
import net.ideahut.quarkus.report.ReportHandler;
import net.ideahut.quarkus.report.ReportInput;
import net.ideahut.quarkus.report.ReportType;
import net.ideahut.quarkus.template.app.AppProperties;
import net.ideahut.quarkus.template.object.ReportData;
import net.sf.jasperreports.engine.JRParameter;
import net.sf.jasperreports.engine.JasperReport;
import net.sf.jasperreports.engine.fill.JRSwapFileVirtualizer;
import net.sf.jasperreports.engine.util.JRLoader;
import net.sf.jasperreports.engine.util.JRSwapFile;

/*
 * Contoh penggunaan ReportHandler
 */
@Path("/test/report")
class ReportController implements BeanInitialize {
	
	private final AppProperties appProperties;
	private final ReportHandler reportHandler;
	
	private byte[] template;
	private byte[] imageHeader;
	private byte[] imageDetail;
	
	
	@Inject
	ReportController(
		AppProperties appProperties,
		ReportHandler reportHandler
	) {
		this.appProperties = appProperties;
		this.reportHandler = reportHandler;
	}
	
	
	@Override
	public void afterPropertiesSet() throws Exception {
		String path = appProperties.reportPath().orElse(null);
		ErrorHelper.throwBlank(path, () -> "AppProperties.reportPath is required");
		path = FrameworkHelper.replacePath(path);
		path = StringHelper.removeEnd(path, "/");
		template = FrameworkHelper.toByteArray(FrameworkHelper.getResource(path + "/sample.jasper"));
		imageHeader = FrameworkHelper.toByteArray(FrameworkHelper.getResource(path + "/tree1.png"));
		imageDetail = FrameworkHelper.toByteArray(FrameworkHelper.getResource(path + "/tree2.png"));
	}
	
	// contoh respon resource, memuat seluruh data ke memori
	@Body // tambahkan @Body agar respon tidak diubah ke standar framework
	@Public
	@GET
	@Path("/resource")
	public Response resource(
		@NotBlank @QueryParam("name") String name
	) {
	    ReportType type = getType(name);
	    ByteArrayOutputStream outStream = new ByteArrayOutputStream();
	    export(type, outStream);
	    byte[] bytes = outStream.toByteArray();
	    return Response.ok()
	    .entity(bytes)
	    .type(type.getContentType())
	    .header(HttpHeaders.CONTENT_LENGTH, bytes.length)
	    .build();
	}
	
	// contoh respons stream, tidak memuat seluruh data ke memory
	@Body // tambahkan @Body agar respon tidak diubah ke standar framework
	@Public
	@GET
	@Path("/stream")
	public Response stream(
		@NotBlank @QueryParam("name") String name
	) {
		ReportType type = getType(name);
		StreamingOutput body = outStream -> export(type, outStream);
		return Response.ok()
		.entity(body)
		.type(type.getContentType())
		.build();
	}
	
	private ReportType getType(String name) {
		try {
			return ReportType.valueOf(name.trim().toUpperCase());
		} catch (Exception e) {
			return ReportType.PDF;
		}
	}
	
	private void export(ReportType type, OutputStream outputStream) {
		// agar report tidak disimpan di memory
		JRSwapFile swapFile = new JRSwapFile(System.getProperty("java.io.tmpdir"), 4096, 100);
		JRSwapFileVirtualizer virtualizer = new JRSwapFileVirtualizer(0, swapFile, true);
		try (BufferedOutputStream bufferedOut = new BufferedOutputStream(outputStream, 8192)) {
			JasperReport report = (JasperReport) JRLoader.loadObject(new ByteArrayInputStream(template));
			List<ReportData> datasource = new ArrayList<>();
			for (long i = 0; i < 100; i++) {
				ReportData data = new ReportData();
				data.setDescription("Deskripsi - " + System.nanoTime());
				data.setName("Name - " + System.nanoTime());
				data.setNumber(i);
				data.setUuid(UUID.randomUUID().toString());
				datasource.add(data);
			}
			ReportInput input = new ReportInput()
			.setType(type)
			.setReport(report)
			.setDatasource(datasource)
			.setParameter(JRParameter.REPORT_VIRTUALIZER, virtualizer)
			.setParameter("MAIN_TITLE", "Contoh Report")
			.setParameter("SUB_TITLE", type.name())
			.setParameter("IMAGE_HEADER", new ByteArrayInputStream(imageHeader))
			.setParameter("IMAGE_DETAIL", new ByteArrayInputStream(imageDetail));
			reportHandler.exportReport(input, bufferedOut);
		} catch (Exception e) {
			throw ErrorHelper.exception(e);
		} finally {
			// Wajib dibersihkan agar file temp di disk terhapus
			virtualizer.cleanup();
		}
	}
	
}
