import { Request, Response } from "express";

import * as settingsService from "./settings.service";

export const getSettings = async (req: Request, res: Response) => {
  const userId = req.user!.userId;
  const result = await settingsService.getSettings(userId);
  res.json(result);
};

export const updateSettings = async (req: Request, res: Response) => {
  const userId = req.user!.userId;
  const result = await settingsService.updateSettings(userId, req.body);
  res.json(result);
};
