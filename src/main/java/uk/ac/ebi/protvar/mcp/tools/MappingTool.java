package uk.ac.ebi.protvar.mcp.tools;

import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
public class MappingTool {

    private final ProtvarClient client;

    public MappingTool(ProtvarClient client) {
        this.client = client;
    }

    @Tool(description = """
            Map a genomic variant to its protein position and retrieve full annotations.
            Accepts a variant in any supported notation, e.g.:
              - VCF-style:       19 1010539 . G C
              - HGVS genomic:    NC_000019.10:g.1010539G>C
              - ID-based:        rs123456
              - Protein-level:   P04637 R175H
            Returns coordinate mappings, functional annotations, structural context,
            population frequencies and pathogenicity predictions for the variant.
            """)
    public String mapVariant(
            @ToolParam(description = "Variant input in any supported notation (VCF, HGVS, rsID, or protein-level, e.g. 'P04637 R175H')")
            String input,

            @ToolParam(required = false, description = "Genome assembly: AUTO (default), GRCh38, or GRCh37")
            String assembly) {

        return client.get(uriBuilder -> uriBuilder
                .path("/mapping")
                .queryParam("q", input)
                .queryParamIfPresent("assembly",
                        Optional.ofNullable(assembly).filter(s -> !s.isBlank()))
                .build());
    }
}
