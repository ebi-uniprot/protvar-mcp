package uk.ac.ebi.protvar.mcp.tools;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;

/**
 * Discovery tools: free-text semantic search, and structured filter search over
 * the variant set.
 */
@Component
public class SearchTool {

    private final ProtvarClient client;
    private final ObjectMapper objectMapper;

    public SearchTool(ProtvarClient client, ObjectMapper objectMapper) {
        this.client = client;
        this.objectMapper = objectMapper;
    }

    @Tool(description = """
            Natural-language search over ProtVar protein function descriptions.
            Provide a free-text query — a function, biological process, disease or
            phenotype — and receive a ranked list of relevant proteins and residue
            positions, found by semantic embedding similarity rather than keyword
            matching.
            """)
    public String semanticSearch(
            @ToolParam(description = "Free-text query, e.g. 'DNA mismatch repair' or 'zinc-binding transcription factor'")
            String text,

            @ToolParam(required = false, description = "Maximum number of results to return (default 10)")
            Integer limit) {

        return client.get(uriBuilder -> {
            uriBuilder.path("/semantic-search").queryParam("text", text);
            if (limit != null) {
                uriBuilder.queryParam("limit", limit);
            }
            return uriBuilder.build();
        });
    }

    @Tool(description = """
            Search and filter ProtVar variants by structural and functional criteria.
            Scope the search to a protein accession or a gene, and optionally restrict
            to variants in predicted ligand-binding pockets, protein–protein
            interaction interfaces, experimental (PDB) structural models, or to known
            variants only. At least one of accession, gene, pocket, interact,
            experimentalModel or known must be supplied. Results are paged.
            """)
    public String searchVariants(
            @ToolParam(required = false, description = "Scope to a UniProt accession, e.g. P04637")
            String accession,

            @ToolParam(required = false, description = "Scope to a gene name, e.g. TP53")
            String gene,

            @ToolParam(required = false, description = "Only variants located in a predicted ligand-binding pocket")
            Boolean pocket,

            @ToolParam(required = false, description = "Only variants at a predicted protein–protein interaction interface")
            Boolean interact,

            @ToolParam(required = false, description = "Only variants covered by an experimental (PDB) structural model")
            Boolean experimentalModel,

            @ToolParam(required = false, description = "Only known variants (dbSNP/ClinVar/COSMIC); default also includes potential variants")
            Boolean known,

            @ToolParam(required = false, description = "Page number, 1-based (default 1)")
            Integer page) {

        ObjectNode body = objectMapper.createObjectNode();

        if (accession != null && !accession.isBlank()) {
            addIdentifier(body, "UNIPROT", accession.trim());
        } else if (gene != null && !gene.isBlank()) {
            addIdentifier(body, "GENE", gene.trim());
        }

        if (Boolean.TRUE.equals(pocket)) body.put("pocket", true);
        if (Boolean.TRUE.equals(interact)) body.put("interact", true);
        if (Boolean.TRUE.equals(experimentalModel)) body.put("experimentalModel", true);
        if (Boolean.TRUE.equals(known)) body.put("known", true);
        body.put("page", page != null && page > 0 ? page : 1);

        String json;
        try {
            json = objectMapper.writeValueAsString(body);
        } catch (Exception e) {
            return "Failed to build search request: " + e.getMessage();
        }

        return client.post("/mapping", MediaType.APPLICATION_JSON, json);
    }

    private void addIdentifier(ObjectNode body, String type, String value) {
        ArrayNode ids = body.putArray("ids");
        ObjectNode id = ids.addObject();
        id.put("type", type);
        id.put("value", value);
    }
}
