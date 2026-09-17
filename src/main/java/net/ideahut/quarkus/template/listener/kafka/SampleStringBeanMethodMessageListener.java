package net.ideahut.quarkus.template.listener.kafka;

import org.apache.kafka.clients.consumer.ConsumerRecord;

import jakarta.inject.Named;
import jakarta.inject.Singleton;
import lombok.extern.slf4j.Slf4j;
import net.ideahut.quarkus.kafka.KafkaMessageListener;

@Slf4j
@Singleton
@Named("sampleStringBeanMethodMessageListener")
public class SampleStringBeanMethodMessageListener {

	public KafkaMessageListener<String, String, String> listener() {
		return (ConsumerRecord<String, String> data) -> {
			log.info("SampleStringBeanMethodMessageListener - listener: \n{}", data);
			return null;
		};
	}
	
	public void message(ConsumerRecord<String, String> data) {
		log.info("SampleStringBeanMethodMessageListener - message: \n{}", data);
	}

}
