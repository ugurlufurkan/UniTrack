import { Router } from "express";

import {
  getAllCourses,
  createCourse,
  updateCourse,
  deleteCourse,
} from "./course.controller";

import { authMiddleware } from "../../shared/middleware/auth.middleware";
import { validate } from "../../shared/middleware/validate.middleware";
import { createCourseSchema, updateCourseSchema } from "./course.validator";

const router = Router();

router.use(authMiddleware);

router.get("/", getAllCourses);

router.post("/", validate(createCourseSchema), createCourse);

router.put("/:id", validate(updateCourseSchema), updateCourse);

router.delete("/:id", deleteCourse);

export default router;
