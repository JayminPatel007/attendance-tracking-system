# AggregateRoot base class, domain events, and optimistic locking

**Status**: accepted. **Extends [ADR-0019](0019-bounded-context-module-taxonomy.md).**

DDD/Clean Architecture works only if business rules actually live on the aggregates. The current backend code (`Occurrence` as a `record` with one predicate, `MarkAttendanceUseCase` containing the state-transition check) is the textbook *anemic* shape — domain data with no behaviour, behaviour scattered into use cases. This ADR pins down the **rich domain pattern** all aggregates must follow, the base type that gives them uniform domain-event collection and optimistic-locking semantics, and the application-service template that drives them.

## The base type

`common-domain` declares:

```java
public abstract class AggregateRoot<ID> {

    private final List<DomainEvent> domainEvents = new ArrayList<>();
    protected Long version;                            // null on first save

    public abstract ID id();

    protected void registerEvent(DomainEvent event) {
        domainEvents.add(event);
    }

    public List<DomainEvent> pullDomainEvents() {
        List<DomainEvent> drained = List.copyOf(domainEvents);
        domainEvents.clear();
        return drained;
    }

    public Long version() { return version; }
}
```

And:

```java
public interface DomainEvent {
    Instant occurredAt();
    UUID aggregateId();
}
```

Both live in `common-domain` so every context can extend / implement them. Pure Java — no Spring, no JPA.

## How aggregates use it

An aggregate is a class (not a record) extending `AggregateRoot<ID>`. Mutations are methods on the class that:

1. Validate preconditions (and throw a domain exception if violated).
2. Mutate internal state.
3. Call `registerEvent(new SomethingHappened(...))`.

Concrete shape for `Occurrence` after the refactor that implements this ADR:

```java
public class Occurrence extends AggregateRoot<UUID> {

    private final UUID id;
    private final UUID sabhaId;
    private LocalDate date;
    private OccurrenceState state;
    private final Map<UUID, AttendanceMarking> markings = new HashMap<>();

    public void open() {
        if (state != SCHEDULED && state != RESCHEDULED && state != CANCELLED) {
            throw new InvalidOccurrenceTransitionException(id, state, OPEN_FOR_MARKING);
        }
        state = OPEN_FOR_MARKING;
        registerEvent(new OccurrenceOpened(id, Instant.now()));
    }

    public void mark(UUID personId, boolean present, UUID markedBy) {
        if (state != OPEN_FOR_MARKING) {
            throw new OccurrenceNotOpenForMarkingException(id, state);
        }
        markings.put(personId, new AttendanceMarking(personId, present, markedBy));
        registerEvent(new AttendanceMarked(id, personId, present, markedBy, Instant.now()));
    }

    public void finalize() { ... }
    public void cancel() { ... }
    public void reschedule(LocalDate newDate) { ... }

    @Override public UUID id() { return id; }
}
```

`AttendanceMarking` is an **entity inside** the `Occurrence` aggregate (per Q8 of the design discussion that produced this ADR), not its own aggregate root. There is **no** `AttendanceMarkingRepository` — markings persist via `occurrenceRepository.save(occurrence)`, which cascades them.

## Domain services

Stateless operations that don't fit on one aggregate (e.g. a `UniqueUsernameChecker`, a `RosterEligibilityChecker` that compares a Person's demographics against a Sabha's kind) live in `*-domain-core` as **pure functions**:

- They take their inputs as parameters; they do **not** call repositories or any other port.
- If they need data the application service didn't have, the application service loads it first and passes it in.
- They return a value or throw a domain exception. They never mutate via side-channels.

This keeps `*-domain-core` free of driven-port dependencies. The application service is the only thing that talks to ports.

## The application-service template

Every application service in `*-application-service` follows the same four-step skeleton:

```java
@Service
public class MarkAttendanceApplicationService {

    private final CallerResolver callerResolver;        // port — common-domain
    private final OccurrenceRepository occurrences;     // port — attendance-application-service
    private final DomainEventPublisher events;          // port — common-domain

    @Transactional
    public void execute(UUID keycloakSubject, UUID occurrenceId, UUID personId, boolean present) {
        UUID markedBy = callerResolver.resolveUserId(keycloakSubject)
                .orElseThrow(() -> new CallerUnknownException(keycloakSubject));

        // 1. Load aggregate
        Occurrence occurrence = occurrences.findById(occurrenceId)
                .orElseThrow(() -> new OccurrenceNotFoundException(occurrenceId));

        // 2. Mutate via aggregate methods
        occurrence.mark(personId, present, markedBy);

        // 3. Save
        occurrences.save(occurrence);

        // 4. Publish drained events
        events.publishAll(occurrence.pullDomainEvents());
    }
}
```

Four steps. Always in this order. Any logic that doesn't fit one of these four steps is in the wrong place — it should be on the aggregate, in a domain service, or in an adapter.

## Optimistic locking

Two Sah-Sanchalaks can simultaneously mark different People on the same Occurrence. Per [ADR-0019](0019-bounded-context-module-taxonomy.md) the unit of write is the whole `Occurrence` aggregate (Q8 in the design discussion), so concurrent marks race each other. We resolve this with optimistic locking on the aggregate, not pessimistic locking:

- `Occurrence` carries a `version: Long` (on `AggregateRoot`).
- `JdbcOccurrenceRepository.save(occurrence)` writes `UPDATE occurrences SET ..., version = version + 1 WHERE id = ? AND version = ?`. If `rowsAffected == 0`, throw `OptimisticLockException` (a `common-domain` exception type).
- The application service catches `OptimisticLockException` and **retries the whole `load → mutate → save` block** up to **3 times** before giving up. The retry is *not* a generic Spring retry — it's a `for (int attempt = 0; attempt < 3; attempt++)` loop in the application-service method itself, so the load-mutate-save-publish sequence stays explicit and visible.
- On give-up, the use case throws `ConcurrentModificationException` (also from `common-domain`); the global `@RestControllerAdvice` maps it to HTTP 409.

Rationale for optimistic over pessimistic: marking attendance is a high-frequency, short-transaction operation; the conflict rate in practice is very low (two Karyakars tapping the same Occurrence at the same moment). Pessimistic locking would serialize all writes to the Occurrence; optimistic locking lets concurrent non-conflicting writes proceed and only pays the retry cost on actual conflict.

Rationale for retrying in the application service rather than letting the caller retry: the mobile app is offline-capable (ADR-0007); surfacing a 409 to the device for a transient in-server race would force the offline-sync code to handle a retryable failure that the server can handle itself.

## The anti-pattern this ADR rules out

Anything that looks like the *current* `Occurrence` + `MarkAttendanceUseCase` shape is forbidden going forward:

```java
// ANTI-PATTERN — do not write code that looks like this

public record Occurrence(UUID id, UUID sabhaId, LocalDate date, OccurrenceState state) {
    public boolean isOpenForMarking() {                 // anemic — query only
        return state == OPEN_FOR_MARKING;
    }
}

@Service
public class MarkAttendanceUseCase {
    @Transactional
    public void execute(UUID subject, UUID occurrenceId, UUID personId, boolean present) {
        Occurrence occurrence = occurrences.findById(occurrenceId).orElseThrow(...);
        if (!occurrence.isOpenForMarking()) {           // business rule in the wrong place
            throw new OccurrenceNotOpenForMarkingException(...);
        }
        markings.upsert(new AttendanceMarking(...));    // separate repository for an entity
    }
}
```

What's wrong with it, in this ADR's terms:

- `Occurrence` is a `record` — no mutation methods, no events, no `version`. It cannot enforce its own invariants.
- The "is the Occurrence open for marking?" rule lives in the use case. Adding a second caller (a CLI, a scheduled job, a different use case) duplicates the check — or worse, forgets it.
- `AttendanceMarking` has its own repository, treated as a top-level row. The aggregate boundary is not respected.
- No domain events emitted. Analytics has nothing to project from.
- No optimistic locking. Concurrent writes silently overwrite each other.

## Consequences

- The refactor of `Occurrence`, `AttendanceMarking`, `MarkAttendanceUseCase`, `JdbcOccurrenceRepository`, `JdbcAttendanceMarkingRepository`, and `AttendanceRestController` to fit this ADR is a separate PR. The current `slice-2b-roster-marking` branch ships the feature with the anemic shape; the refactor lands afterwards under the new module taxonomy from ADR-0019.
- Every new aggregate in any context extends `AggregateRoot<ID>`. PRs that introduce a domain type as a `record` should be questioned in review.
- Domain-event consumers (analytics projections, future cross-context flows) subscribe via a `DomainEventPublisher` port. The in-process implementation lives in `application-container` initially; a future ADR may move to an out-of-process bus.
- Every aggregate update path through an application service is wrapped in `@Transactional` and the four-step skeleton. New patterns require their own ADR.
