export type TaskType = "assignment" | "project" | "presentation" | "other";
export type TaskPriority = "low" | "medium" | "high";
export type TaskStatus = "pending" | "completed";

export interface ChecklistItemDto {
  id: string;
  title: string;
  isDone: boolean;
  sortOrder: number;
}

export interface TaskDto {
  id: string;
  courseId: string | null;
  courseName: string | null;
  title: string;
  description: string | null;
  type: TaskType;
  dueAt: Date | null;
  priority: TaskPriority;
  status: TaskStatus;
  completedAt: Date | null;
  checklist: ChecklistItemDto[];
  checklistTotal: number;
  checklistDone: number;
  createdAt: Date;
  updatedAt: Date;
}

export interface CreateTaskDto {
  courseId?: string | null;
  title: string;
  description?: string | null;
  type?: TaskType;
  dueAt?: string | null;
  priority?: TaskPriority;
  checklist?: { title: string }[];
}

export interface UpdateTaskDto {
  courseId?: string | null;
  title?: string;
  description?: string | null;
  type?: TaskType;
  dueAt?: string | null;
  priority?: TaskPriority;
  status?: TaskStatus;
}

export interface TaskFilters {
  status?: TaskStatus;
  type?: TaskType;
  courseId?: string;
}

export interface CreateChecklistItemDto {
  title: string;
}

export interface UpdateChecklistItemDto {
  title?: string;
  isDone?: boolean;
  sortOrder?: number;
}
