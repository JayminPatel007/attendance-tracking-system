package org.sabha.common;

/**
 * The operational (Karyakar) role tiers baked into the organization's hierarchy
 * (ADR-0005). These are the roles that can hold an entry in {@code
 * role_assignments}; the oversight tiers Sant and Madhyastha Karyalaya are
 * modelled elsewhere and are deliberately absent here.
 *
 * <p>Sabha-scoped roles ({@link #SANCHALAK}, {@link #SAH_SANCHALAK}, {@link
 * #NIRIKSHAK}) attach to a specific Sabha; the Kshetra-level tiers attach to a
 * Kshetra.</p>
 */
public enum Role {
    SANCHALAK,
    SAH_SANCHALAK,
    NIRIKSHAK,
    NIRDESHAK,
    SAH_NIRDESHAK,
    SANYOJAK;

    /**
     * The Kshetra tiers permitted to reopen a Finalized Occurrence (ADR-0001):
     * Nirikshak, Nirdeshak, and Sah-Nirdeshak — explicitly not the Sanchalak who
     * owns shaping, nor the oversight tiers (Sanyojak, Sant, MK). This is the one
     * canonical definition of the reopen authority set; the attendance
     * Authorization Engine (write path) and the web shell's section visibility
     * both read it, and the Occurrence-reopen read model grants exactly these tiers
     * by passing the set to {@link CallerVisibility#tiersFor(java.util.Set)}.
     */
    public static final java.util.Set<Role> REOPEN_TIERS =
            java.util.Set.of(NIRIKSHAK, NIRDESHAK, SAH_NIRDESHAK);
}
