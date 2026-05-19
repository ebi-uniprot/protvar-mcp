# ProtVar MCP Tools Reference

All tools are hand-written `@Tool` methods with descriptions tailored for an AI
client. Each wraps a ProtVar API call via the shared `ProtvarClient`, which
turns an empty `404` from the API into a plain `"No data available"` result
rather than an error.

Tools are grouped by `tools/` class: `MappingTool`, `BatchMappingTool`,
`AnnotationTool`, `PredictionTool`, `SearchTool`.

---

## Mapping

### `mapVariant`
Map a single genomic variant to its protein position and retrieve full annotations.

| Parameter | Type | Required | Description |
|---|---|---|---|
| `input` | string | yes | Variant in any notation — VCF (`19 1010539 . G C`), HGVS (`NC_000019.10:g.1010539G>C`), rsID (`rs123456`), or protein-level (`P04637 R175H`) |
| `assembly` | string | no | Genome assembly: `AUTO` (default), `GRCh38`, or `GRCh37` |

**Endpoint:** `GET /mapping?q={input}&assembly={assembly}`

### `mapVariants`
Map one or more variants at once. Submits a batch job and polls for results.

| Parameter | Type | Required | Description |
|---|---|---|---|
| `variants` | string | yes | One or more variants, one per line, in any supported notation |

**Flow:** `POST /input/text` → extract `inputId` → poll `GET /mapping/{inputId}` until ready

---

## Annotation

Residue-level annotation — broader than the prediction tools below.

### `getFunction`
Functional annotations for a residue: UniProt features (domains, regions, sites, PTMs), plus pathogenicity predictions/scores where available.

| Parameter | Type | Required | Description |
|---|---|---|---|
| `accession` | string | yes | UniProt protein accession |
| `position` | integer | yes | Residue position (UniProt numbering) |
| `variantAA` | string | no | Variant amino acid, 1-letter code |

**Endpoint:** `GET /function/{accession}/{position}?variantAA={variantAA}`

### `getPopulation`
Other variants co-located at the same residue — population observations and known variants (dbSNP, ClinVar, COSMIC).

| Parameter | Type | Required | Description |
|---|---|---|---|
| `accession` | string | yes | UniProt protein accession |
| `position` | integer | yes | Residue position (UniProt numbering) |
| `genomicVariant` | string | no | Genomic variant for context (e.g. `14-89993420-A-C`) |

**Endpoint:** `GET /population/{accession}/{position}?genomicVariant={genomicVariant}`

### `getStructure`
Structural context — the residue's position within experimental PDB structures.

| Parameter | Type | Required | Description |
|---|---|---|---|
| `accession` | string | yes | UniProt protein accession |
| `position` | integer | yes | Residue position (UniProt numbering) |

**Endpoint:** `GET /structure/{accession}/{position}`

---

## Predictions

ProtVar's novel structure-based predictions, exposed as focused tools.

### `getFoldx`
FoldX stability predictions (ΔΔG) for a protein variant, from AlphaFold DB structures.

| Parameter | Type | Required | Description |
|---|---|---|---|
| `accession` | string | yes | UniProt protein accession |
| `position` | integer | yes | Residue position (UniProt numbering) |
| `variantAA` | string | no | Variant amino acid, 1- or 3-letter code |

**Endpoint:** `GET /prediction/foldx/{accession}/{position}?variantAA={variantAA}`

### `getPockets`
Predicted ligand-binding pockets near a residue (AlphaFold DB structures, AutoSite detection).

| Parameter | Type | Required | Description |
|---|---|---|---|
| `accession` | string | yes | UniProt protein accession |
| `resid` | integer | yes | Residue position (UniProt numbering) |

**Endpoint:** `GET /prediction/pocket/{accession}/{resid}`

### `getInteractions`
Predicted protein–protein interaction interfaces for a residue.

| Parameter | Type | Required | Description |
|---|---|---|---|
| `accession` | string | yes | UniProt protein accession |
| `resid` | integer | yes | Residue position (UniProt numbering) |

**Endpoint:** `GET /prediction/interaction/{accession}/{resid}`

---

## Search

### `semanticSearch`
Natural-language search over protein function descriptions, by embedding similarity.

| Parameter | Type | Required | Description |
|---|---|---|---|
| `text` | string | yes | Free-text query (function, process, disease, phenotype) |
| `limit` | integer | no | Max results (default 10) |

**Endpoint:** `GET /semantic-search?text={text}&limit={limit}`

### `searchVariants`
Filter the variant set by structural/functional criteria. At least one of `accession`, `gene`, `pocket`, `interact`, `experimentalModel` or `known` must be supplied.

| Parameter | Type | Required | Description |
|---|---|---|---|
| `accession` | string | no | Scope to a UniProt accession |
| `gene` | string | no | Scope to a gene name |
| `pocket` | boolean | no | Only variants in a predicted ligand-binding pocket |
| `interact` | boolean | no | Only variants at a predicted interaction interface |
| `experimentalModel` | boolean | no | Only variants with an experimental (PDB) model |
| `known` | boolean | no | Only known variants (dbSNP/ClinVar/COSMIC) |
| `page` | integer | no | Page number, 1-based (default 1) |

**Endpoint:** `POST /mapping` with a JSON `MappingRequest` body (`ids`, filter flags, `page`)

---

## Adding a tool

1. Add a `@Tool`-annotated method to an existing `tools/` class, or create a new
   `@Component` class for a new group.
2. Call the ProtVar API through the injected `ProtvarClient`.
3. If it is a new class, add it as a parameter to
   `ProtvarMcpApplication.protvarTools()` and pass it to `.toolObjects(...)`.
