import { Router } from "express";

import {
  getAllTasks,
  getTaskById,
  createChecklistItem,
  updateChecklistItem,
  deleteChecklistItem,
  reorderChecklist,
} from "./task.controller";

import { authMiddleware } from "../../shared/middleware/auth.middleware";
import { validate } from "../../shared/middleware/validate.middleware";
import {
  createChecklistItemSchema,
  updateChecklistItemSchema,
  reorderChecklistSchema,
} from "./task.validator";

const router = Router();

router.use(authMiddleware);

router.get("/", getAllTasks);
router.get("/:id", getTaskById);

router.post(
  "/:id/checklist",
  validate(createChecklistItemSchema),
  createChecklistItem
);
router.put(
  "/:id/checklist/reorder",
  validate(reorderChecklistSchema),
  reorderChecklist
);
router.put(
  "/checklist/:itemId",
  validate(updateChecklistItemSchema),
  updateChecklistItem
);
router.delete("/checklist/:itemId", deleteChecklistItem);

export default router;
