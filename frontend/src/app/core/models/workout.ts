import { RpeLevel, WorkoutDifficulty } from './enums';

export interface WorkoutSet {
  id: string;
  exerciseId: string;
  exerciseName: string;
  setNumber: number;
  reps: number | null;
  weightKg: number | null;
  durationSeconds: number | null;
  distanceMeters: number | null;
  completed: boolean;
  rpe: RpeLevel | null;
  notes: string | null;
}

export interface Workout {
  id: string;
  userId: string;
  performedAt: string;
  name: string | null;
  durationSeconds: number | null;
  totalWeightLifted: number | null;
  difficulty: WorkoutDifficulty | null;
  notes: string | null;
  sourceTemplateId: string | null;
  createdAt: string;
  updatedAt: string;
  sets: WorkoutSet[];
}

export interface WorkoutSetRequest {
  exerciseId: string;
  setNumber: number;
  reps?: number | null;
  weightKg?: number | null;
  durationSeconds?: number | null;
  distanceMeters?: number | null;
  completed?: boolean | null;
  rpe?: RpeLevel | null;
  notes?: string | null;
}

export interface WorkoutRequest {
  performedAt: string;
  name?: string | null;
  durationSeconds?: number | null;
  difficulty?: WorkoutDifficulty | null;
  notes?: string | null;
  sourceTemplateId?: string | null;
  sets: WorkoutSetRequest[];
}

export interface WorkoutListParams {
  from?: string;
  to?: string;
}
