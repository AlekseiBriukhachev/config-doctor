package com.aleksei.configdoctor.plugin.analysis

import com.aleksei.configdoctor.plugin.model.ConfigProperty

/**
 * A single suspicious-path finding, per AGENTS.md section 14: "The agent
 * must document WHAT evidence is being used to make this determination."
 *
 * [evidence] is a human-readable explanation, not just an internal flag -
 * it is written so it can be reused verbatim (or near-verbatim) as an
 * inspection message later (section 16 requires the message to explain
 * the actual path, the related expected path, and why the relationship is
 * suspicious).
 */
data class SuspiciousPathFinding(
    val actual: ConfigProperty,
    val relatedExpected: ConfigProperty,
    val evidenceKind: EvidenceKind,
    val evidence: String
)
