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

    @Tool(description = """
            Retrieve FoldX stability predictions (ΔΔG values) for a protein variant.
            Uses AlphaFold DB structures and FoldX calculations to estimate the energetic
            impact of amino acid substitutions on protein stability.
            """)
    public String getFoldx(
            @ToolParam(description = "UniProt protein accession (e.g. Q9NUW8)")
            String accession,

            @ToolParam(description = "Protein residue position using UniProt numbering (e.g. 493)")
            Integer position,

            @ToolParam(
                    required = false,
                    description = "Optional amino acid variant at this position (1-letter or 3-letter code, e.g. R or Arg)"
            )
            String variantAA) {

        return client.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/prediction/foldx/{a}/{p}")
                        .queryParamIfPresent("variantAA",
                                Optional.ofNullable(variantAA).filter(s -> !s.isBlank()))
                        .build(accession, position))
                .retrieve()
                .body(String.class);
    }
}