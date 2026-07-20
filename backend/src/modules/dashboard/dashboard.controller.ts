import { Request, Response } from "express";
import * as dashboardService from "./dashboard.service";

export const getDashboard = async (
  req: Request,
  res: Response
) => {
  const dashboard = await dashboardService.getDashboard(
    req.user!.userId
  );

  res.json(dashboard);
};