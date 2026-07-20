import * as repository from "./course.repository";
import { CreateCourseDto, UpdateCourseDto } from "./course.types";

export const getAll = async (userId: string) => repository.findAll(userId);

export const create = async (userId: string, data: CreateCourseDto) =>
  repository.create(userId, data);

export const update = async (userId: string, courseId: string, data: UpdateCourseDto) =>
  repository.update(userId, courseId, data);

export const remove = async (userId: string, courseId: string) =>
  repository.remove(userId, courseId);