package org.sabha.analytics.application;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;

import org.sabha.analytics.applicationservice.CandidateRow;
import org.sabha.analytics.applicationservice.DashboardOverview;
import org.sabha.analytics.applicationservice.DashboardQueries;
import org.sabha.analytics.applicationservice.SabhaTree;
import org.sabha.analytics.applicationservice.ThresholdAdmin;
import org.sabha.analytics.applicationservice.ThresholdConfig;
import org.sabha.analytics.domain.Thresholds;
import org.sabha.common.CallerResolver;
import org.sabha.common.MadhyasthaKaryalayaLookup;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
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
    private final ThresholdConfig thresholdConfig;
    private final ThresholdAdmin thresholdAdmin;
    private final CallerResolver callers;
    private final MadhyasthaKaryalayaLookup madhyasthaKaryalaya;

    public DashboardBffController(DashboardQueries queries,
                                  ThresholdConfig thresholdConfig,
                                  ThresholdAdmin thresholdAdmin,
                                  CallerResolver callers,
                                  MadhyasthaKaryalayaLookup madhyasthaKaryalaya) {
        this.queries = queries;
        this.thresholdConfig = thresholdConfig;
        this.thresholdAdmin = thresholdAdmin;
        this.callers = callers;
        this.madhyasthaKaryalaya = madhyasthaKaryalaya;
    }

    @GetMapping("/bff/dashboard/overview")
    public ResponseEntity<DashboardOverview> overview(Authentication authentication) {
        return forCaller(authentication, queries::overview);
    }

    @GetMapping("/bff/dashboard/people")
    public ResponseEntity<List<CandidateRow>> people(Authentication authentication) {
        return forCaller(authentication, queries::people);
    }

    @GetMapping("/bff/dashboard/sabha-tree")
    public ResponseEntity<SabhaTree> sabhaTree(Authentication authentication) {
        return forCaller(authentication, queries::sabhaTree);
    }

    @GetMapping("/bff/dashboard/thresholds")
    public ResponseEntity<Thresholds> thresholds(Authentication authentication) {
        return forCaller(authentication, userId -> thresholdConfig.current());
    }

    @PutMapping("/bff/dashboard/thresholds")
    public ResponseEntity<Void> updateThresholds(@RequestBody ThresholdsRequest request,
                                                 Authentication authentication) {
        UUID subject = UUID.fromString(authentication.getName());
        Optional<UUID> mkMember = callers.resolveUserId(subject).filter(madhyasthaKaryalaya::isMember);
        if (mkMember.isEmpty()) {
            return ResponseEntity.status(403).build();
        }
        // Invalid thresholds surface as 422 via the domain invariant on Thresholds.
        thresholdAdmin.update(new Thresholds(request.candidate(), request.priority()), mkMember.get());
        return ResponseEntity.noContent().build();
    }

    private <T> ResponseEntity<T> forCaller(Authentication authentication, Function<UUID, T> read) {
        UUID subject = UUID.fromString(authentication.getName());
        return callers.resolveUserId(subject)
                .map(read)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.status(403).build());
    }

    public record ThresholdsRequest(int candidate, int priority) {
    }
}
