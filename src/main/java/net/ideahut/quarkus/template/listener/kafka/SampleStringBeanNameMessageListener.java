package net.ideahut.quarkus.template.listener.kafka;

import org.apache.kafka.clients.consumer.ConsumerRecord;

import jakarta.inject.Named;
import jakarta.inject.Singleton;
import lombok.extern.slf4j.Slf4j;
import net.ideahut.quarkus.kafka.KafkaMessageListener;

@Slf4j
@Singleton
@Named("sampleStringBeanNameMessageListener")
class SampleStringBeanNameMessageListener implements KafkaMessageListener<String, String, String> {

	@Override
	public String onMessage(ConsumerRecord<String, String> data) {
		log.info("SampleBeanNameMessageListener - BeanName: \n{}", data);
		return null;
	}

}
