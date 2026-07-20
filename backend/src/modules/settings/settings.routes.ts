import { Router } from "express";

import { getSettings, updateSettings } from "./settings.controller";
import { authMiddleware } from "../../shared/middleware/auth.middleware";
import { validate } from "../../shared/middleware/validate.middleware";
import { settingsUpdateSchema } from "../../shared/validation/settings.schema";

const router = Router();

router.use(authMiddleware);

// Cihazlar arası senkron edilen kişisel ayarlar (tema, hedef GANO, sınav dönemi).
router.get("/", getSettings);

router.patch("/", validate(settingsUpdateSchema), updateSettings);

export default router;
