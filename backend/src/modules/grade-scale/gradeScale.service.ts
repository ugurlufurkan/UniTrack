import * as repository from "./gradeScale.repository";
import { GradeBand } from "../../db/schema";

export const getDefault = (userId: string) => repository.getDefault(userId);

export const setDefault = (userId: string, gradeScale: GradeBand[] | null) =>
  repository.setDefault(userId, gradeScale);
