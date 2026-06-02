import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { Section, SessionService } from 'identity-domain';

/**
 * Route guard for the shell's section routes (Slice 9). The route's
 * `data.section` must be in the signed-in user's visible set (from the BFF
 * session); otherwise the user is bounced back to the always-available
 * dashboard. Authority decides reachability, not just sidebar visibility.
 */
export const sectionGuard: CanActivateFn = (route) => {
  const sessions = inject(SessionService);
  const router = inject(Router);

  const section = route.data['section'] as Section;
  const current = sessions.session();
  if (current?.sections.includes(section)) {
    return true;
  }
  return router.parseUrl('/dashboard');
};
