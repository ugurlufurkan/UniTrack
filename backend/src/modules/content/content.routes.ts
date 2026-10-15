import { Router } from "express";

import { getAnnouncements, getFaqs, getPage, getTips } from "./content.controller";
import { authMiddleware } from "../../shared/middleware/auth.middleware";

const router = Router();

// Yasal metinler uygulama kurulmadan ve giriş yapılmadan da erişilebilir.
router.get("/pages/:slug", getPage);

// Mevcut içerik hem Android hem web istemcisi tarafından okunur.
router.use(authMiddleware);

router.get("/announcements", getAnnouncements);
router.get("/faqs", getFaqs);
router.get("/tips", getTips);

export default router;
