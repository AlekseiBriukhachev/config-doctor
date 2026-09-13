package com.aleksei.configdoctor.plugin.analysis

import com.aleksei.configdoctor.plugin.model.ConfigProperty

/**
 * Represents one concrete suspicious configuration finding.
 *
 * The `actual` property is the YAML key that looks wrong, while `relatedExpected`
 * is the nearby property it appears to be structurally related to. The
 * `evidence` text explains why the relationship is considered suspicious and can
 * be reused in the inspection message.
 */
data class SuspiciousPathFinding(
    val actual: ConfigProperty,
    val relatedExpected: ConfigProperty,
    val evidenceKind: EvidenceKind,
    val evidence: String
)
