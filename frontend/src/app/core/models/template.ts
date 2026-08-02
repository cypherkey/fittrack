import { TemplateVisibility, WorkoutDifficulty } from './enums';

export interface TemplateSet {
  id: string;
  exerciseId: string;
  exerciseName: string;
  setNumber: number;
  reps: number | null;
  weightKg: number | null;
  durationSeconds: number | null;
  distanceMeters: number | null;
  notes: string | null;
}

export interface Template {
  id: string;
  userId: string;
  name: string;
  difficulty: WorkoutDifficulty | null;
  notes: string | null;
  visibility: TemplateVisibility;
  createdAt: string;
  updatedAt: string;
  setCount: number;
  sets: TemplateSet[];
}

export interface TemplateSetRequest {
  exerciseId: string;
  setNumber: number;
  reps?: number | null;
  weightKg?: number | null;
  durationSeconds?: number | null;
  distanceMeters?: number | null;
  notes?: string | null;
}

export interface TemplateRequest {
  name?: string | null;
  difficulty?: WorkoutDifficulty | null;
  notes?: string | null;
  visibility: TemplateVisibility;
  sets: TemplateSetRequest[];
}

export interface CloneTemplateRequest {
  name?: string | null;
}

export interface ReorderSetItem {
  setId: string;
  setNumber: number;
}

export interface ReorderSetsRequest {
  items: ReorderSetItem[];
}
