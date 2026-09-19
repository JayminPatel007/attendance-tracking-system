package org.sabha.container;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.stream.Stream;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Rule <b>R1</b> of <a href="../../../../../../../docs/adr/0032-caller-authority-resolved-at-the-request-edge.md">ADR-0032</a>:
 * <b>{@code role_assignments} has one authority reader.</b> Outside
 * {@code identity-data-access}, the table may appear only in an allowlisted
 * file, each entry carrying the ADR-0029-clause-1 justification for reading it
 * there.
 *
 * <p>This is the <b>first enforcement ADR-0029 has ever had</b>. That ADR was
 * written because the {@code role = 'SANT'} check had already been copy-pasted
 * into three adapters across two contexts, and until now nothing stopped a
 * fourth.
 *
 * <p><b>Why this is not an ArchUnit rule.</b> ArchUnit reads bytecode, and the
 * SQL this rule is about lives in text blocks — constant-pool entries bytecode
 * does not expose as anything a rule can match on. R1 is therefore a
 * <em>source-text</em> check, the one gate in this module that reads
 * {@code .java} files rather than classes.
 *
 * <p><b>Why it must strip comments first.</b> 32 non-test files outside
 * {@code identity-data-access} mention {@code role_assignments}, but only
 * <b>7</b> carry SQL; the other 25 are javadoc — 10 of them in
 * {@code common-domain}, where naming the table in a port's contract is exactly
 * what ADR-0029 clause 2 asks for. A rule that matched raw text would fire on
 * the documentation written to satisfy it. {@link #withoutJavaComments} is the
 * whole difference between a live gate and noise.
 *
 * <p>Three ways to fail, because an allowlist that only ever grows is a way of
 * forgetting:
 *
 * <ol>
 *   <li>a file outside {@code identity-data-access} carries the table in real
 *       source text and is not allowlisted;</li>
 *   <li>an allowlisted file no longer carries it — its entry is now a lie and
 *       must go;</li>
 *   <li>an entry matches no file at all — a renamed or deleted class left its
 *       exemption behind.</li>
 * </ol>
 *
 * <p>The residual path this closes is narrow and deliberate. ADR-0032's other
 * invariant — <em>no engine may hold a port that answers a question about the
 * caller's own {@code role_assignments}</em> — is inexpressible (caller-vs-target
 * is a meaning: {@code JdbcReissueAuthorityLookup} reads {@code WHERE user_id = ?}
 * where {@code user_id} is the <em>target</em>, one table away from
 * {@code JdbcAuditScopeLookup} where the same lexeme is the caller). It does not
 * need a test, because {@code CallerAuthority}'s private rows make re-derivation
 * a <em>compile</em> error. The sole way left to re-derive the predicate is a new
 * adapter writing fresh {@code role_assignments} SQL below the port layer — and
 * that path, only that path, is what R1 catches.
 */
class RoleAssignmentsAccessRuleTest {

    /** Same relative path as the drift gate: surefire runs in the module directory. */
    private static final Path BACKEND = Path.of("..");

    /**
     * The module that owns the table. ADR-0029: identity writes every row and is
     * the authority on what a row <em>means</em>, so inside its own adapter ring
     * the table is permitted outright and needs no allowlist entry. {@code
     * JdbcSantLookup} is the case that makes this matter — it survives the
     * caller-authority fold, and it survives <em>here</em>.
     */
    private static final String OWNING_MODULE = "identity-service/identity-data-access";

    private static final String TABLE = "role_assignments";

    /**
     * The files outside {@code identity-data-access} allowed to name the table in
     * SQL, each with its ADR-0029-clause-1 justification — the clause that lets a
     * CQRS read-model join across context tables, as against clause 2's rule that
     * a <em>decision</em> crosses the seam through an identity-owned port.
     *
     * <p><b>Keyed by simple file name</b>, which {@code
     * IntraModuleArchitectureRulesTest.no_two_types_share_a_simple_name} already
     * guarantees is unique repo-wide (issue #218). A path would say the module
     * out loud but would also churn on every move; the uniqueness rule is the
     * cheaper spelling, and if it is ever relaxed this list's
     * matches-nothing check degrades loudly rather than silently.
     *
     * <p><b>{@code CallerVisibility} is the entry to read twice.</b> It is not a
     * read-model and it is not in a data-access ring — it sits in
     * {@code common-domain} and holds the role-tier {@code EXISTS} fragment that
     * issue #66 factored out of two adapters. It is the anti-duplication
     * mechanism ADR-0029 exists to encourage, which is why R1 keys off the
     * <b>module</b> rather than ADR-0032's original "an allowlisted CQRS
     * read-model" phrasing: that phrase no longer describes the population, and a
     * rule that fired on {@code CallerVisibility} would have the wrong predicate.
     */
    private static final Map<String, String> FILES_ALLOWED_TO_JOIN_THE_TABLE = filesAllowedToJoinTheTable();

    private static Map<String, String> filesAllowedToJoinTheTable() {
        Map<String, String> allowed = new LinkedHashMap<>();

        allowed.put("JdbcAuditFeed.java",
                "clause 1: the audit log is a read-model over existing tables (ADR-0023)");
        allowed.put("JdbcAuditScopeLookup.java",
                "clause 1: projects the viewer's geographic scope for the audit feed (ADR-0023)");

        allowed.put("JdbcCurrentRosterQuery.java",
                "clause 1: joins identity, attendance and sabha to render the Sanchalak's roster");
        allowed.put("JdbcCurrentOccurrenceQuery.java",
                "clause 1: same roster projection, narrowed to the open occurrence");
        allowed.put("JdbcSanchalakSabhasQuery.java",
                "clause 1: projects the Sabhas a Sanchalak conducts, for rendering");
        allowed.put("JdbcProxySabhaQueries.java",
                "clause 1: projects the Nirikshak's proxy-markable Sabhas (Slice 14)");

        allowed.put("CallerVisibility.java",
                "clause 1, shared fragment: the role-tier EXISTS clause issue #66 factored "
                        + "out of two adapters — one copy is the point of ADR-0029, not a breach of it");

        return Map.copyOf(allowed);
    }

    private static Map<String, String> sourcesByFileName;

    /** Every production {@code .java} file outside the owning module, mapped by simple name. */
    @BeforeAll
    static void readBackendSources() throws IOException {
        Map<String, String> sources = new TreeMap<>();
        try (Stream<Path> tree = Files.walk(BACKEND)) {
            tree.filter(Files::isRegularFile)
                    .filter(path -> path.toString().endsWith(".java"))
                    .filter(path -> slashed(path).contains("/src/main/java/"))
                    .filter(path -> !slashed(path).contains("/target/"))
                    .filter(path -> !slashed(path).contains("/" + OWNING_MODULE + "/"))
                    .forEach(path -> sources.put(path.getFileName().toString(), read(path)));
        }
        sourcesByFileName = sources;
    }

    /**
     * The gate. One assertion over all three failure modes, so a run reports
     * everything wrong at once rather than one thing per fix.
     */
    @Test
    void roleAssignmentsSqlOutsideIdentityLivesOnlyInAnAllowlistedFile() {
        List<String> violations = new ArrayList<>();
        Set<String> unmatchedEntries = new TreeSet<>(FILES_ALLOWED_TO_JOIN_THE_TABLE.keySet());

        sourcesByFileName.forEach((fileName, source) -> {
            boolean carriesSql = withoutJavaComments(source).contains(TABLE);
            boolean allowlisted = FILES_ALLOWED_TO_JOIN_THE_TABLE.containsKey(fileName);

            if (allowlisted) {
                unmatchedEntries.remove(fileName);
                if (!carriesSql) {
                    violations.add(fileName + " no longer names " + TABLE + " in SQL — delete its allowlist entry");
                }
                return;
            }
            if (carriesSql) {
                violations.add(fileName + " names " + TABLE + " outside " + OWNING_MODULE
                        + " and is not allowlisted — read ADR-0029: a projection may join the table "
                        + "(clause 1, add an entry here saying why), a decision goes through an "
                        + "identity-owned port (clause 2)");
            }
        });

        for (String fileName : unmatchedEntries) {
            violations.add("allowlisted " + fileName + " matches no production file — delete its allowlist entry");
        }

        assertThat(violations).as("ADR-0032 rule R1: %s is identity-owned", TABLE).isEmpty();
    }

    /**
     * Non-vacuity, and the only guard against the silent failure this check has
     * that an ArchUnit rule does not: a wrong {@link #BACKEND} or a changed
     * source layout would walk an empty tree and pass everything. Pinning the
     * count to the allowlist's own size means the scan has to keep finding the
     * files the rule is about.
     */
    @Test
    void theScanActuallyReachesTheBackendSources() {
        long carryTheTable = sourcesByFileName.values().stream()
                .filter(source -> withoutJavaComments(source).contains(TABLE))
                .count();

        assertThat(sourcesByFileName).as("production sources outside " + OWNING_MODULE).hasSizeGreaterThan(100);
        assertThat(carryTheTable).as("files carrying " + TABLE + " in SQL")
                .isEqualTo(FILES_ALLOWED_TO_JOIN_THE_TABLE.size());
    }

    /**
     * Comments out, string and text-block contents in. Hand-written because the
     * job is smaller than any parser worth a dependency: a lexer that knows four
     * things — text block, string, char, comment — is enough to separate the SQL
     * from the prose about the SQL.
     *
     * <p>Text-block content is <b>kept</b>, because that is where every query in
     * this codebase lives; the first version of this scan dropped it and reported
     * one offender instead of seven.
     */
    private static String withoutJavaComments(String source) {
        StringBuilder code = new StringBuilder(source.length());
        int i = 0;
        int end = source.length();

        while (i < end) {
            if (source.startsWith("\"\"\"", i)) {
                int close = i + 3;
                while (close < end && !source.startsWith("\"\"\"", close)) {
                    close += source.charAt(close) == '\\' ? 2 : 1;
                }
                code.append(source, i + 3, Math.min(close, end));
                i = close + 3;
            } else if (source.charAt(i) == '"' || source.charAt(i) == '\'') {
                char quote = source.charAt(i++);
                while (i < end && source.charAt(i) != quote) {
                    if (source.charAt(i) == '\\') {
                        i++;
                    }
                    code.append(source.charAt(i++));
                }
                i++;
            } else if (source.startsWith("//", i)) {
                while (i < end && source.charAt(i) != '\n') {
                    i++;
                }
            } else if (source.startsWith("/*", i)) {
                int close = source.indexOf("*/", i + 2);
                i = close < 0 ? end : close + 2;
            } else {
                code.append(source.charAt(i++));
            }
        }
        return code.toString();
    }

    private static String slashed(Path path) {
        return path.toString().replace('\\', '/');
    }

    private static String read(Path path) {
        try {
            return Files.readString(path);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
