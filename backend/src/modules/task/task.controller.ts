import { Request, Response } from "express";

import * as taskService from "./task.service";

export const getAllTasks = async (req: Request, res: Response) => {
  const userId = req.user!.userId;
  const { status, type } = req.query;

  const tasks = await taskService.getAllTasks(userId, {
    status: status as string | undefined,
    type: type as string | undefined,
  });

  res.json(tasks);
};

export const getTaskById = async (req: Request, res: Response) => {
  const userId = req.user!.userId;
  const task = await taskService.getTaskById(userId, req.params.id as string);
  res.json(task);
};

export const createChecklistItem = async (req: Request, res: Response) => {
  const userId = req.user!.userId;
  const item = await taskService.createChecklistItem(
    userId,
    req.params.id as string,
    req.body
  );
  res.status(201).json(item);
};

export const updateChecklistItem = async (req: Request, res: Response) => {
  const userId = req.user!.userId;
  const item = await taskService.updateChecklistItem(
    userId,
    req.params.itemId as string,
    req.body
  );
  res.json(item);
};

export const deleteChecklistItem = async (req: Request, res: Response) => {
  const userId = req.user!.userId;
  await taskService.removeChecklistItem(userId, req.params.itemId as string);
  res.status(200).json({ success: true });
};

export const reorderChecklist = async (req: Request, res: Response) => {
  const userId = req.user!.userId;
  const items = await taskService.reorderChecklist(
    userId,
    req.params.id as string,
    req.body
  );
  res.json(items);
};
