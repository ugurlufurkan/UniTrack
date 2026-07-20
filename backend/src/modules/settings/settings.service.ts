import * as repository from "./settings.repository";
import { SettingsUpdateInput } from "../../shared/validation/settings.schema";

export const getSettings = (userId: string) => repository.getSettings(userId);

export const updateSettings = (userId: string, data: SettingsUpdateInput) =>
  repository.updateSettings(userId, data);
