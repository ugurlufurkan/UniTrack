import { Router } from "express";

import { getDefaultGradeScale, setDefaultGradeScale } from "./gradeScale.controller";
import { authMiddleware } from "../../shared/middleware/auth.middleware";
import { validate } from "../../shared/middleware/validate.middleware";
import { gradeScaleUpdateSchema } from "../../shared/validation/gradeScale.schema";

const router = Router();

router.use(authMiddleware);

router.get("/default", getDefaultGradeScale);

router.put("/default", validate(gradeScaleUpdateSchema), setDefaultGradeScale);

export default router;
