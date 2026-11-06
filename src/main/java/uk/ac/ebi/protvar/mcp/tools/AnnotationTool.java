package uk.ac.ebi.protvar.mcp.tools;

import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

import java.util.Optional;

/**
 * Residue-level annotation tools. These return the broader annotation set for a
 * protein position — function, co-located population variants, and structural
 * context — which encompasses the focused {@link PredictionTool} data and more.
 */
@Component
public class AnnotationTool {

    private final ProtvarClient client;

    public AnnotationTool(ProtvarClient client) {
        this.client = client;
    }

    @Tool(description = """
            Retrieve UniProt functional annotations for a protein residue: features
            (domains, regions, sites, post-translational modifications) overlapping
            the position, and the protein's comments (function, catalytic activity,
            subcellular location, interactions). For predictions and scores at the
            position use getPredictions.
            """)
    public String getFunction(
            @ToolParam(description = "UniProt protein accession (e.g. Q9NUW8)")
            String accession,

            @ToolParam(description = "Protein residue position using UniProt numbering")
            Integer position) {

        return client.get("/function/{a}/{p}", accession, position);
    }

    @Tool(description = """
            Retrieve other variants co-located at the same protein residue:
            population observations and known variants (dbSNP, ClinVar, COSMIC)
            recorded at this amino acid position.
            """)
    public String getPopulation(
            @ToolParam(description = "UniProt protein accession (e.g. Q9NUW8)")
            String accession,

            @ToolParam(description = "Protein residue position using UniProt numbering")
            Integer position,

            @ToolParam(required = false, description = "Optional genomic variant for context (e.g. 14-89993420-A-C)")
            String genomicVariant) {

        return client.get(uriBuilder -> uriBuilder
                .path("/population/{a}/{p}")
                .queryParamIfPresent("genomicVariant",
                        Optional.ofNullable(genomicVariant).filter(s -> !s.isBlank()))
                .build(accession, position));
    }

    @Tool(description = """
            Retrieve the structural context for a protein residue: its position
            within experimental PDB structures (entries and chains) covering the
            amino acid position.
            """)
    public String getStructure(
            @ToolParam(description = "UniProt protein accession (e.g. Q9NUW8)")
            String accession,

            @ToolParam(description = "Protein residue position using UniProt numbering")
            Integer position) {

        return client.get("/structure/{a}/{p}", accession, position);
    }
}
