import { Router } from "express";

import {
  getAllCoursesSummary,
  getOverview,
  getCourseAttendance,
  upsertRecord,
  updateRecord,
  deleteRecord,
  updateCourseSettings,
} from "./attendance.controller";

import { authMiddleware } from "../../shared/middleware/auth.middleware";
import { validate } from "../../shared/middleware/validate.middleware";
import {
  upsertAttendanceRecordSchema,
  updateAttendanceRecordSchema,
  updateAttendanceSettingsSchema,
} from "./attendance.validator";

const router = Router();

router.use(authMiddleware);

router.get("/overview", getOverview);
router.get("/courses", getAllCoursesSummary);
router.get("/courses/:courseId", getCourseAttendance);
router.post(
  "/courses/:courseId/records",
  validate(upsertAttendanceRecordSchema),
  upsertRecord
);
router.put(
  "/courses/:courseId/settings",
  validate(updateAttendanceSettingsSchema),
  updateCourseSettings
);

router.put("/records/:id", validate(updateAttendanceRecordSchema), updateRecord);
router.delete("/records/:id", deleteRecord);

export default router;
