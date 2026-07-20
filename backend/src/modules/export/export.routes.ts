import { Router } from "express";

import { exportMyData } from "./export.controller";
import { authMiddleware } from "../../shared/middleware/auth.middleware";

const router = Router();

router.use(authMiddleware);

// Kullanıcının TÜM verisinin tek seferlik JSON dökümü ("yedeğimi indir").
router.get("/", exportMyData);

export default router;
