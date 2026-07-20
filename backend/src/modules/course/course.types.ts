import { GradeBand } from "../../db/schema";

export interface CourseComponentDto {
  id?: string;
  name: string;
  weight: number;
  score?: number | null;
}

export interface CreateCourseDto {
  semesterId: string;
  name: string;
  credit: number;
  components: CourseComponentDto[];
  gradeScale?: GradeBand[] | null;
}

export interface UpdateCourseDto {
  name?: string;
  credit?: number;
  components?: CourseComponentDto[];
  gradeScale?: GradeBand[] | null;
}
