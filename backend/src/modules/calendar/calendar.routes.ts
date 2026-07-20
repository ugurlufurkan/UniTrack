import { Router } from "express";

import {
  getAllEvents,
  getEventById,
  getUpcomingEvents,
  createEvent,
  updateEvent,
  deleteEvent,
  getAllSchedule,
  createSchedule,
  updateSchedule,
  deleteSchedule,
  getCalendarSummary,
} from "./calendar.controller";

import { authMiddleware } from "../../shared/middleware/auth.middleware";
import { validate } from "../../shared/middleware/validate.middleware";
import {
  createEventSchema,
  updateEventSchema,
  createScheduleSchema,
  updateScheduleSchema,
} from "./calendar.validator";

const router = Router();

router.use(authMiddleware);

router.get("/summary", getCalendarSummary);
router.get("/events/upcoming", getUpcomingEvents);
router.get("/events", getAllEvents);
router.get("/events/:id", getEventById);
router.post("/events", validate(createEventSchema), createEvent);
router.put("/events/:id", validate(updateEventSchema), updateEvent);
router.delete("/events/:id", deleteEvent);

router.get("/schedule", getAllSchedule);
router.post("/schedule", validate(createScheduleSchema), createSchedule);
router.put("/schedule/:id", validate(updateScheduleSchema), updateSchedule);
router.delete("/schedule/:id", deleteSchedule);

export default router;
