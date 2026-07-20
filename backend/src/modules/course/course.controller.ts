import { Request, Response } from "express";

import * as courseService from "./course.service";

export const getAllCourses = async (req: Request, res: Response) => {
  const userId = req.user!.userId;
  const courses = await courseService.getAll(userId);
  res.json(courses);
};

export const createCourse = async (req: Request, res: Response) => {
  const userId = req.user!.userId;
  const course = await courseService.create(userId, {
    semesterId: req.body.semesterId,
    name: req.body.name,
    credit: req.body.credit,
    components: req.body.components,
    gradeScale: req.body.gradeScale,
  });
  res.status(201).json(course);
};

export const updateCourse = async (req: Request, res: Response) => {
  const userId = req.user!.userId;
  const courseId = req.params.id as string;
  const course = await courseService.update(userId, courseId, {
    name: req.body.name,
    credit: req.body.credit,
    components: req.body.components,
    gradeScale: req.body.gradeScale,
  });
  res.json(course);
};

export const deleteCourse = async (req: Request, res: Response) => {
  const userId = req.user!.userId;
  const courseId = req.params.id as string;
  await courseService.remove(userId, courseId);
  res.status(200).json({ success: true });
};
