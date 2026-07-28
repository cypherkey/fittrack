import { ExerciseLevel, ExerciseMechanic } from './enums';

export interface ExerciseMuscle {
  muscleId: string;
  name: string;
  primary: boolean;
}

export interface ExerciseImage {
  imageId: string;
  path: string;
  altText: string | null;
  sortOrder: number;
}

export interface Exercise {
  id: string;
  name: string;
  force: string | null;
  level: ExerciseLevel;
  mechanic: ExerciseMechanic | null;
  equipmentId: string | null;
  equipmentName: string | null;
  instructions: string | null;
  category: string | null;
  trackedParameters: number;
  custom: boolean;
  addedById: string | null;
  muscles: ExerciseMuscle[];
  images: ExerciseImage[];
}

export interface MuscleLink {
  muscleId: string;
  primary: boolean;
}

export interface ExerciseRequest {
  name: string;
  force?: string | null;
  level: ExerciseLevel;
  mechanic?: ExerciseMechanic | null;
  equipmentId?: string | null;
  instructions?: string | null;
  category?: string | null;
  trackedParameters?: number | null;
  muscles?: MuscleLink[];
}

export interface ExerciseListParams {
  q?: string;
  muscle?: string;
  equipment?: string;
  category?: string;
  customOnly?: boolean;
  page?: number;
  size?: number;
}
