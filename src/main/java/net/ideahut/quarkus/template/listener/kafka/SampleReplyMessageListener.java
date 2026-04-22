package net.ideahut.quarkus.template.listener.kafka;

import org.apache.kafka.clients.consumer.ConsumerRecord;

import net.ideahut.quarkus.sysparam.dto.SysParamDto;

public class SampleReplyMessageListener {

	public SysParamDto listen01(ConsumerRecord<String, String> data) {
		return createResponse(data.value());
	}
	
	public SysParamDto listen02(String text) {
		return createResponse(text);
	}
	
	private SysParamDto createResponse(String text) {
		String suffix = System.nanoTime() + "";
		return new SysParamDto().setSysCode("TEST").setParamCode("PARAM-" + suffix).setValue(text);
	}
	
}
