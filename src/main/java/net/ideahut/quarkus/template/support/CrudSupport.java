package net.ideahut.quarkus.template.support;

import java.util.HashMap;
import java.util.Map;

import net.ideahut.quarkus.crud.CrudSpecificValueGetter;
import net.ideahut.quarkus.module.FrameworkModules;

public class CrudSupport {

private CrudSupport() {}
	
	/*
	 * SPECIFIC VALUE GETTERS
	 */
	public static Map<String, CrudSpecificValueGetter> getSpecificValueGetters() {
		Map<String, CrudSpecificValueGetter> specifics = new HashMap<>();
		specifics.putAll(FrameworkModules.getCrudSpesificValueGetters());
		return specifics;
	}
	
}
