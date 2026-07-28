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
