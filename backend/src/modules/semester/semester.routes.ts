import { Router } from "express";

import {
  getSemesters,
  createSemester,
  updateSemester,
  deleteSemester,
} from "./semester.controller";

import { authMiddleware } from "../../shared/middleware/auth.middleware";
import { validate } from "../../shared/middleware/validate.middleware";
import { createSemesterSchema, updateSemesterSchema } from "./semester.validator";

const router = Router();

router.use(authMiddleware);

router.get("/", getSemesters);

router.post("/", validate(createSemesterSchema), createSemester);

router.put("/:id", validate(updateSemesterSchema), updateSemester);

router.delete("/:id", deleteSemester);

export default router;