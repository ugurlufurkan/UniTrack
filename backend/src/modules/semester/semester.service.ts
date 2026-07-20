import * as repository from "./semester.repository";
import { AppError } from "../../shared/errors/AppError";

export const getAll = async (userId: string) => {
  return repository.findAll(userId);
};

export const create = async (
  userId: string,
  year: number,
  term: string
) => {
  const exists = await repository.exists(
    userId,
    year,
    term
  );

  if (exists) {
    throw AppError.conflict("Bu dönem zaten mevcut.");
  }

  return repository.create(userId, year, term);
};

export const update = async (
  userId: string,
  semesterId: string,
  year: number,
  term: string
) => {
  const semester = await repository.findById(
    userId,
    semesterId
  );

  if (!semester) {
    throw AppError.notFound("Dönem bulunamadı.");
  }

  return repository.update(
    userId,
    semesterId,
    year,
    term
  );
};

export const remove = async (
  userId: string,
  semesterId: string
) => {
  const semester = await repository.findById(
    userId,
    semesterId
  );

  if (!semester) {
    throw AppError.notFound("Dönem bulunamadı.");
  }

  await repository.remove(userId, semesterId);

  return {
    success: true,
  };
};