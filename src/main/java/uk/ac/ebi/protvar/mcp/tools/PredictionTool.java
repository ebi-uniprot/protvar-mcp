package uk.ac.ebi.protvar.mcp.tools;

import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
public class PredictionTool {

    private final ProtvarClient client;

    public PredictionTool(ProtvarClient client) {
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

            @ToolParam(required = false, description = "Optional amino acid variant at this position (1-letter or 3-letter code, e.g. R or Arg)")
            String variantAA) {

        return client.get(uriBuilder -> uriBuilder
                .path("/prediction/foldx/{a}/{p}")
                .queryParamIfPresent("variantAA",
                        Optional.ofNullable(variantAA).filter(s -> !s.isBlank()))
                .build(accession, position));
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

        return client.get("/prediction/pocket/{a}/{r}", accession, resid);
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

        return client.get("/prediction/interaction/{a}/{r}", accession, resid);
    }
}
