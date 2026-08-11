import { RpeLevel, WorkoutDifficulty } from './enums';

export interface WorkoutSet {
  id: string;
  exerciseId: string;
  exerciseName: string;
  trackedParameters: number;
  setNumber: number;
  reps: number | null;
  weightKg: number | null;
  durationSeconds: number | null;
  distanceMeters: number | null;
  completed: boolean;
  rpe: RpeLevel | null;
  notes: string | null;
}

export interface WorkoutSetPatchRequest {
  completed?: boolean | null;
  reps?: number | null;
  weightKg?: number | null;
  durationSeconds?: number | null;
  distanceMeters?: number | null;
  rpe?: RpeLevel | null;
  notes?: string | null;
}

export interface Workout {
  id: string;
  userId: string;
  startedAt: string | null;
  endedAt: string | null;
  name: string | null;
  completed: boolean;
  useMetric: boolean;
  totalWeightLifted: number | null;
  difficulty: WorkoutDifficulty | null;
  notes: string | null;
  sourceTemplateId: string | null;
  createdAt: string;
  updatedAt: string;
  setCount: number;
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
  startedAt?: string | null;
  endedAt?: string | null;
  name?: string | null;
  completed?: boolean | null;
  useMetric?: boolean | null;
  difficulty?: WorkoutDifficulty | null;
  notes?: string | null;
  sourceTemplateId?: string | null;
  sets: WorkoutSetRequest[];
}

export interface WorkoutListParams {
  from?: string;
  to?: string;
}
