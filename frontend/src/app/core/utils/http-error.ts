import { HttpErrorResponse } from '@angular/common/http';

export function errorMessage(err: unknown, fallback = 'Something went wrong'): string {
  if (err instanceof HttpErrorResponse) {
    const body = err.error;
    if (typeof body === 'string' && body.trim()) {
      return body;
    }
    if (body && typeof body === 'object') {
      const msg = (body as { message?: string; detail?: string }).message ?? (body as { detail?: string }).detail;
      if (msg) {
        return msg;
      }
    }
    if (err.statusText) {
      return err.statusText;
    }
  }
  if (err instanceof Error && err.message) {
    return err.message;
  }
  return fallback;
}
