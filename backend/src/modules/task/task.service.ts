import * as taskRepository from "./task.repository";
import {
  CreateChecklistItemDto,
  ReorderChecklistDto,
  UpdateChecklistItemDto,
} from "./task.types";

export const getAllTasks = async (
  userId: string,
  filters?: { status?: string; type?: string }
) => taskRepository.findAllTasks(userId, filters);

export const getTaskById = async (userId: string, eventId: string) =>
  taskRepository.findTaskById(userId, eventId);

export const createChecklistItem = async (
  userId: string,
  eventId: string,
  data: CreateChecklistItemDto
) => taskRepository.createChecklistItem(userId, eventId, data);

export const updateChecklistItem = async (
  userId: string,
  itemId: string,
  data: UpdateChecklistItemDto
) => taskRepository.updateChecklistItem(userId, itemId, data);

export const removeChecklistItem = async (userId: string, itemId: string) =>
  taskRepository.removeChecklistItem(userId, itemId);

export const reorderChecklist = async (
  userId: string,
  eventId: string,
  data: ReorderChecklistDto
) => taskRepository.reorderChecklist(userId, eventId, data);
