import * as repository from "./settings.repository";
import { SettingsUpdateInput } from "../../shared/validation/settings.schema";
import { AppError } from "../../shared/errors/AppError";

export const getSettings = (userId: string) => repository.getSettings(userId);

export const updateSettings = async (userId: string, data: SettingsUpdateInput) => {
  const current = await repository.getSettings(userId);
  const start = data.examPeriodStart === undefined ? current.examPeriodStart : data.examPeriodStart;
  const end = data.examPeriodEnd === undefined ? current.examPeriodEnd : data.examPeriodEnd;
  if (start && end && start > end) throw AppError.badRequest("Sınav dönemi başlangıcı bitişten sonra olamaz.");
  return repository.updateSettings(userId, data);
};
