import { Request, Response } from "express";

import * as calendarService from "./calendar.service";

export const getAllEvents = async (req: Request, res: Response) => {
  const userId = req.user!.userId;
  const { startDate, endDate, type } = req.query;

  const events = await calendarService.getAllEvents(userId, {
    startDate: startDate as string | undefined,
    endDate: endDate as string | undefined,
    type: type as string | undefined,
  });

  res.json(events);
};

export const getEventById = async (req: Request, res: Response) => {
  const userId = req.user!.userId;
  const eventId = req.params.id as string;
  const event = await calendarService.getEventById(userId, eventId);
  res.json(event);
};

export const getUpcomingEvents = async (req: Request, res: Response) => {
  const userId = req.user!.userId;
  const limit = req.query.limit ? Number(req.query.limit) : 20;
  const events = await calendarService.getUpcomingEvents(userId, limit);
  res.json(events);
};

export const createEvent = async (req: Request, res: Response) => {
  const userId = req.user!.userId;
  const event = await calendarService.createEvent(userId, req.body);
  res.status(201).json(event);
};

export const updateEvent = async (req: Request, res: Response) => {
  const userId = req.user!.userId;
  const eventId = req.params.id as string;
  const event = await calendarService.updateEvent(userId, eventId, req.body);
  res.json(event);
};

export const deleteEvent = async (req: Request, res: Response) => {
  const userId = req.user!.userId;
  const eventId = req.params.id as string;
  await calendarService.removeEvent(userId, eventId);
  res.status(200).json({ success: true });
};

export const getAllSchedule = async (req: Request, res: Response) => {
  const userId = req.user!.userId;
  const schedule = await calendarService.getAllSchedule(userId);
  res.json(schedule);
};

export const createSchedule = async (req: Request, res: Response) => {
  const userId = req.user!.userId;
  const entry = await calendarService.createSchedule(userId, req.body);
  res.status(201).json(entry);
};

export const updateSchedule = async (req: Request, res: Response) => {
  const userId = req.user!.userId;
  const scheduleId = req.params.id as string;
  const entry = await calendarService.updateSchedule(
    userId,
    scheduleId,
    req.body
  );
  res.json(entry);
};

export const deleteSchedule = async (req: Request, res: Response) => {
  const userId = req.user!.userId;
  const scheduleId = req.params.id as string;
  await calendarService.removeSchedule(userId, scheduleId);
  res.status(200).json({ success: true });
};

export const getCalendarSummary = async (req: Request, res: Response) => {
  const userId = req.user!.userId;
  const summary = await calendarService.getCalendarSummary(userId);
  res.json(summary);
};
