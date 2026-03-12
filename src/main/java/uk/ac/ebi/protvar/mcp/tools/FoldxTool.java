package uk.ac.ebi.protvar.mcp.tools;

import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.Optional;

@Component
public class FoldxTool {

    private final RestClient client;

    public FoldxTool(RestClient client) {
        this.client = client;
    }

    @Tool(description = "Retrieve FoldX predictions for a protein variant")
    public String getFoldx(
            String accession,
            Integer position,
            @ToolParam(required = false, description = "Optional 1- or 3-letter amino acid variant") String variantAA) {

        return client.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/prediction/foldx/{a}/{p}")
                        .queryParamIfPresent("variantAA", Optional.ofNullable(variantAA)
                                .filter(s -> !s.isBlank()))
                        .build(accession, position))
                .retrieve()
                .body(String.class);
    }

}