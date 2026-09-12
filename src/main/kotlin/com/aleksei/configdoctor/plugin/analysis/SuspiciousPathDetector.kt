package com.aleksei.configdoctor.plugin.analysis

import com.aleksei.configdoctor.plugin.model.ConfigProperty
import com.aleksei.configdoctor.plugin.model.PropertyPath

/**
 * Stage 5 (AGENTS.md section 14): a first, deliberately narrow detection
 * prototype, proving the core problem is actually detectable before any
 * IntelliJ inspection framework is involved. There is no LocalInspectionTool
 * here, no plugin.xml wiring, no UI - just a pure function over already-
 * extracted properties (Stage 3 + Stage 4), with real automated tests.
 *
 * ## What this detects
 *
 * A property path that contains two identical, immediately adjacent
 * segments (e.g. "spring.datasource.datasource.url"), where removing one
 * of the duplicated segments produces a path that is an ACTUAL, currently
 * used property elsewhere in the project (e.g. "spring.datasource.url").
 *
 * This is evidence tier 2 from AGENTS.md section 15 ("Strong relationship
 * with another configuration property in the same project"): the
 * "expected" path isn't invented or guessed - it must be a real property
 * that already exists, found via a deterministic transformation, not
 * vague textual similarity. See EvidenceKind for the full tier list.
 *
 * ## What this deliberately does NOT detect (scope limits)
 *
 * - An extra, non-duplicate segment inserted anywhere else (e.g.
 *   "app.wrapper.feature.enabled" vs. the intended "app.feature.enabled")
 *   is NOT flagged. That is a different failure shape - detecting it
 *   would require genuine structural-similarity scoring (tier 4), which
 *   is not implemented yet, precisely to avoid the vague "this looks
 *   unusual" style of warning AGENTS.md section 15 prohibits.
 * - A duplicated segment where no matching real property exists anywhere
 *   in the project is NOT flagged - there is nothing to point to as "the
 *   expected path", so no finding is reported rather than guessing one.
 */
object SuspiciousPathDetector {

    fun findSuspiciousDuplicatedSegments(properties: Collection<ConfigProperty>): List<SuspiciousPathFinding> {
        val byPath: Map<PropertyPath, ConfigProperty> = properties.associateBy { it.path }
        val findings = mutableListOf<SuspiciousPathFinding>()

        for (candidate in properties) {
            val segments = candidate.path.segments
            for (i in 0 until segments.size - 1) {
                if (segments[i] != segments[i + 1]) continue

                val collapsedSegments = segments.toMutableList().also { it.removeAt(i) }
                if (collapsedSegments.isEmpty()) continue
                val collapsedPath = PropertyPath(collapsedSegments)
                if (collapsedPath == candidate.path) continue

                val match = byPath[collapsedPath] ?: continue

                findings += SuspiciousPathFinding(
                    actual = candidate,
                    relatedExpected = match,
                    evidenceKind = EvidenceKind.STRONG_PROJECT_RELATIONSHIP,
                    evidence = buildString {
                        append("Removing the duplicated adjacent segment '${segments[i]}' from ")
                        append("'${candidate.path}' yields '${match.path}', which is an existing ")
                        append("property already used elsewhere in this project")
                        append(if (match.profile != null) " (profile: ${match.profile})." else " (default profile).")
                    }
                )
            }
        }

        return findings
    }
}
