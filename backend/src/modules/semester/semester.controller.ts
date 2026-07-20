import { Request, Response } from "express";
import * as semesterService from "./semester.service";

export const getSemesters = async (
  req: Request,
  res: Response
) => {
  const semesters = await semesterService.getAll(
    req.user!.userId
  );

  res.json(semesters);
};

export const createSemester = async (
  req: Request,
  res: Response
) => {
  const semester = await semesterService.create(
    req.user!.userId,
    Number(req.body.year),
    req.body.term
  );

  res.status(201).json(semester);
};

export const updateSemester = async (
  req: Request,
  res: Response
) => {
  const semesterId = req.params.id as string;

  const semester = await semesterService.update(
    req.user!.userId,
    semesterId,
    Number(req.body.year),
    req.body.term
  );

  res.json(semester);
};

export const deleteSemester = async (
  req: Request,
  res: Response
) => {
  const semesterId = req.params.id as string;

  const result = await semesterService.remove(
    req.user!.userId,
    semesterId
  );

  res.json(result);
};