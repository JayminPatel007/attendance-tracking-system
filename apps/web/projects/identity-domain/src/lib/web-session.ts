import { Section } from './section';

/**
 * What the web shell needs to render after login (Slice 9): the User's display
 * name, whether they are a Madhyastha Karyalaya or Regional Team member, and the
 * {@link Section}s their authority unlocks. Frontend mirror of the backend
 * `WebSession` record; the JSON shape matches
 * `BffSessionController.WebSessionResponse`.
 */
export interface WebSession {
  username: string;
  madhyasthaKaryalaya: boolean;
  regionalTeam: boolean;
  sections: Section[];
}
