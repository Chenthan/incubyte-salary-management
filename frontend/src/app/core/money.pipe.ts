import { Pipe, PipeTransform } from '@angular/core';

/** Format server-rounded decimal strings without converting authoritative totals to floats. */
@Pipe({ name: 'money' })
export class MoneyPipe implements PipeTransform {
  transform(value: string | null | undefined, currency = 'USD'): string {
    if (value == null || !/^-?\d+(\.\d{1,2})?$/.test(value)) return '—';
    const [integer, fraction = ''] = value.split('.');
    return `${currency} ${integer.replace(/\B(?=(\d{3})+(?!\d))/g, ',')}.${fraction.padEnd(2, '0')}`;
  }
}
