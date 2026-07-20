import * as tasksRepository from "./tasks.repository";
import {
  CreateChecklistItemDto,
  CreateTaskDto,
  TaskFilters,
  UpdateChecklistItemDto,
  UpdateTaskDto,
} from "./tasks.types";

export const findAll = async (userId: string, filters?: TaskFilters) =>
  tasksRepository.findAll(userId, filters);

export const getById = async (userId: string, taskId: string) =>
  tasksRepository.getById(userId, taskId);

export const create = async (userId: string, data: CreateTaskDto) =>
  tasksRepository.create(userId, data);

export const update = async (
  userId: string,
  taskId: string,
  data: UpdateTaskDto
) => tasksRepository.update(userId, taskId, data);

export const remove = async (userId: string, taskId: string) =>
  tasksRepository.remove(userId, taskId);

export const toggleStatus = async (userId: string, taskId: string) =>
  tasksRepository.toggleStatus(userId, taskId);

export const addChecklistItem = async (
  userId: string,
  taskId: string,
  data: CreateChecklistItemDto
) => tasksRepository.addChecklistItem(userId, taskId, data);

export const updateChecklistItem = async (
  userId: string,
  taskId: string,
  itemId: string,
  data: UpdateChecklistItemDto
) => tasksRepository.updateChecklistItem(userId, taskId, itemId, data);

export const removeChecklistItem = async (
  userId: string,
  taskId: string,
  itemId: string
) => tasksRepository.removeChecklistItem(userId, taskId, itemId);
