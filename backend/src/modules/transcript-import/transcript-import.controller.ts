import { Request, Response } from "express";

import * as transcriptImportService from "./transcript-import.service";
import { AppError } from "../../shared/errors/AppError";

export const parseTranscript = async (req: Request, res: Response) => {
  const file = (req as Request & { file?: Express.Multer.File }).file;

  if (!file) {
    throw AppError.badRequest("PDF dosyası bulunamadı (form alanı adı: 'file').");
  }

  const preview = await transcriptImportService.parsePdf(file.buffer);

  res.json({ semesters: preview });
};

export const importTranscript = async (req: Request, res: Response) => {
  const userId = req.user!.userId;

  const result = await transcriptImportService.importSemesters(
    userId,
    req.body.semesters
  );

  res.status(201).json(result);
};
