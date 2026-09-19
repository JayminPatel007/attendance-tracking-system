package org.sabha.container;

import static com.tngtech.archunit.core.domain.properties.CanBeAnnotated.Predicates.annotatedWith;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.methods;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noMethods;

import java.lang.annotation.Annotation;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import com.tngtech.archunit.base.DescribedPredicate;
import com.tngtech.archunit.core.domain.JavaAnnotation;
import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.JavaEnumConstant;
import com.tngtech.archunit.core.domain.JavaField;
import com.tngtech.archunit.core.domain.JavaMethod;
import com.tngtech.archunit.core.domain.JavaMethodCall;
import com.tngtech.archunit.core.domain.JavaModifier;
import com.tngtech.archunit.core.domain.JavaParameter;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchCondition;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.lang.ConditionEvents;
import com.tngtech.archunit.lang.SimpleConditionEvent;
import org.sabha.common.AggregateRoot;
import org.sabha.common.CallerAuthority;
import org.sabha.common.DomainEvent;
import org.sabha.common.web.CurrentUser;
import org.sabha.identity.applicationservice.otp.OtpGuardedFlow;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;

/**
 * Executable encoding of the intra-module architecture rules that the Maven
 * module graph cannot enforce on its own (issue #69). ADR-0019 gives the build
 * graph the cross-module guarantees — cross-context reach-ins don't compile, and
 * domain-core stays Spring-free — but the conventions <em>inside</em> a module
 * (a controller not injecting a repository, {@code @Transactional} staying in the
 * use-case tier) are convention only. These rules make those conventions fail the
 * build.
 *
 * <p>Package ↔ Clean-ring mapping (ADR-0019), where {@code <ctx>} is one of
 * identity / sabha / attendance / analytics:
 * <ul>
 *   <li>{@code org.sabha.common}              — common-domain (entities ring)</li>
 *   <li>{@code org.sabha.<ctx>.domain}        — *-domain-core (entities ring)</li>
 *   <li>{@code org.sabha.<ctx>.applicationservice} — *-application-service (use-case ring)</li>
 *   <li>{@code org.sabha.<ctx>.dataaccess}    — *-data-access (adapter ring)</li>
 *   <li>{@code org.sabha.<ctx>.messaging}     — *-messaging (adapter ring)</li>
 *   <li>{@code org.sabha.<ctx>.application}   — *-application / presentation (adapter ring)</li>
 *   <li>{@code org.sabha.container}           — application-container (frameworks ring)</li>
 * </ul>
 *
 * <p>Tests are excluded from the import so the rules judge production code only.
 *
 * <p><b>Which rules have teeth.</b> Four of these rules catch violations the
 * compiler permits and are the load-bearing ones: {@code @Transactional}
 * placement (spring-tx is on the adapter classpath); aggregate/event residence
 * (an application-service can reference the {@code common-domain} base types and
 * declare a subtype); credential handling (spring-security is on every
 * {@code *-application} classpath, so re-introducing a subject parse compiles
 * fine — ADR-0030); and the query-port wrapper rule (nothing stops a new
 * delegating {@code @Service}). The presentation→adapter rule and the
 * adapter-isolation rules are <em>defense-in-depth</em>: the Maven module graph
 * (ADR-0019) already makes those dependencies impossible to compile, so they
 * cannot fire today. They are kept anyway — cheaply — as executable intent that
 * becomes live the moment a module's pom gains a forbidden dependency.
 */
@AnalyzeClasses(packages = "org.sabha", importOptions = ImportOption.DoNotIncludeTests.class)
class IntraModuleArchitectureRulesTest {

    private static final ArchCondition<JavaMethod> CALL_OTP_CONSUME =
            new ArchCondition<>("call " + OtpGuardedFlow.class.getSimpleName() + ".consume") {
                @Override
                public void check(JavaMethod method, ConditionEvents events) {
                    for (JavaMethodCall call : method.getMethodCallsFromSelf()) {
                        boolean isConsume = call.getTargetOwner().isAssignableTo(OtpGuardedFlow.class)
                                && "consume".equals(call.getTarget().getName());
                        events.add(new SimpleConditionEvent(call, isConsume, call.getDescription()));
                    }
                }
            };

    /** The {@code @RequestMapping} shortcuts, in the order {@link #routeOf} tries them. */
    private static final Map<Class<? extends Annotation>, String> MAPPING_SHORTCUTS = mappingShortcuts();

    private static Map<Class<? extends Annotation>, String> mappingShortcuts() {
        Map<Class<? extends Annotation>, String> shortcuts = new LinkedHashMap<>();
        shortcuts.put(GetMapping.class, "GET");
        shortcuts.put(PostMapping.class, "POST");
        shortcuts.put(PutMapping.class, "PUT");
        shortcuts.put(PatchMapping.class, "PATCH");
        shortcuts.put(DeleteMapping.class, "DELETE");
        return Map.copyOf(shortcuts);
    }

    /**
     * The handlers allowed to resolve no caller, each with the reason it is
     * allowed (issue #210). Two kinds of entry, and the difference is the point:
     *
     * <ul>
     *   <li><b>public by design</b> — the endpoint is reachable before or without
     *       authentication, so there is no caller to resolve;</li>
     *   <li><b>unreviewed</b> — the endpoint is authenticated and resolves nobody.
     *       {@code SecurityConfig} is {@code anyRequest().authenticated()} on both
     *       chains and there is no {@code @PreAuthorize} in the codebase, so
     *       "is logged in" is the entire authorization check. Each of these is a
     *       gap until triage says otherwise.</li>
     * </ul>
     *
     * <p>An exemption is a line someone has to delete or rewrite, which is what
     * makes this list worth more than the silence it replaces: {@code
     * GET /api/sabhas/{sabhaId}/monthly-compliance} was callerless for four
     * months (issue #209) and nothing said so. This list should be empty of
     * {@code unreviewed} entries when #210 closes.
     */
    private static final Map<String, String> HANDLERS_THAT_RESOLVE_NO_CALLER = handlersThatResolveNoCaller();

    private static Map<String, String> handlersThatResolveNoCaller() {
        Map<String, String> exempt = new LinkedHashMap<>();

        exempt.put("POST /api/password-reset/request", "public by design: pre-authentication (Slice 18)");
        exempt.put("POST /api/password-reset/verify", "public by design: pre-authentication (Slice 18)");
        exempt.put("POST /api/password-reset/complete", "public by design: pre-authentication (Slice 18)");
        exempt.put("GET /api/who-appointed-me", "public by design: lost-mobile lookup keyed on username (ADR-0004)");

        exempt.put("GET /api/directory/persons", "unreviewed (#210): unscoped Person search");
        exempt.put("GET /api/directory/name-search", "unreviewed (#210): unscoped Person search");
        exempt.put("GET /api/directory/walk-in-search", "unreviewed (#210): unscoped Person search");
        exempt.put("GET /api/directory/persons/{id}", "unreviewed (#210): unscoped Person read");
        exempt.put("GET /bff/directory/search", "unreviewed (#210): unscoped Person search");
        exempt.put("GET /bff/directory/name-search", "unreviewed (#210): unscoped Person search");
        exempt.put("GET /bff/structure/cities", "unreviewed (#210): reference data, probably fine");
        exempt.put("GET /bff/structure/zones", "unreviewed (#210): reference data, probably fine");
        exempt.put("GET /bff/structure/kshetras", "unreviewed (#210): reference data, probably fine");
        exempt.put("GET /bff/structure/sabha-kinds", "unreviewed (#210): reference data, probably fine");
        exempt.put("GET /bff/appointments/sah-nirdeshak-cap", "unreviewed (#210): Sah-Nirdeshak count per Kshetra (#86)");
        exempt.put("POST /api/home-sabha-transfers/{id}/confirm", "unreviewed (#210): a write — check the OTP is the authorization");

        return Map.copyOf(exempt);
    }

    /**
     * ADR-0030: a handler that never learns who is calling cannot authorize
     * anything below it. Three ways to fail, because an allowlist that only ever
     * grows is a way of forgetting:
     *
     * <ol>
     *   <li>a handler takes no {@code @CurrentUser} and is not exempt;</li>
     *   <li>an exempt handler has since gained a caller — its entry is now a lie
     *       and must go;</li>
     *   <li>an entry matches no handler at all — a renamed or deleted route left
     *       its exemption behind.</li>
     * </ol>
     */
    private static final ArchCondition<JavaMethod> RESOLVE_THEIR_CALLER =
            new ArchCondition<>("resolve their caller as a @CurrentUser CallerAuthority (ADR-0030, ADR-0032)") {
                private Set<String> unmatchedExemptions;

                @Override
                public void init(Collection<JavaMethod> handlers) {
                    unmatchedExemptions = new TreeSet<>(HANDLERS_THAT_RESOLVE_NO_CALLER.keySet());
                }

                @Override
                public void check(JavaMethod handler, ConditionEvents events) {
                    String route = routeOf(handler);
                    List<JavaParameter> bound = handler.getParameters().stream()
                            .filter(parameter -> parameter.isAnnotatedWith(CurrentUser.class))
                            .toList();
                    boolean resolvesCaller = !bound.isEmpty();

                    if (HANDLERS_THAT_RESOLVE_NO_CALLER.containsKey(route)) {
                        unmatchedExemptions.remove(route);
                        events.add(new SimpleConditionEvent(handler, !resolvesCaller,
                                route + " now takes a @CurrentUser — delete its exemption"));
                        return;
                    }
                    events.add(new SimpleConditionEvent(handler, resolvesCaller,
                            route + " takes no @CurrentUser parameter (" + handler.getFullName() + ")"));

                    // ADR-0032 rule R2: one predicate on this rule, not a rule of its
                    // own. There is exactly one caller parameter type, and that is what
                    // makes the authority query happen once per request with nothing
                    // resembling a cache. A relapse to @CurrentUser UserId still
                    // satisfies the clause above — it is this clause that fails it.
                    for (JavaParameter parameter : bound) {
                        boolean isAuthority = parameter.getRawType()
                                .isAssignableTo(CallerAuthority.class);
                        events.add(new SimpleConditionEvent(handler, isAuthority,
                                route + " binds @CurrentUser to "
                                        + parameter.getRawType().getSimpleName()
                                        + ", not CallerAuthority — ADR-0032 allows exactly one caller "
                                        + "parameter type (" + handler.getFullName() + ")"));
                    }
                }

                @Override
                public void finish(ConditionEvents events) {
                    for (String route : unmatchedExemptions) {
                        events.add(SimpleConditionEvent.violated(route,
                                "exempt route " + route + " matches no handler — delete its exemption"));
                    }
                }
            };

    /**
     * Spring's own definition: every mapping shortcut is meta-annotated
     * {@code @RequestMapping}, and so is any composed annotation a future
     * controller might introduce. Asking the meta-annotation — rather than
     * listing the five shortcuts — is what keeps the gate from having the same
     * shape of blind spot it exists to close. A handler it cannot name still
     * gets checked; only its key degrades, and since a degraded key is in no
     * exemption list, the failure is loud.
     */
    private static final DescribedPredicate<JavaMethod> ARE_HTTP_MAPPED =
            DescribedPredicate.describe("are HTTP-mapped",
                    method -> method.isAnnotatedWith(RequestMapping.class)
                            || method.isMetaAnnotatedWith(RequestMapping.class));

    /**
     * The exemption key for a handler: {@code "GET /api/sanchalak/current-roster"}.
     * Falls back to the method's full name for a mapping this cannot read —
     * a composed annotation, or one that names its path some third way. That
     * fallback matches no exemption, so an unreadable mapping fails rather than
     * slips through.
     */
    private static String routeOf(JavaMethod method) {
        for (Map.Entry<Class<? extends Annotation>, String> shortcut : MAPPING_SHORTCUTS.entrySet()) {
            if (method.isAnnotatedWith(shortcut.getKey())) {
                return shortcut.getValue() + " " + pathOf(method.getAnnotationOfType(shortcut.getKey().getName()));
            }
        }
        if (method.isAnnotatedWith(RequestMapping.class)) {
            JavaAnnotation<?> mapping = method.getAnnotationOfType(RequestMapping.class.getName());
            Object[] verbs = (Object[]) mapping.get("method").orElse(new Object[0]);
            // A JavaEnumConstant prints as "RequestMethod.GET"; the key reads as the
            // route a person would type, so take the constant's bare name.
            String verb = verbs.length == 0 ? "ANY" : ((JavaEnumConstant) verbs[0]).name();
            return verb + " " + pathOf(mapping);
        }
        return method.getFullName();
    }

    /**
     * {@code value} and {@code path} are {@code @AliasFor} each other, and ArchUnit
     * reads bytecode, where an alias is just the other member left at its default —
     * so both have to be asked.
     */
    private static String pathOf(JavaAnnotation<?> mapping) {
        for (String member : List.of("value", "path")) {
            Object[] paths = (Object[]) mapping.get(member).orElse(new Object[0]);
            if (paths.length > 0) {
                return String.valueOf(paths[0]);
            }
        }
        return "(no path)";
    }

    private static final ArchCondition<JavaClass> WRAP_ONE_QUERY_PORT =
            new ArchCondition<>("wrap a single query port") {
                @Override
                public void check(JavaClass type, ConditionEvents events) {
                    // Instance fields only: getFields() includes statics, so counting
                    // them would let any constant — an unused one included — defeat
                    // the rule. Verified: adding `private static final int` to a
                    // wrapper made it pass.
                    List<JavaField> fields = type.getFields().stream()
                            .filter(field -> !field.getModifiers().contains(JavaModifier.STATIC))
                            .toList();
                    if (fields.size() == 1 && isQueryPort(fields.get(0))) {
                        events.add(new SimpleConditionEvent(type, true,
                                type.getName() + " wraps only " + fields.get(0).getRawType().getSimpleName()));
                    }
                }

                /**
                 * The repo's naming for a read-side port. Not restricted to
                 * interfaces: {@code MonthlyComplianceQuery} is a concrete
                 * {@code @Service} carrying the suffix, so wrapping one is the same
                 * smell and should fire the same way.
                 */
                private boolean isQueryPort(JavaField field) {
                    String name = field.getRawType().getSimpleName();
                    return name.endsWith("Query") || name.endsWith("Queries");
                }
            };

    /**
     * ADR-0017/0019: REST controllers + DTOs in {@code *-application} are
     * presentation only. They depend on their {@code *-application-service} (use
     * cases, ports, DTOs) and never on outbound adapters — a controller injecting
     * a JDBC repository or a messaging adapter directly skips the use-case tier.
     * Defense-in-depth (see class javadoc): {@code *-application} has no pom
     * dependency on {@code *-data-access}/{@code *-messaging}, so this already
     * fails to compile; the rule guards against a future pom loosening that.
     */
    @ArchTest
    static final ArchRule presentation_does_not_depend_on_outbound_adapters =
            noClasses()
                    .that().resideInAPackage("..application..")
                    .should().dependOnClassesThat().resideInAnyPackage("..dataaccess..", "..messaging..")
                    .as("REST controllers (*-application) must not depend on *-data-access or *-messaging adapters (ADR-0017)")
                    .because("presentation delegates to use cases in *-application-service, not to outbound adapters");

    /**
     * ADR-0018/0019: the transaction boundary belongs to the use-case tier.
     * {@code @Transactional} (whether on a class or a method) lives only in
     * {@code *-application-service}. It must not appear in domain-core (pure
     * Java), in presentation, or in an outbound adapter — an adapter that opens
     * its own transaction hides the boundary from the orchestrating use case.
     */
    @ArchTest
    static final ArchRule transactional_classes_only_in_application_service =
            noClasses()
                    .that().resideOutsideOfPackage("..applicationservice..")
                    .should().beAnnotatedWith(Transactional.class)
                    .as("@Transactional may annotate a class only in *-application-service (ADR-0018)")
                    .because("the transaction boundary belongs to the use-case tier, not to adapters or the domain");

    @ArchTest
    static final ArchRule transactional_methods_only_in_application_service =
            noMethods()
                    .that().areDeclaredInClassesThat().resideOutsideOfPackage("..applicationservice..")
                    .should().beAnnotatedWith(Transactional.class)
                    .as("@Transactional may annotate a method only in *-application-service (ADR-0018)")
                    .because("the transaction boundary belongs to the use-case tier, not to adapters or the domain");

    /**
     * ADR-0018 + issue #130: {@link OtpGuardedFlow#consume} carries the
     * {@code noRollbackFor} rules that let a rejected OTP keep its incremented
     * attempt count, so it has to be the outermost transaction of the consume
     * step. A caller that opened its own {@code @Transactional} boundary around it
     * would have <em>its</em> rules decide the rollback instead, and the attempt
     * budget would silently reset on every wrong guess — a regression no unit test
     * of the caller can see, since it needs a real transaction to show up.
     */
    @ArchTest
    static final ArchRule callers_of_the_otp_consume_step_do_not_own_its_transaction =
            noMethods()
                    .that().areAnnotatedWith(Transactional.class)
                    .should(CALL_OTP_CONSUME)
                    .as("a @Transactional method must not call OtpGuardedFlow.consume (issue #130)")
                    .because("consume owns the rollback rules that let a rejected OTP persist its attempt count");

    /**
     * ADR-0019: outbound adapters implement ports declared in their own context's
     * {@code *-application-service} and depend inward only — never on a sibling
     * adapter or on presentation. Defense-in-depth (see class javadoc): the module
     * graph gives {@code *-data-access} a dependency on its {@code *-application-service}
     * alone, so a reference to {@code *-messaging} or {@code *-application} already
     * fails to compile. This rule fires only if a pom later loosens that.
     */
    @ArchTest
    static final ArchRule data_access_adapters_depend_inward_only =
            noClasses()
                    .that().resideInAPackage("..dataaccess..")
                    .should().dependOnClassesThat().resideInAnyPackage("..messaging..", "..application..")
                    .as("*-data-access must not depend on *-messaging or *-application (ADR-0019)")
                    .because("an adapter implements its own application-service's ports; it does not call sibling adapters or presentation");

    @ArchTest
    static final ArchRule messaging_adapters_depend_inward_only =
            noClasses()
                    .that().resideInAPackage("..messaging..")
                    .should().dependOnClassesThat().resideInAnyPackage("..dataaccess..", "..application..")
                    .as("*-messaging must not depend on *-data-access or *-application (ADR-0019)")
                    .because("an adapter implements its own application-service's ports; it does not call sibling adapters or presentation");

    /**
     * ADR-0020: rich-domain conventions. Aggregate roots extend {@code AggregateRoot}
     * and domain events implement {@code DomainEvent}; both base types live in
     * common-domain, and every concrete subtype is a domain object that belongs in
     * a {@code *-domain-core} package ({@code org.sabha.<ctx>.domain}). The
     * accepted residences are therefore domain-core or common-domain itself (which
     * holds the base types). An aggregate or event surfacing in an
     * application-service, adapter, or the container would mean domain state had
     * leaked out of the entities ring.
     */
    @ArchTest
    static final ArchRule aggregate_roots_live_in_domain_core =
            classes()
                    .that().areAssignableTo(AggregateRoot.class)
                    .should().resideInAnyPackage("..domain..", "org.sabha.common")
                    .as("AggregateRoot subclasses must live in *-domain-core (ADR-0020)")
                    .because("aggregates are entities and belong in the innermost ring, not in a use case or adapter");

    /**
     * ADR-0030: caller identity is resolved once, at the HTTP edge. Exactly two
     * packages may know what a credential looks like — {@code common-application}
     * (the {@code @CurrentUser} resolver) and {@code application-container} (the
     * filter chains). Everything else takes a resolved {@code UserId}.
     *
     * <p>This rule has teeth: {@code spring-security-core} and
     * {@code -oauth2-jose} are on every {@code *-application} module's classpath
     * (ADR-0019), so re-introducing {@code UUID.fromString(jwt.getSubject())} in a
     * controller compiles perfectly well. Before this rule, four controllers
     * resolved at the edge and thirteen did not.
     *
     * <p>It also forecloses reading any other claim in a controller. No endpoint
     * needs one today; if one ever does, the honest move is to widen what the edge
     * resolves, not to reach for the token in presentation.
     */
    @ArchTest
    static final ArchRule only_the_edge_knows_what_a_credential_is =
            noClasses()
                    .that().resideOutsideOfPackages("org.sabha.common.web..", "org.sabha.container..")
                    .should().dependOnClassesThat().resideInAnyPackage(
                            "org.springframework.security.core..",
                            "org.springframework.security.oauth2..")
                    .as("only common-application and application-container may touch a credential (ADR-0030)")
                    .because("the caller is resolved once at the edge; everything below takes a UserId");

    /**
     * Issue #203's tail: a read-only application service that wraps exactly one
     * query port hides nothing. {@code GetCurrentRosterUseCase} and
     * {@code GetCurrentOccurrenceUseCase} each hid one decision — resolve the
     * caller first — and when ADR-0030 moved that decision to the edge, both
     * collapsed to {@code query.find…(caller)} and were deleted. Nine controllers
     * across the four contexts now inject a query port directly for reads; none
     * wraps one.
     *
     * <p>Deliberately narrow: it keys on a single field whose type name ends
     * {@code Query}/{@code Queries}, which is this repo's naming for a read-side
     * port. A one-field service over a differently-named port (identity's
     * {@code PersonDirectory}) is the same smell and escapes — accepted, because
     * the alternative is bytecode heuristics for "does this method only
     * delegate?", which would argue with legitimately thin services that hide a
     * page size or a fallback. A rule that catches the exact regression we fixed,
     * with no false positives, beats a cleverer one nobody trusts.
     */
    @ArchTest
    static final ArchRule application_services_do_not_wrap_a_single_query_port =
            noClasses()
                    .that().resideInAPackage("..applicationservice..")
                    // Grouped deliberately: the fluent `.and().areAnnotatedWith(A)
                    // .or().areAnnotatedWith(B)` reads as (package AND A) OR B, which
                    // drops the package restriction entirely. Verified — it fired on a
                    // @Component in org.sabha.container.
                    .and(annotatedWith(Service.class).or(annotatedWith(Component.class)))
                    .should(WRAP_ONE_QUERY_PORT)
                    .as("an application service must not be a wrapper over one query port (issue #203)")
                    .because("presentation reads straight through the port; a delegating service hides nothing");

    /**
     * ADR-0030 resolved the caller at the edge; this keeps every handler asking
     * for it. The decision it guards is not "controllers call use cases" — nine
     * controllers inject a read-side query port directly and that is the accepted
     * shape — but the narrower, load-bearing one: <em>a handler resolves a caller
     * and passes it down</em>. {@code AttendanceRestController} is the example.
     * Its first two handlers go straight to a query port, and they are fine,
     * because they go {@code query.findForSanchalak(caller)}. The eleventh went
     * {@code query.needsOccurrence(sabhaId, today)} and was the one hole.
     *
     * <p>This rule has teeth for the same reason ADR-0030's does: omitting a
     * parameter compiles. Nothing else in the build notices — not the type
     * system, and not the OpenAPI drift gate, which documents a callerless
     * handler as happily as any other.
     *
     * <p><b>What it checks is that the parameter is declared, not that it is
     * used.</b> A handler could take {@code @CurrentUser CallerAuthority caller} and
     * ignore it, reproducing issue #209's hole while passing. That gap is
     * accepted: a parameter's use lives in local-variable bytecode that ArchUnit
     * does not model, and the alternative — matching against method calls that
     * happen to pass a {@code UserId} — would argue with every handler that
     * legitimately forwards the caller through a DTO. The regression this
     * catches is the one that actually happened: an omission nobody wrote down.
     */
    @ArchTest
    static final ArchRule every_handler_resolves_its_caller =
            methods()
                    .that().areDeclaredInClassesThat().resideInAPackage("..application..")
                    .and(ARE_HTTP_MAPPED)
                    .should(RESOLVE_THEIR_CALLER)
                    .as("every HTTP handler must take a @CurrentUser parameter or be listed as exempt (ADR-0030, issue #210)")
                    .because("a handler that never learns who is calling cannot authorize anything below it");

    /**
     * No two top-level types may share a simple name (issue #218). The name a
     * reader sees at an import is the only thing they have to go on: two types
     * called {@code SabhaKind} meant an auto-import landed on the aggregate when
     * the caller wanted the column encoding, and four {@code *NotFoundException}
     * classes meant the same 404 was written four times.
     *
     * <p>Scoped to <em>top-level</em> types — one per file, which is what a
     * reader actually browses and what an import names without qualification.
     * Nested types are excluded deliberately: a controller's private
     * {@code CancelRequest} record is read through its enclosing class and
     * collides with nothing.</p>
     */
    private static final ArchCondition<JavaClass> HAVE_A_UNIQUE_SIMPLE_NAME =
            new ArchCondition<>("have a simple name no other top-level type uses") {
                private Map<String, Set<String>> bySimpleName;

                @Override
                public void init(Collection<JavaClass> types) {
                    bySimpleName = new LinkedHashMap<>();
                    for (JavaClass type : types) {
                        bySimpleName
                                .computeIfAbsent(type.getSimpleName(), name -> new TreeSet<>())
                                .add(type.getName());
                    }
                }

                @Override
                public void check(JavaClass type, ConditionEvents events) {
                    // The verdict is collective, so it is reported once in finish().
                }

                @Override
                public void finish(ConditionEvents events) {
                    bySimpleName.forEach((simpleName, fullNames) -> {
                        if (fullNames.size() > 1) {
                            events.add(SimpleConditionEvent.violated(simpleName,
                                    simpleName + " is the simple name of " + fullNames.size()
                                            + " top-level types: " + String.join(", ", fullNames)));
                        }
                    });
                }
            };

    /**
     * Excludes nested types (a nested type has an enclosing class) and
     * {@code package-info}, which is one per package by construction and names
     * no type a reader can import.
     */
    private static final DescribedPredicate<JavaClass> ARE_TOP_LEVEL_TYPES =
            DescribedPredicate.describe("are top-level types",
                    type -> !type.getEnclosingClass().isPresent()
                            && !type.getSimpleName().equals("package-info"));

    @ArchTest
    static final ArchRule no_two_types_share_a_simple_name =
            classes()
                    .that().resideInAPackage("org.sabha..")
                    .and(ARE_TOP_LEVEL_TYPES)
                    .should(HAVE_A_UNIQUE_SIMPLE_NAME)
                    .as("no two top-level types under org.sabha may share a simple name (issue #218)")
                    .because("a reader seeing the bare name cannot tell which type they have, and no file can import both");

    @ArchTest
    static final ArchRule domain_events_live_in_domain_core =
            classes()
                    .that().areAssignableTo(DomainEvent.class)
                    .should().resideInAnyPackage("..domain..", "org.sabha.common")
                    .as("DomainEvent implementations must live in *-domain-core (ADR-0020)")
                    .because("domain events are emitted by aggregates and belong in the innermost ring");

    /**
     * ADR-0032 rule R3. The five Authorization Engines that survived the
     * caller-authority fold, by name: every public method returns a decision — a
     * {@code boolean}, a sealed type, or an {@link java.util.Optional} — and
     * neither the class nor any method is {@code @Transactional}.
     *
     * <p><b>A reviewed list, not a marker interface.</b> The repo's two existing
     * markers earn their keep by carrying behaviour ({@link AggregateRoot}) or a
     * dispatch contract ({@link DomainEvent}); one carrying neither would be the
     * test leaking into the domain, which ADR-0019's ring discipline exists to
     * prevent. A list also <em>detects its own staleness</em> — a marker on a
     * deleted class vanishes in silence, while a stale entry here fails
     * loudly.</p>
     *
     * <p>Retrodiction, which is why this rule is believable rather than merely
     * true: it catches {@code DashboardAccess.cityChip}, a DTO-returning method
     * that PR B of #133 relocated to the query side <em>before</em> this rule
     * existed. What it cannot catch is a write — {@code selectCity} returned a
     * sealed {@code DashboardScope} and would have passed while mutating. The
     * instrument that found that one was human inventory, and the fix was
     * relocation, not a rule; "an engine never throws or mutates" stays
     * javadoc, deliberately.</p>
     */
    private static final Set<String> THE_AUTHORIZATION_ENGINES = Set.of(
            "org.sabha.attendance.applicationservice.AuthorizationEngine",
            "org.sabha.identity.applicationservice.appointment.AppointmentAuthorization",
            "org.sabha.identity.applicationservice.passwordreset.ReissueAuthorization",
            "org.sabha.analytics.applicationservice.AuditLogAccess",
            "org.sabha.analytics.applicationservice.DashboardAccess");

    private static final DescribedPredicate<JavaClass> ARE_AUTHORIZATION_ENGINES =
            DescribedPredicate.describe("are one of the five Authorization Engines (ADR-0032)",
                    type -> THE_AUTHORIZATION_ENGINES.contains(type.getName()));

    private static final ArchCondition<JavaClass> RETURN_A_DECISION_AND_HOLD_NO_TRANSACTION =
            new ArchCondition<>("return a decision and hold no transaction") {
                private Set<String> unmatched;

                @Override
                public void init(Collection<JavaClass> types) {
                    unmatched = new TreeSet<>(THE_AUTHORIZATION_ENGINES);
                }

                @Override
                public void check(JavaClass type, ConditionEvents events) {
                    unmatched.remove(type.getName());

                    events.add(new SimpleConditionEvent(type, !type.isAnnotatedWith(Transactional.class),
                            type.getSimpleName() + " is @Transactional — an engine decides, it does not write"));

                    for (JavaMethod method : type.getMethods()) {
                        if (!method.getModifiers().contains(JavaModifier.PUBLIC)) {
                            continue;
                        }
                        events.add(new SimpleConditionEvent(method, !method.isAnnotatedWith(Transactional.class),
                                method.getFullName() + " is @Transactional — an engine decides, it does not write"));

                        JavaClass returned = method.getRawReturnType();
                        boolean decides = returned.getName().equals("boolean")
                                || returned.isAssignableTo(java.util.Optional.class)
                                || returned.getModifiers().contains(JavaModifier.valueOf("ABSTRACT"))
                                && returned.isInterface()
                                || isSealed(returned);
                        events.add(new SimpleConditionEvent(method, decides,
                                method.getFullName() + " returns " + returned.getSimpleName()
                                        + " — an engine's public methods return a boolean, a sealed type "
                                        + "or an Optional, never a DTO (ADR-0032 R3)"));
                    }
                }

                @Override
                public void finish(ConditionEvents events) {
                    for (String name : unmatched) {
                        events.add(SimpleConditionEvent.violated(name,
                                "listed engine " + name + " matches no class — delete or rename its entry"));
                    }
                }

                /** Sealed-ness is not on ArchUnit's model, so it is read off the bytecode's own type. */
                private boolean isSealed(JavaClass type) {
                    return type.reflect().isSealed();
                }
            };

    @ArchTest
    static final ArchRule authority_engines_return_a_decision_and_hold_no_transaction =
            classes()
                    .that(ARE_AUTHORIZATION_ENGINES)
                    .should(RETURN_A_DECISION_AND_HOLD_NO_TRANSACTION)
                    .as("an Authorization Engine returns a decision and holds no transaction (ADR-0032 R3)")
                    .because("an engine that writes, or hands back a view-model, has stopped being a decision component");

    /**
     * ADR-0032 rule R4. {@link CallerAuthority} is an application-ring type: the
     * innermost ring ({@code *-domain-core}) and the outermost ({@code
     * *-data-access}, {@code *-messaging}) may not depend on it (ADR-0019).
     *
     * <p>Plain ArchUnit, because it keys off one named type — no marker, no list.
     * <b>Verified non-vacuous</b>: the poms do not already close this. {@code
     * VisibleSections}, in {@code identity-domain-core}, imports {@code
     * org.sabha.common.Role} today, so {@code common-domain} <em>is</em> visible
     * from the innermost ring and this rule has something real to forbid.</p>
     */
    @ArchTest
    static final ArchRule caller_authority_stays_out_of_the_inner_and_outer_rings =
            noClasses()
                    .that().resideInAnyPackage("org.sabha..domain", "org.sabha..dataaccess", "org.sabha..messaging")
                    .should().dependOnClassesThat().areAssignableTo(CallerAuthority.class)
                    .as("no *-domain-core, *-data-access or *-messaging class may depend on CallerAuthority (ADR-0032 R4)")
                    .because("the caller's authority is resolved in the application ring and decided above the aggregates");
}
