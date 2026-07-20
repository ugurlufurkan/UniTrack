import * as calendarRepository from "./calendar.repository";
import {
  CreateEventDto,
  CreateScheduleDto,
  UpdateEventDto,
  UpdateScheduleDto,
} from "./calendar.types";

export const getAllEvents = async (
  userId: string,
  filters?: { startDate?: string; endDate?: string; type?: string }
) => calendarRepository.findAllEvents(userId, filters);

export const getEventById = async (userId: string, eventId: string) =>
  calendarRepository.findEventById(userId, eventId);

export const getUpcomingEvents = async (userId: string, limit?: number) =>
  calendarRepository.findUpcomingEvents(userId, limit);

export const createEvent = async (userId: string, data: CreateEventDto) =>
  calendarRepository.createEvent(userId, data);

export const updateEvent = async (
  userId: string,
  eventId: string,
  data: UpdateEventDto
) => calendarRepository.updateEvent(userId, eventId, data);

export const removeEvent = async (userId: string, eventId: string) =>
  calendarRepository.removeEvent(userId, eventId);

export const getAllSchedule = async (userId: string) =>
  calendarRepository.findAllSchedule(userId);

export const createSchedule = async (userId: string, data: CreateScheduleDto) =>
  calendarRepository.createSchedule(userId, data);

export const updateSchedule = async (
  userId: string,
  scheduleId: string,
  data: UpdateScheduleDto
) => calendarRepository.updateSchedule(userId, scheduleId, data);

export const removeSchedule = async (userId: string, scheduleId: string) =>
  calendarRepository.removeSchedule(userId, scheduleId);

export const getCalendarSummary = async (userId: string) =>
  calendarRepository.getCalendarSummary(userId);
