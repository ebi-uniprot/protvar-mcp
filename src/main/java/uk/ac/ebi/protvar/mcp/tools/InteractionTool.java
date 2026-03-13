package uk.ac.ebi.protvar.mcp.tools;

import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
public class InteractionTool {

    private final RestClient client;

    public InteractionTool(RestClient client) {
        this.client = client;
    }

    @Tool(description = """
            Retrieve predicted protein–protein interaction interfaces for a protein residue.
            These predictions identify structural interaction partners and confidence scores
            derived from structural models.
            """)
    public String getInteractions(

            @ToolParam(description = "UniProt protein accession (e.g. Q9NUW8)")
            String accession,

            @ToolParam(description = "Protein residue position using UniProt numbering")
            Integer resid) {

        return client.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/prediction/interaction/{a}/{r}")
                        .build(accession, resid))
                .retrieve()
                .body(String.class);
    }
}