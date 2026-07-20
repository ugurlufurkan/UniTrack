import { Router } from "express";

import {
  addChecklistItem,
  create,
  findAll,
  getById,
  remove,
  removeChecklistItem,
  toggleStatus,
  update,
  updateChecklistItem,
} from "./tasks.controller";

import { authMiddleware } from "../../shared/middleware/auth.middleware";
import { validate } from "../../shared/middleware/validate.middleware";
import {
  createChecklistItemSchema,
  createTaskSchema,
  updateChecklistItemSchema,
  updateTaskSchema,
} from "./tasks.validator";

const router = Router();

router.use(authMiddleware);

router.get("/", findAll);
router.get("/:taskId", getById);
router.post("/", validate(createTaskSchema), create);
router.put("/:taskId", validate(updateTaskSchema), update);
router.delete("/:taskId", remove);
router.patch("/:taskId/toggle", toggleStatus);

router.post(
  "/:taskId/checklist",
  validate(createChecklistItemSchema),
  addChecklistItem
);
router.put(
  "/:taskId/checklist/:itemId",
  validate(updateChecklistItemSchema),
  updateChecklistItem
);
router.delete("/:taskId/checklist/:itemId", removeChecklistItem);

export default router;
