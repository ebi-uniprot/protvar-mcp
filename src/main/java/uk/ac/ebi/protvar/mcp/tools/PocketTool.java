package uk.ac.ebi.protvar.mcp.tools;

import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
public class PocketTool {

    private final RestClient client;

    public PocketTool(RestClient client) {
        this.client = client;
    }

    @Tool(description = """
            Retrieve predicted ligand-binding pockets near a protein residue.
            Predictions are based on AlphaFold DB monomeric structures analysed
            using AutoSite pocket detection.
            """)
    public String getPockets(

            @ToolParam(description = "UniProt protein accession (e.g. Q9NUW8)")
            String accession,

            @ToolParam(description = "Protein residue position using UniProt numbering")
            Integer resid) {

        return client.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/prediction/pocket/{a}/{r}")
                        .build(accession, resid))
                .retrieve()
                .body(String.class);
    }
}