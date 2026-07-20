// Görev yönetimi, ayrı bir tablo yerine mevcut `events` tablosunu
// (type: assignment | project | presentation) ve ona bağlı yeni
// `event_checklist_items` alt görev tablosunu kullanır — böylece Takvim ile
// Görevler ekranı aynı veriyi paylaşır ve birbirinden kopmaz.

export type TaskType = "assignment" | "project" | "presentation";

export const TASK_TYPES: TaskType[] = ["assignment", "project", "presentation"];

export interface CreateChecklistItemDto {
  title: string;
}

export interface UpdateChecklistItemDto {
  title?: string;
  isDone?: boolean;
  sortOrder?: number;
}

export interface ReorderChecklistDto {
  itemIds: string[];
}
