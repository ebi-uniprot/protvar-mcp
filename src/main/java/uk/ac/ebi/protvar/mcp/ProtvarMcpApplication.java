package uk.ac.ebi.protvar.mcp;

import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.ai.tool.method.MethodToolCallbackProvider;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import uk.ac.ebi.protvar.mcp.tools.FoldxTool;

@SpringBootApplication
public class ProtvarMcpApplication {

	public static void main(String[] args) {
		SpringApplication.run(ProtvarMcpApplication.class, args);
	}

	@Bean
	public ToolCallbackProvider protvarTools(FoldxTool foldxTool) {
		return MethodToolCallbackProvider.builder()
				.toolObjects(foldxTool)
				.build();
	}
}
