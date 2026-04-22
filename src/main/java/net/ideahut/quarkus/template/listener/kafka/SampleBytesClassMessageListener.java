package net.ideahut.quarkus.template.listener.kafka;

import org.apache.kafka.clients.consumer.ConsumerRecord;

import lombok.extern.slf4j.Slf4j;
import net.ideahut.quarkus.kafka.KafkaMessageListener;

@Slf4j
public class SampleBytesClassMessageListener {
	
	public static KafkaMessageListener<String, byte[], byte[]> staticListener() {
		return (ConsumerRecord<String, byte[]> value) -> {
			log.info("SampleBytesClassMessageListener - staticListener: \n{}", value);
			return null;
		};
	}
	
	public static void staticMessage(ConsumerRecord<String, byte[]> data) {
		log.info("SampleBytesClassMessageListener - staticMessage: \n{}", data);
	}
	
	public KafkaMessageListener<String, byte[], byte[]> nonStaticListener() {
		return (ConsumerRecord<String, byte[]> data) -> {
			log.info("SampleBytesClassMessageListener - nonStaticListener: \n{}", data);
			return null;
		};
	}
	
	public void nonStaticMessage(ConsumerRecord<String, byte[]> data) {
		log.info("SampleBytesClassMessageListener - nonStaticMessage: \n{}", data);
	}
	
}
