import { Request, Response } from "express";

import * as gradeScaleService from "./gradeScale.service";

export const getDefaultGradeScale = async (req: Request, res: Response) => {
  const userId = req.user!.userId;
  const result = await gradeScaleService.getDefault(userId);
  res.json(result);
};

export const setDefaultGradeScale = async (req: Request, res: Response) => {
  const userId = req.user!.userId;
  // gradeScale: null gonderilirse sistem varsayilanina donulur.
  const result = await gradeScaleService.setDefault(userId, req.body.gradeScale);
  res.json(result);
};
