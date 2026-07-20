import * as attendanceRepository from "./attendance.repository";
import {
  UpdateAttendanceRecordDto,
  UpdateAttendanceSettingsDto,
  UpsertAttendanceRecordDto,
} from "./attendance.types";

export const getAllCoursesSummary = async (userId: string) =>
  attendanceRepository.findAllCoursesWithSummary(userId);

export const getCourseAttendance = async (userId: string, courseId: string) =>
  attendanceRepository.findCourseAttendance(userId, courseId);

export const getOverview = async (userId: string) =>
  attendanceRepository.getOverview(userId);

export const upsertRecord = async (
  userId: string,
  courseId: string,
  data: UpsertAttendanceRecordDto
) => attendanceRepository.upsertRecord(userId, courseId, data);

export const updateRecord = async (
  userId: string,
  recordId: string,
  data: UpdateAttendanceRecordDto
) => attendanceRepository.updateRecord(userId, recordId, data);

export const removeRecord = async (userId: string, recordId: string) =>
  attendanceRepository.removeRecord(userId, recordId);

export const updateCourseSettings = async (
  userId: string,
  courseId: string,
  data: UpdateAttendanceSettingsDto
) => attendanceRepository.updateCourseSettings(userId, courseId, data);
