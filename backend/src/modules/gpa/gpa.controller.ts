import { Request, Response } from "express";
import * as gpaService from "./gpa.service";

export const calculate = async (
  req: Request,
  res: Response
) => {
  const userId = req.user!.userId;

  const result = await gpaService.calculate(userId);

  res.json(result);
};