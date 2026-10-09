const ADULT_AGE_YEARS = 18;

/** True when the date of birth makes the student under 18 on the given day (spec 2.7). */
export function isMinor(dateOfBirth: string, today: Date = new Date()): boolean {
  const birth = new Date(dateOfBirth);
  if (Number.isNaN(birth.getTime())) {
    return false;
  }
  const adultOn = new Date(birth);
  adultOn.setFullYear(birth.getFullYear() + ADULT_AGE_YEARS);
  return adultOn > today;
}
