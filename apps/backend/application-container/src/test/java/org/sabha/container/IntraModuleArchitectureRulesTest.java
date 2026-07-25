package org.sabha.container;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noMethods;

import com.tngtech.archunit.core.domain.JavaMethod;
import com.tngtech.archunit.core.domain.JavaMethodCall;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchCondition;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.lang.ConditionEvents;
import com.tngtech.archunit.lang.SimpleConditionEvent;
import org.sabha.common.AggregateRoot;
import org.sabha.common.DomainEvent;
import org.sabha.identity.applicationservice.otp.OtpGuardedFlow;
import org.springframework.transaction.annotation.Transactional;

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
 * <p><b>Which rules have teeth.</b> Two of these rules catch violations the
 * compiler permits and are the load-bearing ones: {@code @Transactional}
 * placement (spring-tx is on the adapter classpath) and aggregate/event residence
 * (an application-service can reference the {@code common-domain} base types and
 * declare a subtype). The presentation→adapter rule and the adapter-isolation
 * rules are <em>defense-in-depth</em>: the Maven module graph (ADR-0019) already
 * makes those dependencies impossible to compile, so they cannot fire today. They
 * are kept anyway — cheaply — as executable intent that becomes live the moment a
 * module's pom gains a forbidden dependency.
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

    @ArchTest
    static final ArchRule domain_events_live_in_domain_core =
            classes()
                    .that().areAssignableTo(DomainEvent.class)
                    .should().resideInAnyPackage("..domain..", "org.sabha.common")
                    .as("DomainEvent implementations must live in *-domain-core (ADR-0020)")
                    .because("domain events are emitted by aggregates and belong in the innermost ring");
}
