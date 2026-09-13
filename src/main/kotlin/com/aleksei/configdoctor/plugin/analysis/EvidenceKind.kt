package com.aleksei.configdoctor.plugin.analysis

/**
 * Type of evidence used when a property path is considered suspicious.
 *
 * The identifier describes the actual proof the detector had for a warning,
 * rather than a vague heuristic label.
 */
enum class EvidenceKind {
    /**
     * The candidate path collapses into an existing project property after
     * removing one nested segment.
     */
    STRONG_PROJECT_RELATIONSHIP,

    /**
     * The candidate property is not a valid override for the base property
     * because it belongs to a different leaf key under the same parent path.
     */
    PROFILE_OVERRIDE_RELATIONSHIP
}
