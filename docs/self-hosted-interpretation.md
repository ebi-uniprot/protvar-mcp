# Self-Hosted Variant Interpretation with Small Language Models

## Context

The ProtVar MCP server exposes structural and functional data for protein variants — FoldX stability predictions (ΔΔG), ligand-binding pocket detection, and protein–protein interaction interfaces. A large language model (LLM) like Claude can consume this output and produce rich biological interpretation.

However, using a hosted LLM requires an external account and sends data to a third-party API. This document describes how to achieve meaningful variant interpretation entirely on your own infrastructure using small, self-hostable language models.

---

## What ProtVar MCP Provides

For a given variant (e.g. `P04637 R175H`), the MCP tools return:

| Tool | Output |
|---|---|
| `getFoldx` | ΔΔG (kcal/mol), wild-type/mutant AA, pLDDT confidence |
| `getPockets` | Pocket score, buriedness, radius of gyration, residues |
| `getInteractions` | Interaction partners, interface residues, pDockQ confidence |
| `getFunction` | UniProt features, plus pathogenicity scores (Conservation, EVE, ESM1b, AlphaMissense) where available |
| `getStructure` | PDB structure mappings, chain/position |
| `getPopulation` | Co-located variants from population databases (gnomAD, dbSNP, ClinVar, COSMIC) |
| `mapVariant` / `mapVariants` | Full coordinate mapping and annotations for one or more variants |

See [TOOLS.md](TOOLS.md) for the complete reference. The structural and functional outputs (FoldX, pockets, interactions) are the most relevant for the self-hosted interpretation pipeline described here, as they produce structured numerical data well-suited to rule-based pre-processing before an LLM sees them.

---

## Why Generic BERT is Not the Right Fit

A generic BERT model is an encoder-only architecture trained on general text. It:

- Cannot generate explanatory text (no decoder)
- Has no protein or biomedical domain knowledge
- Cannot reason over numerical inputs like ΔΔG values

It is not suitable for this task without significant fine-tuning and architectural changes.

---

## Recommended Self-Hostable Models

### For Variant Effect Scoring (from sequence)

These models operate directly on protein sequences and do not require ProtVar output:

| Model | Size | Description |
|---|---|---|
| **ESM-2** (Meta) | 150M – 3B | Protein language model; scores variant effects via masked token probabilities |
| **AlphaMissense** | ~500M | Trained specifically to predict missense variant pathogenicity |

### For Interpreting ProtVar Structured Output

These are small instruction-following LLMs that can take structured data as input and produce natural language interpretation:

| Model | Size | Notes |
|---|---|---|
| **Phi-3 Mini** | 3.8B | Strong reasoning per parameter; low resource requirements |
| **Llama 3.2 3B** | 3B | Good general instruction-following; quantised versions available |
| **Mistral 7B** | 7B | Strong reasoning; runs on a single consumer GPU |
| **BioMistral** | 7B | Mistral fine-tuned on biomedical literature; preferred for this domain |
| **BioGPT** | 347M | Biomedical text generation; lighter option |
| **BioMedLM** | 2.7B | Stronger biomedical reasoning; still self-hostable |

### Better BERT-Family Alternatives (if BERT architecture is required)

| Model | Notes |
|---|---|
| **PubMedBERT** | BERT fine-tuned on PubMed abstracts and full-text |
| **BioLinkBERT** | Extends PubMedBERT with document-link pretraining |

These are suitable for classification tasks (e.g. pathogenic/benign) but not for free-text interpretation.

---

## Proposed Pipeline Architecture

```
Variant Input (e.g. P04637 R175H)
        │
        ▼
  ProtVar MCP Tools
  ├── getFoldx        → ΔΔG, pLDDT
  ├── getPockets      → pocket score, buriedness, residues
  ├── getInteractions → pDockQ scores, partner proteins
  └── getFunction     → UniProt features + pathogenicity scores (EVE, ESM1b, AlphaMissense, Conservation)
        │
        ▼
  Rule-based interpretation layer
  ├── ΔΔG > 2 kcal/mol        → destabilising
  ├── ΔΔG > 5 kcal/mol        → severely destabilising
  ├── buriedness > 0.8        → deeply buried pocket (druggable)
  └── pDockQ > 0.5            → high-confidence interaction partner
        │
        ▼
  Small LLM (e.g. BioMistral 7B or Phi-3 Mini)
  Prompt: "Given the following structural findings for variant {variant},
           interpret the functional impact: {structured_summary}"
        │
        ▼
  Natural language interpretation
```

The rule-based layer is important — it converts raw numbers into categorical signals before the LLM sees them, reducing the burden on the model and improving reliability.

---

## Example: P04637 R175H (TP53)

### ProtVar MCP output (summarised)

- **ΔΔG**: +11.4 kcal/mol (severely destabilising; pLDDT 96.38)
- **Pocket 2**: score 934.8, buriedness 0.847 — residue 175 is deeply buried in a high-scoring pocket
- **Interactions**: residue 175 at the interface of 40+ predicted partners; highest confidence includes WDR5 (pDockQ 0.674), TOP1 (0.645), PSMD10/Gankyrin (0.642)

### Rule-based signals

```
stability:    SEVERELY_DESTABILISING  (ΔΔG > 10)
pocket:       DEEPLY_BURIED_HIGH_SCORE
interactions: MULTIPLE_HIGH_CONFIDENCE_PARTNERS
```

### Prompt to small LLM

```
Variant: P04637 R175H (TP53, Arg175His)
Stability: severely destabilising (ΔΔG = +11.4 kcal/mol)
Pocket: residue is deeply buried in a high-scoring ligand-binding pocket (score 934.8, buriedness 0.847)
Interactions: disrupts high-confidence interfaces with WDR5, TOP1, PSMD10, and 14-3-3 sigma

Interpret the likely functional impact of this variant.
```

---

## Limitations vs a Large LLM

| Aspect | Small LLM (7B) | Large LLM (Claude/GPT-4) |
|---|---|---|
| Structured data reasoning | Adequate with good prompting | Strong |
| Biomedical knowledge depth | Moderate (BioMistral better) | Extensive |
| Novel variant generalisation | Limited | Better |
| Self-hostable | Yes | No |
| Data privacy | Full control | Third-party |
| Cost | Infrastructure only | Per-token API cost |

The interpretation quality from a small LLM will be meaningful and biologically grounded for well-characterised variant features, though it will not match the depth of reasoning a large model produces.

---

## Summary

For fully self-hosted variant interpretation:

1. Use **ProtVar MCP** to retrieve structural evidence (ΔΔG, pockets, interactions)
2. Apply a **rule-based layer** to categorise the numerical outputs
3. Pass the categorised summary to a **small instruction-following LLM** (BioMistral 7B or Phi-3 Mini recommended)
4. Optionally supplement with **ESM-2** or **AlphaMissense** for sequence-based pathogenicity scoring

This pipeline requires no external accounts, keeps data on your infrastructure, and produces interpretations that are anchored in structural evidence rather than relying solely on what the model has seen in training data.
