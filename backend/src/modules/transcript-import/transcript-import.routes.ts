import { Router } from "express";
import multer from "multer";

import { parseTranscript, importTranscript } from "./transcript-import.controller";
import { authMiddleware } from "../../shared/middleware/auth.middleware";
import { validate } from "../../shared/middleware/validate.middleware";
import { importTranscriptSchema } from "./transcript-import.validator";
import { AppError } from "../../shared/errors/AppError";

const upload = multer({
  storage: multer.memoryStorage(),
  limits: { fileSize: 10 * 1024 * 1024 }, // 10MB yeter de artar bile
  fileFilter: (_req, file, cb) => {
    if (file.mimetype !== "application/pdf") {
      cb(AppError.badRequest("Sadece PDF dosyası yüklenebilir."));
      return;
    }
    cb(null, true);
  },
});

const router = Router();

router.use(authMiddleware);

// Adım 1: PDF'i yükle, satırları çıkar, KAYDETMEDEN önizleme döndür.
router.post("/parse", upload.single("file"), parseTranscript);

// Adım 2: Kullanıcının onayladığı (gerekirse düzenlediği) veriyi kaydet.
router.post("/import", validate(importTranscriptSchema), importTranscript);

export default router;
