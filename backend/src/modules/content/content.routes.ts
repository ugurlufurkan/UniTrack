import { Router } from "express";

import { getAnnouncements, getFaqs, getPage, getTips } from "./content.controller";
import { authMiddleware } from "../../shared/middleware/auth.middleware";

const router = Router();

// Bu içerik admin panelinden (Next.js, ayrı uygulama) yönetiliyor; burası
// sadece mobil uygulamanın OKUMASI için. Kullanıcı girişli olsun diye
// authMiddleware'i koruduk (API'nin geri kalanıyla tutarlı olsun diye),
// ama içeriğin kendisi kullanıcıya özel değil, herkese aynı.
router.use(authMiddleware);

router.get("/announcements", getAnnouncements);
router.get("/faqs", getFaqs);
router.get("/tips", getTips);
router.get("/pages/:slug", getPage);

export default router;
