import { Request, Response } from "express";

import * as exportService from "./export.service";

export const exportMyData = async (req: Request, res: Response) => {
  const userId = req.user!.userId;
  const data = await exportService.exportAllUserData(userId);

  const filename = `unitrack-yedek-${new Date().toISOString().slice(0, 10)}.json`;

  res.setHeader("Content-Type", "application/json; charset=utf-8");
  res.setHeader("Content-Disposition", `attachment; filename="${filename}"`);
  res.json(data);
};
