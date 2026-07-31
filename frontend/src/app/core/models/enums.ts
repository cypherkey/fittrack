export enum RpeLevel {
  Easy = 'EASY',
  Challenging = 'CHALLENGING',
  Hard = 'HARD',
}

export enum WorkoutDifficulty {
  Easy = 'EASY',
  Medium = 'MEDIUM',
  Hard = 'HARD',
}

export enum TemplateVisibility {
  Private = 'PRIVATE',
  Public = 'PUBLIC',
}

export enum ExerciseLevel {
  Beginner = 'BEGINNER',
  Intermediate = 'INTERMEDIATE',
  Expert = 'EXPERT',
}

export enum ExerciseMechanic {
  Compound = 'COMPOUND',
  Isolation = 'ISOLATION',
}

export const RPE_LEVELS = Object.values(RpeLevel);
export const WORKOUT_DIFFICULTIES = Object.values(WorkoutDifficulty);
export const TEMPLATE_VISIBILITIES = Object.values(TemplateVisibility);
export const EXERCISE_LEVELS = Object.values(ExerciseLevel);
export const EXERCISE_MECHANICS = Object.values(ExerciseMechanic);

/** Catalog force values (stored lowercase; nullable on exercise). */
export const EXERCISE_FORCES = ['push', 'pull', 'static'] as const;
export type ExerciseForce = (typeof EXERCISE_FORCES)[number];

/** Catalog category values (stored as in free-exercise-db). */
export const EXERCISE_CATEGORIES = [
  'strength',
  'stretching',
  'plyometrics',
  'strongman',
  'powerlifting',
  'cardio',
  'olympic weightlifting',
] as const;
export type ExerciseCategory = (typeof EXERCISE_CATEGORIES)[number];

/** Bit flags matching backend `TrackedParameters`. */
export const TRACKED_PARAM = {
  REPS: 1,
  WEIGHT: 2,
  DURATION: 4,
  DISTANCE: 8,
} as const;

export const TRACKED_PARAM_OPTIONS = [
  { flag: TRACKED_PARAM.REPS, label: 'Reps' },
  { flag: TRACKED_PARAM.WEIGHT, label: 'Weight' },
  { flag: TRACKED_PARAM.DURATION, label: 'Duration' },
  { flag: TRACKED_PARAM.DISTANCE, label: 'Distance' },
] as const;

export const DEFAULT_TRACKED_PARAMETERS = TRACKED_PARAM.REPS | TRACKED_PARAM.WEIGHT;

export function hasTrackedParam(flags: number | null | undefined, flag: number): boolean {
  return ((flags ?? 0) & flag) !== 0;
}

export function toggleTrackedParam(flags: number | null | undefined, flag: number, on: boolean): number {
  const current = flags ?? 0;
  return on ? current | flag : current & ~flag;
}

export function trackedParamLabels(flags: number | null | undefined): string[] {
  return TRACKED_PARAM_OPTIONS.filter((o) => hasTrackedParam(flags, o.flag)).map((o) => o.label);
}
