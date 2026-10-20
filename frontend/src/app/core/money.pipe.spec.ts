import { MoneyPipe } from './money.pipe';

describe('MoneyPipe', () => {
  const pipe = new MoneyPipe();
  it('keeps every digit in large server totals without a floating-point conversion', () => {
    expect(pipe.transform('99999999999999.99', 'INR')).toBe('INR 99,999,999,999,999.99');
  });
  it('distinguishes an unavailable statistic from a zero total', () => {
    expect(pipe.transform(null)).toBe('—');
    expect(pipe.transform('0.00')).toBe('USD 0.00');
  });
  it('rejects malformed amounts and never displays more precision than reported', () => {
    expect(pipe.transform('1.001')).toBe('—');
    expect(pipe.transform('invalid')).toBe('—');
    expect(pipe.transform('100.1', 'EUR')).toBe('EUR 100.10');
  });
});
