import { Request, Response } from "express";

import * as attendanceService from "./attendance.service";

export const getAllCoursesSummary = async (req: Request, res: Response) => {
  const userId = req.user!.userId;
  const data = await attendanceService.getAllCoursesSummary(userId);
  res.json(data);
};

export const getOverview = async (req: Request, res: Response) => {
  const userId = req.user!.userId;
  const data = await attendanceService.getOverview(userId);
  res.json(data);
};

export const getCourseAttendance = async (req: Request, res: Response) => {
  const userId = req.user!.userId;
  const courseId = req.params.courseId as string;
  const data = await attendanceService.getCourseAttendance(userId, courseId);
  res.json(data);
};

export const upsertRecord = async (req: Request, res: Response) => {
  const userId = req.user!.userId;
  const courseId = req.params.courseId as string;
  const data = await attendanceService.upsertRecord(userId, courseId, req.body);
  res.status(201).json(data);
};

export const updateRecord = async (req: Request, res: Response) => {
  const userId = req.user!.userId;
  const recordId = req.params.id as string;
  const data = await attendanceService.updateRecord(userId, recordId, req.body);
  res.json(data);
};

export const deleteRecord = async (req: Request, res: Response) => {
  const userId = req.user!.userId;
  const recordId = req.params.id as string;
  await attendanceService.removeRecord(userId, recordId);
  res.status(200).json({ success: true });
};

export const updateCourseSettings = async (req: Request, res: Response) => {
  const userId = req.user!.userId;
  const courseId = req.params.courseId as string;
  const data = await attendanceService.updateCourseSettings(
    userId,
    courseId,
    req.body
  );
  res.json(data);
};
