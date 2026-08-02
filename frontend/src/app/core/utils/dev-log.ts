import { environment } from '../../../environments/environment';

/** console.debug only when not production (ng serve / development config). */
export function devLog(...args: unknown[]): void {
  if (!environment.production) {
    // eslint-disable-next-line no-console
    console.debug(...args);
  }
}
