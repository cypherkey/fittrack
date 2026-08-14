/** Weight is stored as kg in the API; convert for imperial display/input only. Distance stays meters. */

export const KG_TO_LB = 2.2046226218;

/** Round display weights to one decimal (matches 0.5 step inputs). */
export function roundWeight(value: number): number {
  return Math.round(value * 10) / 10;
}

export function kgToLb(kg: number): number {
  return roundWeight(kg * KG_TO_LB);
}

/** Convert lb → kg for API storage (two decimals to limit float noise). */
export function lbToKg(lb: number): number {
  return Math.round((lb / KG_TO_LB) * 100) / 100;
}

export function weightUnitLabel(useMetric: boolean): string {
  return useMetric ? 'kg' : 'lb';
}

/** Map a stored kg value to the value shown in the UI. */
export function toDisplayWeight(
  weightKg: number | null | undefined,
  useMetric: boolean,
): number | null {
  if (weightKg == null) {
    return null;
  }
  return useMetric ? weightKg : kgToLb(weightKg);
}

/** Map a UI weight value back to kg for the API. */
export function toStorageWeight(
  displayValue: number | null | undefined,
  useMetric: boolean,
): number | null {
  if (displayValue == null) {
    return null;
  }
  return useMetric ? displayValue : lbToKg(displayValue);
}

export function formatWeight(
  weightKg: number | null | undefined,
  useMetric: boolean,
  empty = '—',
): string {
  const display = toDisplayWeight(weightKg, useMetric);
  if (display == null) {
    return empty;
  }
  return `${display} ${weightUnitLabel(useMetric)}`;
}