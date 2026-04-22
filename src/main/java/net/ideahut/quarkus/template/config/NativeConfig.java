package net.ideahut.quarkus.template.config;

import java.io.File;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Set;

import org.apache.commons.io.FileUtils;

import lombok.extern.slf4j.Slf4j;
import net.ideahut.quarkus.helper.NativeImageHelper;
import net.ideahut.quarkus.helper.ObjectHelper;
import net.ideahut.quarkus.module.ModuleApi;
import net.ideahut.quarkus.object.StringSet;
import net.ideahut.quarkus.serializer.DataMapperBinarySerializer;
import net.ideahut.quarkus.serializer.HessianBinarySerializer;
import net.ideahut.quarkus.serializer.JdkBinarySerializer;
import net.ideahut.quarkus.template.Application;

@Slf4j
public class NativeConfig {
	NativeConfig() {}

	private static final File serializationFile = new File("serialization.tmp");
	private static final File binaryFile = new File("src/main/resources/serialization.bin");
	private static final File metadataFile = new File("src/main/resources/META-INF/native-image/reachability-metadata.json");
	//private static final File metadataFile = new File("src/main/resources/META-INF/native-image/"); //-
	
	public static void registerToNativeImageAgent() {
		ObjectHelper.runIf(
			NativeImageHelper.isAgentEnabled(), 
			() -> NativeImageHelper.registerMetadata(collectClasses(), serializationFile)
		);
	}
	
	private static Collection<Class<?>> collectClasses() {
		StringSet names = new StringSet(NativeImageHelper.Module.allModuleClassNames());
		names.addAll(NativeImageHelper.Module.getJsonWebTokenClassNames());
		names.addAll(NativeImageHelper.allClassNameInPackage(
			Application.Package.APPLICATION + ".app",
			Application.Package.APPLICATION + ".controller",
			Application.Package.APPLICATION + ".interceptor",
			Application.Package.APPLICATION + ".job",
			Application.Package.APPLICATION + ".listener",
			Application.Package.APPLICATION + ".object",
			Application.Package.APPLICATION + ".properties",
			Application.Package.APPLICATION + ".repo",
			Application.Package.APPLICATION + ".service"
		));
		names.addAll(Arrays.asList(
			"org.hibernate.community.dialect.CommunityDialectSelector",
			"org.hibernate.community.dialect.CommunityDialectResolver"
		));
		
		Set<Class<?>> classes = new LinkedHashSet<>(NativeImageHelper.convertToClasses(names));
		NativeImageHelper.addToClasses(classes, ModuleApi.getDefaultProcessors().toArray(new Class<?>[0]));
		NativeImageHelper.addToClasses(classes, 
			DataMapperBinarySerializer.class,
			JdkBinarySerializer.class,
			HessianBinarySerializer.class
			//KryoBinarySerializer.class,
			//ForyBinarySerializer.class
		);
		return classes;
	}
	
	public static void main(String... args) {
		NativeImageHelper.mergeSerializationToMetadata(
			serializationFile, 
			metadataFile,
			(String type) -> type.indexOf("$HibernateAccessOptimizer$") != -1,
			(String type) -> type.indexOf("$HibernateInstantiator$") != -1
		);
		NativeImageHelper.excludeResourceFromMetadata(
			metadataFile, 
			"META-INF/services/org.hibernate.bytecode.spi.BytecodeProvider"::equals
		);
		NativeImageHelper.beautifyMetadata(metadataFile);
		try {
			FileUtils.copyFile(serializationFile, binaryFile);
		} catch (Exception e) {
			log.error("Copy", e);
		}
	}
	
}
