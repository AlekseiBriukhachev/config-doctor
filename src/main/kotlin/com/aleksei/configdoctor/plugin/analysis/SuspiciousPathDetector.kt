package com.aleksei.configdoctor.plugin.analysis

import com.aleksei.configdoctor.plugin.model.ConfigProperty
import com.aleksei.configdoctor.plugin.model.PropertyPath

/**
 * Stage 5 (AGENTS.md section 14): a detection prototype proving that a
 * structurally shifted YAML path can be identified from project-local evidence
 * before any IntelliJ inspection framework is involved. There is no
 * LocalInspectionTool here, no plugin.xml wiring, no UI - just a pure function
 * over already-extracted properties (Stage 3 + Stage 4), with automated tests.
 *
 * ## What this detects
 *
 * A property path that collapses to a real, currently-used property elsewhere in
 * the project when one nested segment is removed. This covers both the original
 * duplicate-adjacent case (e.g. "spring.datasource.datasource.url" ->
 * "spring.datasource.url") and the shifted-wrapper case (e.g.
 * "spring.application.name" -> "spring.name").
 *
 * This is evidence tier 2 from AGENTS.md section 15 ("Strong relationship with
 * another configuration property in the same project"): the "expected" path is
 * not invented or guessed - it must exist in the project, found via a
 * deterministic transformation rather than vague textual similarity.
 *
 * ## What this deliberately does NOT detect
 *
 * A path with no matching real project property after removing a single segment
 * is not flagged: there is no concrete expected path to compare against, so no
 * warning is reported rather than guessing.
 */
object SuspiciousPathDetector {

    fun findSuspiciousPaths(properties: Collection<ConfigProperty>): List<SuspiciousPathFinding> {
        val byPath: Map<PropertyPath, ConfigProperty> = properties.associateBy { it.path }
        val findings = mutableListOf<SuspiciousPathFinding>()

        for (candidate in properties) {
            findings += findCollapsedSegmentMatches(candidate, byPath)
            if (candidate.profile != null) {
                findings += findProfileOverrideMismatch(candidate, byPath)
            }
        }

        return findings.distinctBy { it.actual.path to it.relatedExpected.path to it.evidenceKind }
    }

    private fun findCollapsedSegmentMatches(
        candidate: ConfigProperty,
        byPath: Map<PropertyPath, ConfigProperty>
    ): List<SuspiciousPathFinding> {
        val segments = candidate.path.segments
        val findings = mutableListOf<SuspiciousPathFinding>()

        for (i in 0 until segments.size - 1) {
            val collapsedSegments = segments.toMutableList().also { it.removeAt(i) }
            if (collapsedSegments.isEmpty()) continue
            val collapsedPath = PropertyPath(collapsedSegments)
            if (collapsedPath == candidate.path) continue

            val match = byPath[collapsedPath] ?: continue
            val removed = segments[i]
            val isDuplicate = i < segments.size - 1 && segments[i] == segments[i + 1]

            findings += SuspiciousPathFinding(
                actual = candidate,
                relatedExpected = match,
                evidenceKind = EvidenceKind.STRONG_PROJECT_RELATIONSHIP,
                evidence = buildString {
                    if (isDuplicate) {
                        append("Removing the duplicated adjacent segment '${removed}' from ")
                    } else {
                        append("Removing the misplaced segment '${removed}' from ")
                    }
                    append("'${candidate.path}' yields '${match.path}', which is an existing ")
                    append("property already used elsewhere in this project")
                    append(if (match.profile != null) " (profile: ${match.profile})." else " (default profile).")
                }
            )
        }

        return findings
    }

    private fun findProfileOverrideMismatch(
        candidate: ConfigProperty,
        byPath: Map<PropertyPath, ConfigProperty>
    ): List<SuspiciousPathFinding> {
        val candidateSegments = candidate.path.segments
        if (candidateSegments.size < 2) return emptyList()

        val candidateParentPath = PropertyPath(candidateSegments.dropLast(1))
        val candidateLeaf = singularize(candidateSegments.last())

        val baseMatches = byPath.values.filter { property ->
            property.profile == null &&
                property.path.segments.size >= 2 &&
                PropertyPath(property.path.segments.dropLast(1)) == candidateParentPath &&
                singularize(property.path.segments.last()) == candidateLeaf &&
                property.path != candidate.path
        }

        return baseMatches.map { match ->
            SuspiciousPathFinding(
                actual = candidate,
                relatedExpected = match,
                evidenceKind = EvidenceKind.PROFILE_OVERRIDE_RELATIONSHIP,
                evidence = "The profile property '${candidate.path}' does not override '${match.path}' because it belongs to the same parent path but is a different leaf key."
            )
        }
    }

    private fun singularize(segment: String): String {
        var value = segment.lowercase()
        if (value.endsWith("ies") && value.length > 3) value = value.substring(0, value.length - 3) + "y"
        else if (value.endsWith("sses") && value.length > 4) value = value.substring(0, value.length - 2)
        else if (value.endsWith("es") && value.length > 3) value = value.substring(0, value.length - 2)
        else if (value.endsWith("s") && value.length > 2) value = value.substring(0, value.length - 1)
        return value
    }

    @Deprecated(
        message = "Use findSuspiciousPaths() for duplicate and shifted-segment detection.",
        replaceWith = ReplaceWith("findSuspiciousPaths(properties)")
    )
    fun findSuspiciousDuplicatedSegments(properties: Collection<ConfigProperty>): List<SuspiciousPathFinding> =
        findSuspiciousPaths(properties)
}
