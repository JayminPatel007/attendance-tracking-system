package org.sabha.analytics.application;

import java.util.List;
import java.util.UUID;
import java.util.function.Function;

import org.sabha.analytics.applicationservice.CandidateRow;
import org.sabha.analytics.applicationservice.CityChip;
import org.sabha.analytics.applicationservice.CityChipQuery;
import org.sabha.analytics.applicationservice.DashboardAccess;
import org.sabha.analytics.applicationservice.DashboardOverview;
import org.sabha.analytics.applicationservice.DashboardQueries;
import org.sabha.analytics.applicationservice.DashboardScope;
import org.sabha.analytics.applicationservice.SabhaTree;
import org.sabha.analytics.applicationservice.SantCityPreferenceService;
import org.sabha.analytics.applicationservice.ThresholdAdmin;
import org.sabha.analytics.applicationservice.ThresholdConfig;
import org.sabha.analytics.domain.Thresholds;
import org.sabha.common.CallerAuthority;
import org.sabha.common.web.CurrentUser;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

/**
 * Re-engagement dashboard BFF for the Angular web shell (Slice 15, ADR-0010,
 * ADR-0022). Cookie/session authenticated; the caller arrives already resolved
 * to the local User by the edge (ADR-0030), which rejects an authenticated
 * subject with no local User (403). Every read is scoped to the caller's roles inside
 * {@link DashboardQueries}. Thresholds are readable by any resolved caller but
 * updatable only by the Madhyastha Karyalaya.
 */
@RestController
public class DashboardBffController {

    private final DashboardQueries queries;
    private final DashboardAccess access;
    private final CityChipQuery cityChip;
    private final SantCityPreferenceService cityPreference;
    private final ThresholdConfig thresholdConfig;
    private final ThresholdAdmin thresholdAdmin;

    public DashboardBffController(DashboardQueries queries,
                                  DashboardAccess access,
                                  CityChipQuery cityChip,
                                  SantCityPreferenceService cityPreference,
                                  ThresholdConfig thresholdConfig,
                                  ThresholdAdmin thresholdAdmin) {
        this.queries = queries;
        this.access = access;
        this.cityChip = cityChip;
        this.cityPreference = cityPreference;
        this.thresholdConfig = thresholdConfig;
        this.thresholdAdmin = thresholdAdmin;
    }

    @GetMapping("/bff/dashboard/overview")
    public ResponseEntity<DashboardOverview> overview(@CurrentUser CallerAuthority caller) {
        return forScope(caller, queries::overview);
    }

    @GetMapping("/bff/dashboard/people")
    public ResponseEntity<List<CandidateRow>> people(@CurrentUser CallerAuthority caller) {
        return forScope(caller, queries::people);
    }

    @GetMapping("/bff/dashboard/sabha-tree")
    public ResponseEntity<SabhaTree> sabhaTree(@CurrentUser CallerAuthority caller) {
        return forScope(caller, queries::sabhaTree);
    }

    /**
     * What the City chip should render (Slice 17): for a Sant, the City list and
     * their current pick; for everyone else an inert chip the web replaces with a
     * static scope indicator.
     */
    @GetMapping("/bff/dashboard/scope")
    public ResponseEntity<CityChip> scope(@CurrentUser CallerAuthority caller) {
        return ResponseEntity.ok(cityChip.forCaller(caller));
    }

    /**
     * A Sant picks a City (Slice 17): persists it as their default and refilters.
     * Non-Sant → 403 ({@code NotASantException}); unknown City → 404
     * ({@code CityNotFoundException}); both via the global handler.
     */
    @PostMapping("/bff/dashboard/city")
    public ResponseEntity<Void> chooseCity(@RequestBody ChooseCityRequest request,
                                           @CurrentUser CallerAuthority caller) {
        cityPreference.selectCity(caller, request.cityId());
        return ResponseEntity.noContent().build();
    }

    /**
     * Readable by any caller the system knows. {@code caller} is deliberately
     * unread, and since ADR-0032 binding it is no longer free: the edge now
     * resolves a whole {@link CallerAuthority}, so this endpoint pays a
     * <b>second</b> query, against {@code role_assignments}, whose result it
     * discards entirely.
     *
     * <p>It still pays it. What the bind buys is the {@code users}-row check —
     * an authenticated subject with no local User is refused here, exactly as it
     * was before ADR-0030 — and that check is worth one extra statement on an
     * endpoint nobody hits in a loop. The alternative was a second caller
     * parameter type so that zero-authority handlers could bind identity alone,
     * and ADR-0032 rejected that: a permanent fork in the handler population, a
     * decision for every future handler author, bought for <b>two</b> handlers out
     * of fifty-three. If that two ever grows past roughly fifteen percent of
     * caller-bound handlers, the trade flips and the migration is a widened
     * {@code supportsParameter}, not a redesign.</p>
     */
    @GetMapping("/bff/dashboard/thresholds")
    public ResponseEntity<Thresholds> thresholds(@CurrentUser CallerAuthority caller) {
        return ResponseEntity.ok(thresholdConfig.current());
    }

    @PutMapping("/bff/dashboard/thresholds")
    public ResponseEntity<Void> updateThresholds(@RequestBody ThresholdsRequest request,
                                                 @CurrentUser CallerAuthority caller) {
        if (!caller.isMadhyasthaKaryalaya()) {
            return ResponseEntity.status(403).build();
        }
        // Invalid thresholds surface as 422 via the domain invariant on Thresholds.
        thresholdAdmin.update(new Thresholds(request.candidate(), request.priority()), caller.userId());
        return ResponseEntity.noContent().build();
    }

    /** Resolves the caller's {@link DashboardScope} then runs a scope-filtered read. */
    private <T> ResponseEntity<T> forScope(CallerAuthority caller, Function<DashboardScope, T> read) {
        return ResponseEntity.ok(read.apply(access.viewFor(caller)));
    }

    public record ThresholdsRequest(int candidate, int priority) {
    }

    public record ChooseCityRequest(UUID cityId) {
    }
}
