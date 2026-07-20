import { Router } from "express";

import * as controller from "./gpa.controller";

import { authMiddleware } from "../../shared/middleware/auth.middleware";

const router = Router();

router.get("/", authMiddleware, controller.calculate);

export default router;