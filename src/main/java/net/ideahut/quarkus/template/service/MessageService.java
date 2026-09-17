package net.ideahut.quarkus.template.service;

import java.util.List;

import com.fasterxml.jackson.databind.JsonNode;

import net.ideahut.quarkus.message.MessageHandler;
import net.ideahut.quarkus.object.Option;

public interface MessageService extends MessageHandler {
	
	List<Option> getActiveLanguages();
	String getDefaultLanguage();
	
	JsonNode getResource(String type);
	
}
