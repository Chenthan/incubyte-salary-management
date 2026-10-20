import { HttpErrorResponse } from '@angular/common/http';
import { problem } from './api';

describe('API problem messages', () => {
  it('preserves server conflict details for recovery', () => {
    expect(
      problem(
        new HttpErrorResponse({
          status: 409,
          error: { status: 409, code: 'STALE_EMPLOYEE', detail: 'Reload before editing.' },
        }),
      ).code,
    ).toBe('STALE_EMPLOYEE');
  });
  it('gives useful messages for a lost connection and a non-JSON server failure', () => {
    expect(problem(new HttpErrorResponse({ status: 0 })).detail).toContain('Cannot reach');
    expect(
      problem(new HttpErrorResponse({ status: 502, error: '<html>upstream</html>' })).detail,
    ).not.toContain('<html>');
  });
});
