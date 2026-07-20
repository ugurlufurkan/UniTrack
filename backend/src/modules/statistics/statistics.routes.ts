import { Router } from "express";

import { getStatistics } from "./statistics.controller";
import { authMiddleware } from "../../shared/middleware/auth.middleware";

const router = Router();

router.use(authMiddleware);

router.get("/", getStatistics);

export default router;