package uk.ac.ebi.protvar.mcp.tools;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;

@Component
public class BatchMappingTool {

    private static final Logger log = LoggerFactory.getLogger(BatchMappingTool.class);
    private static final int MAX_POLLS = 10;
    private static final long POLL_INTERVAL_MS = 2000;

    private final ProtvarClient client;
    private final ObjectMapper objectMapper;

    public BatchMappingTool(ProtvarClient client, ObjectMapper objectMapper) {
        this.client = client;
        this.objectMapper = objectMapper;
    }

    @Tool(description = """
            Map one or more genomic variants to their protein positions and retrieve full annotations.
            Accepts multiple variants, one per line. Each variant can be in any supported notation:
              - VCF-style:       19 1010539 . G C
              - HGVS genomic:    NC_000019.10:g.1010539G>C
              - ID-based:        rs123456
              - Protein-level:   P04637 R175H
            Returns coordinate mappings, functional annotations, structural context,
            population frequencies and pathogenicity predictions for all variants.
            Use this when analysing multiple variants at once.
            """)
    public String mapVariants(
            @ToolParam(description = "One or more variants, one per line")
            String variants) {

        // Step 1: submit the input
        String inputResponse = client.post("/input/text", MediaType.TEXT_PLAIN, variants);
        if (inputResponse.startsWith("API error") || inputResponse.startsWith("No data")) {
            return "Failed to submit variants: " + inputResponse;
        }

        String inputId;
        try {
            JsonNode node = objectMapper.readTree(inputResponse);
            inputId = node.path("inputId").asText();
            if (inputId.isBlank()) {
                return "Failed to get inputId from response: " + inputResponse;
            }
        } catch (Exception e) {
            return "Failed to parse inputId: " + e.getMessage();
        }

        log.debug("Submitted batch input, inputId={}", inputId);

        // Step 2: poll for results
        for (int attempt = 1; attempt <= MAX_POLLS; attempt++) {
            String result = client.get("/mapping/{inputId}", inputId);

            if (result.startsWith("API error") || result.startsWith("No data")) {
                return "Error fetching results: " + result;
            }

            if (hasContent(result)) {
                return result;
            }

            log.debug("Results not ready yet (attempt {}/{}), waiting...", attempt, MAX_POLLS);
            try {
                Thread.sleep(POLL_INTERVAL_MS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }

        return "Timed out waiting for results (inputId=" + inputId + "). Try fetching directly with inputId.";
    }

    private boolean hasContent(String result) {
        if (result == null || result.isBlank()) return false;
        try {
            JsonNode node = objectMapper.readTree(result);
            JsonNode inputs = node.path("content").path("inputs");
            return inputs.isArray() && !inputs.isEmpty();
        } catch (Exception e) {
            return false;
        }
    }
}
