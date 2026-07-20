import { Router } from "express";

import { getTranscript } from "./transcript.controller";
import { authMiddleware } from "../../shared/middleware/auth.middleware";

const router = Router();

router.use(authMiddleware);

router.get("/", getTranscript);

export default router;