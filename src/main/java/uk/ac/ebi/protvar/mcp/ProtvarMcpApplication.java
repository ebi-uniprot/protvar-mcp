package uk.ac.ebi.protvar.mcp;

import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.ai.tool.method.MethodToolCallbackProvider;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import uk.ac.ebi.protvar.mcp.tools.AnnotationTool;
import uk.ac.ebi.protvar.mcp.tools.BatchMappingTool;
import uk.ac.ebi.protvar.mcp.tools.MappingTool;
import uk.ac.ebi.protvar.mcp.tools.PredictionTool;
import uk.ac.ebi.protvar.mcp.tools.SearchTool;

@SpringBootApplication
public class ProtvarMcpApplication {

	public static void main(String[] args) {
		SpringApplication.run(ProtvarMcpApplication.class, args);
	}

	@Bean
	public ToolCallbackProvider protvarTools(MappingTool mappingTool,
											 BatchMappingTool batchMappingTool,
											 AnnotationTool annotationTool,
											 PredictionTool predictionTool,
											 SearchTool searchTool) {
		return MethodToolCallbackProvider.builder()
				.toolObjects(mappingTool, batchMappingTool, annotationTool, predictionTool, searchTool)
				.build();
	}
}
