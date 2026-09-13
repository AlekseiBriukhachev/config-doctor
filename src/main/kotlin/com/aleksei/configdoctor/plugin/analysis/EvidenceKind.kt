package com.aleksei.configdoctor.plugin.analysis

/**
 * Evidence tiers from AGENTS.md section 15 ("Evidence Hierarchy"). Only
 * the tier actually implemented so far has a value - the others are
 * listed in a comment for traceability, not implemented speculatively.
 *
 * AGENTS.md section 15 full hierarchy (highest confidence first):
 *   1. Explicit Spring Boot configuration metadata.
 *   2. Strong relationship with another configuration property in the
 *      same project.               <- implemented (this enum value)
 *   3. Profile override relationship.
 *   4. Structural similarity to a known property.
 *   5. Other deterministic project information.
 */
enum class EvidenceKind {
    /**
     * Tier 2: an actual, currently-used property elsewhere in this
     * project was found that the candidate path is structurally derived
     * from via a deterministic transformation (not mere text similarity).
     */
    STRONG_PROJECT_RELATIONSHIP,

    /**
     * Tier 3: the profile property is a different path from the base
     * property it appears to resemble, so it does not actually override it.
     */
    PROFILE_OVERRIDE_RELATIONSHIP
}
