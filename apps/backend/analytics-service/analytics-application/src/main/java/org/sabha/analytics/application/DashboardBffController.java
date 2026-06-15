package org.sabha.analytics.application;

import java.util.List;
import java.util.UUID;
import java.util.function.Function;

import org.sabha.analytics.applicationservice.CandidateRow;
import org.sabha.analytics.applicationservice.DashboardAccess;
import org.sabha.analytics.applicationservice.DashboardOverview;
import org.sabha.analytics.applicationservice.DashboardQueries;
import org.sabha.analytics.applicationservice.DashboardScope;
import org.sabha.analytics.applicationservice.SabhaTree;
import org.sabha.analytics.applicationservice.ThresholdAdmin;
import org.sabha.analytics.applicationservice.ThresholdConfig;
import org.sabha.analytics.domain.Thresholds;
import org.sabha.common.CallerResolver;
import org.sabha.common.MadhyasthaKaryalayaLookup;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

/**
 * Re-engagement dashboard BFF for the Angular web shell (Slice 15, ADR-0010,
 * ADR-0022). Cookie/session authenticated, so the caller is the Keycloak subject
 * in {@link Authentication#getName()}; an authenticated subject with no local User
 * is unauthorized (403). Every read is scoped to the caller's roles inside
 * {@link DashboardQueries}. Thresholds are readable by any resolved caller but
 * updatable only by the Madhyastha Karyalaya.
 */
@RestController
public class DashboardBffController {

    private final DashboardQueries queries;
    private final DashboardAccess access;
    private final ThresholdConfig thresholdConfig;
    private final ThresholdAdmin thresholdAdmin;
    private final CallerResolver callers;
    private final MadhyasthaKaryalayaLookup madhyasthaKaryalaya;

    public DashboardBffController(DashboardQueries queries,
                                  DashboardAccess access,
                                  ThresholdConfig thresholdConfig,
                                  ThresholdAdmin thresholdAdmin,
                                  CallerResolver callers,
                                  MadhyasthaKaryalayaLookup madhyasthaKaryalaya) {
        this.queries = queries;
        this.access = access;
        this.thresholdConfig = thresholdConfig;
        this.thresholdAdmin = thresholdAdmin;
        this.callers = callers;
        this.madhyasthaKaryalaya = madhyasthaKaryalaya;
    }

    @GetMapping("/bff/dashboard/overview")
    public ResponseEntity<DashboardOverview> overview(Authentication authentication) {
        return forScope(authentication, queries::overview);
    }

    @GetMapping("/bff/dashboard/people")
    public ResponseEntity<List<CandidateRow>> people(Authentication authentication) {
        return forScope(authentication, queries::people);
    }

    @GetMapping("/bff/dashboard/sabha-tree")
    public ResponseEntity<SabhaTree> sabhaTree(Authentication authentication) {
        return forScope(authentication, queries::sabhaTree);
    }

    /**
     * What the City chip should render (Slice 17): for a Sant, the City list and
     * their current pick; for everyone else an inert chip the web replaces with a
     * static scope indicator.
     */
    @GetMapping("/bff/dashboard/scope")
    public ResponseEntity<DashboardAccess.CityChip> scope(Authentication authentication) {
        return forCaller(authentication, access::cityChip);
    }

    /**
     * A Sant picks a City (Slice 17): persists it as their default and refilters.
     * Non-Sant → 403 ({@code NotASantException}); unknown City → 404
     * ({@code CityNotFoundException}); both via the global handler.
     */
    @PostMapping("/bff/dashboard/city")
    public ResponseEntity<Void> chooseCity(@RequestBody ChooseCityRequest request,
                                           Authentication authentication) {
        UUID subject = UUID.fromString(authentication.getName());
        access.selectCity(callers.requireUserId(subject), request.cityId());
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/bff/dashboard/thresholds")
    public ResponseEntity<Thresholds> thresholds(Authentication authentication) {
        return forCaller(authentication, userId -> thresholdConfig.current());
    }

    @PutMapping("/bff/dashboard/thresholds")
    public ResponseEntity<Void> updateThresholds(@RequestBody ThresholdsRequest request,
                                                 Authentication authentication) {
        UUID userId = callers.requireUserId(UUID.fromString(authentication.getName()));
        if (!madhyasthaKaryalaya.isMember(userId)) {
            return ResponseEntity.status(403).build();
        }
        // Invalid thresholds surface as 422 via the domain invariant on Thresholds.
        thresholdAdmin.update(new Thresholds(request.candidate(), request.priority()), userId);
        return ResponseEntity.noContent().build();
    }

    private <T> ResponseEntity<T> forCaller(Authentication authentication, Function<UUID, T> read) {
        UUID userId = callers.requireUserId(UUID.fromString(authentication.getName()));
        return ResponseEntity.ok(read.apply(userId));
    }

    /** Resolves the caller's {@link DashboardScope} then runs a scope-filtered read. */
    private <T> ResponseEntity<T> forScope(Authentication authentication, Function<DashboardScope, T> read) {
        return forCaller(authentication, userId -> read.apply(access.viewFor(userId)));
    }

    public record ThresholdsRequest(int candidate, int priority) {
    }

    public record ChooseCityRequest(UUID cityId) {
    }
}
