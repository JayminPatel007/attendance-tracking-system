package org.sabha.analytics.applicationservice;

import java.util.UUID;

/**
 * The slice of the organisation a dashboard read serves (Slice 17). A
 * {@link RoleScoped} view is filtered by the caller's {@code role_assignments}
 * (the Slice 15 behaviour for every operational tier and the MK). A
 * {@link CityScoped} view is the Sant's universal-read exception: every
 * candidate in one City, regardless of the caller's formal assignment.
 * {@link NoCity} is a Sant who has not yet chosen a City — the dashboard shows
 * nothing until they pick.
 */
public sealed interface DashboardScope {

    record RoleScoped(UUID userId) implements DashboardScope {
    }

    record CityScoped(UUID cityId) implements DashboardScope {
    }

    record NoCity() implements DashboardScope {
    }
}
