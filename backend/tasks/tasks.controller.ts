import { Request, Response } from "express";

import * as tasksService from "./tasks.service";

export const findAll = async (req: Request, res: Response) => {
  const userId = req.user!.userId;
  const { status, type, courseId } = req.query;

  const tasks = await tasksService.findAll(userId, {
    status: status as any,
    type: type as any,
    courseId: courseId as string | undefined,
  });
  res.json(tasks);
};

export const getById = async (req: Request, res: Response) => {
  const userId = req.user!.userId;
  const taskId = req.params.taskId as string;
  const task = await tasksService.getById(userId, taskId);
  res.json(task);
};

export const create = async (req: Request, res: Response) => {
  const userId = req.user!.userId;
  const task = await tasksService.create(userId, req.body);
  res.status(201).json(task);
};

export const update = async (req: Request, res: Response) => {
  const userId = req.user!.userId;
  const taskId = req.params.taskId as string;
  const task = await tasksService.update(userId, taskId, req.body);
  res.json(task);
};

export const remove = async (req: Request, res: Response) => {
  const userId = req.user!.userId;
  const taskId = req.params.taskId as string;
  await tasksService.remove(userId, taskId);
  res.status(204).send();
};

export const toggleStatus = async (req: Request, res: Response) => {
  const userId = req.user!.userId;
  const taskId = req.params.taskId as string;
  const task = await tasksService.toggleStatus(userId, taskId);
  res.json(task);
};

export const addChecklistItem = async (req: Request, res: Response) => {
  const userId = req.user!.userId;
  const taskId = req.params.taskId as string;
  const task = await tasksService.addChecklistItem(userId, taskId, req.body);
  res.status(201).json(task);
};

export const updateChecklistItem = async (req: Request, res: Response) => {
  const userId = req.user!.userId;
  const taskId = req.params.taskId as string;
  const itemId = req.params.itemId as string;
  const task = await tasksService.updateChecklistItem(
    userId,
    taskId,
    itemId,
    req.body
  );
  res.json(task);
};

export const removeChecklistItem = async (req: Request, res: Response) => {
  const userId = req.user!.userId;
  const taskId = req.params.taskId as string;
  const itemId = req.params.itemId as string;
  const task = await tasksService.removeChecklistItem(userId, taskId, itemId);
  res.json(task);
};
