package org.sabha.sabha.application;

import java.util.List;
import java.util.UUID;

import org.sabha.common.RegionalTeamCityLookup;
import org.sabha.common.SanyojakZoneLookup;
import org.sabha.common.UserId;
import org.sabha.common.web.CurrentUser;
import org.sabha.sabha.applicationservice.SabhaKindLifecycleService;
import org.sabha.sabha.applicationservice.StructuralCreationService;
import org.sabha.sabha.applicationservice.StructuralQueries;
import org.sabha.sabha.domain.Demographic;
import org.sabha.sabha.domain.Track;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Structural-admin BFF endpoints (ADR-0009, ADR-0024, ADR-0022): the Angular web
 * shell creates and lists Cities and Sabha Kinds (MK), Zones (Regional Team,
 * within their City), and Kshetras (Sanyojak, within their Zone).
 * These are web requests authenticated by the server-side OIDC session; the
 * caller arrives already resolved to the local User by the edge (ADR-0030),
 * which is also where an authenticated subject with no local User is rejected
 * (403). Authority is arbitrated by the {@link StructuralCreationService}, also
 * 403 on denial via the global handler.
 */
@RestController
public class StructuralCreationController {

    private final StructuralCreationService creation;
    private final SabhaKindLifecycleService sabhaKindLifecycle;
    private final StructuralQueries queries;
    private final SanyojakZoneLookup sanyojakZones;
    private final RegionalTeamCityLookup regionalTeamCities;

    public StructuralCreationController(
            StructuralCreationService creation,
            SabhaKindLifecycleService sabhaKindLifecycle,
            StructuralQueries queries,
            SanyojakZoneLookup sanyojakZones,
            RegionalTeamCityLookup regionalTeamCities) {
        this.creation = creation;
        this.sabhaKindLifecycle = sabhaKindLifecycle;
        this.queries = queries;
        this.sanyojakZones = sanyojakZones;
        this.regionalTeamCities = regionalTeamCities;
    }

    @PostMapping("/bff/structure/cities")
    public ResponseEntity<CreatedResponse> createCity(
            @RequestBody CreateCityRequest req, @CurrentUser UserId caller) {
        return created(creation.createCity(caller, req.name()));
    }

    @PostMapping("/bff/structure/zones")
    public ResponseEntity<CreatedResponse> createZone(
            @RequestBody CreateZoneRequest req, @CurrentUser UserId caller) {
        return created(creation.createZone(caller, req.cityId(), req.name()));
    }

    @PostMapping("/bff/structure/sabha-kinds")
    public ResponseEntity<CreatedResponse> createSabhaKind(
            @RequestBody CreateSabhaKindRequest req, @CurrentUser UserId caller) {
        return created(creation.createSabhaKind(caller, req.demographic(), req.track()));
    }

    @PostMapping("/bff/structure/sabha-kinds/{id}/retire")
    public ResponseEntity<Void> retireSabhaKind(
            @PathVariable UUID id, @CurrentUser UserId caller) {
        sabhaKindLifecycle.retire(caller, id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/bff/structure/sabha-kinds/{id}/reactivate")
    public ResponseEntity<Void> reactivateSabhaKind(
            @PathVariable UUID id, @CurrentUser UserId caller) {
        sabhaKindLifecycle.reactivate(caller, id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/bff/structure/kshetras")
    public ResponseEntity<CreatedResponse> createKshetra(
            @RequestBody CreateKshetraRequest req, @CurrentUser UserId caller) {
        return created(creation.createKshetra(caller, req.zoneId(), req.name()));
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
    public ResponseEntity<List<StructuralQueries.ZoneView>> myZones(@CurrentUser UserId caller) {
        return ResponseEntity.ok(queries.zonesByIds(sanyojakZones.zonesOf(caller.value())));
    }

    @GetMapping("/bff/structure/my-cities")
    public ResponseEntity<List<StructuralQueries.CityView>> myCities(@CurrentUser UserId caller) {
        return ResponseEntity.ok(queries.citiesByIds(regionalTeamCities.citiesOf(caller.value())));
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

    public record CreatedResponse(
            @Schema(requiredMode = Schema.RequiredMode.REQUIRED) UUID id) {
    }
}
