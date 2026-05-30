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
    SANYOJAK
}
