import { Request, Response } from "express";
import * as statisticsService from "./statistics.service";
import { StatisticsResponse } from "./statistics.types";

export const getStatistics = async (
  req: Request,
  res: Response<StatisticsResponse>
) => {
  const statistics = await statisticsService.getStatistics(
    req.user!.userId
  );

  res.json(statistics);
};