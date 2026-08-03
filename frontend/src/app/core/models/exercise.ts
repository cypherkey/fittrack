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
  contentType: string | null;
  contentBase64: string | null;
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
  videoUrl: string | null;
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
  videoUrl?: string | null;
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

/** Build a data-URL for an API exercise image, or null if bytes are missing. */
export function exerciseImageSrc(image: ExerciseImage | null | undefined): string | null {
  if (!image?.contentBase64) {
    return null;
  }
  const type = image.contentType || 'image/jpeg';
  return `data:${type};base64,${image.contentBase64}`;
}
