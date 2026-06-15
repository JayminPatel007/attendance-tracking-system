package org.sabha.sabha.application;

import java.util.List;
import java.util.UUID;

import org.sabha.common.CallerResolver;
import org.sabha.common.SanyojakZoneLookup;
import org.sabha.sabha.applicationservice.StructuralCreationService;
import org.sabha.sabha.applicationservice.StructuralQueries;
import org.sabha.sabha.domain.Demographic;
import org.sabha.sabha.domain.Track;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Structural-admin BFF endpoints (ADR-0009, ADR-0022): the Angular web shell
 * creates and lists Cities, Zones, Sabha Kinds (MK) and Kshetras (Sanyojak).
 * These are web requests authenticated by the server-side OIDC session, so the
 * caller is the Keycloak subject in {@link Authentication#getName()}, resolved
 * to the local User via {@link CallerResolver}. Authority is arbitrated by the
 * {@link StructuralCreationService} (403 on denial via the global handler); an
 * authenticated subject with no local User is itself unauthorized (403).
 */
@RestController
public class StructuralCreationController {

    private final StructuralCreationService creation;
    private final StructuralQueries queries;
    private final CallerResolver callers;
    private final SanyojakZoneLookup sanyojakZones;

    public StructuralCreationController(
            StructuralCreationService creation,
            StructuralQueries queries,
            CallerResolver callers,
            SanyojakZoneLookup sanyojakZones) {
        this.creation = creation;
        this.queries = queries;
        this.callers = callers;
        this.sanyojakZones = sanyojakZones;
    }

    @PostMapping("/bff/structure/cities")
    public ResponseEntity<CreatedResponse> createCity(
            @RequestBody CreateCityRequest req, Authentication authentication) {
        return created(creation.createCity(requireCaller(authentication), req.name()));
    }

    @PostMapping("/bff/structure/zones")
    public ResponseEntity<CreatedResponse> createZone(
            @RequestBody CreateZoneRequest req, Authentication authentication) {
        return created(creation.createZone(requireCaller(authentication), req.cityId(), req.name()));
    }

    @PostMapping("/bff/structure/sabha-kinds")
    public ResponseEntity<CreatedResponse> createSabhaKind(
            @RequestBody CreateSabhaKindRequest req, Authentication authentication) {
        return created(creation.createSabhaKind(requireCaller(authentication), req.demographic(), req.track()));
    }

    @PostMapping("/bff/structure/kshetras")
    public ResponseEntity<CreatedResponse> createKshetra(
            @RequestBody CreateKshetraRequest req, Authentication authentication) {
        return created(creation.createKshetra(requireCaller(authentication), req.zoneId(), req.name()));
    }

    @GetMapping("/bff/structure/cities")
    public List<StructuralQueries.CityView> listCities() {
        return queries.listCities();
    }

    @GetMapping("/bff/structure/zones")
    public List<StructuralQueries.ZoneView> listZones() {
        return queries.listZones();
    }

    @GetMapping("/bff/structure/sabha-kinds")
    public List<StructuralQueries.SabhaKindView> listSabhaKinds() {
        return queries.listSabhaKinds();
    }

    @GetMapping("/bff/structure/kshetras")
    public List<StructuralQueries.KshetraView> listKshetras(@RequestParam UUID zoneId) {
        return queries.listKshetras(zoneId);
    }

    @GetMapping("/bff/structure/my-zones")
    public ResponseEntity<List<StructuralQueries.ZoneView>> myZones(Authentication authentication) {
        UUID userId = requireCaller(authentication);
        return ResponseEntity.ok(queries.zonesByIds(sanyojakZones.zonesOf(userId)));
    }

    private UUID requireCaller(Authentication authentication) {
        return callers.requireUserId(UUID.fromString(authentication.getName()));
    }

    private static ResponseEntity<CreatedResponse> created(UUID id) {
        return ResponseEntity.status(201).body(new CreatedResponse(id));
    }

    public record CreateCityRequest(String name) {
    }

    public record CreateZoneRequest(UUID cityId, String name) {
    }

    public record CreateSabhaKindRequest(Demographic demographic, Track track) {
    }

    public record CreateKshetraRequest(UUID zoneId, String name) {
    }

    public record CreatedResponse(UUID id) {
    }
}
