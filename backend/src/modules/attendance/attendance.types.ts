export interface UpsertAttendanceRecordDto {
  weekNumber: number;
  date: string;
  attendedHours: number;
  absentHours: number;
  excusedHours: number;
  note?: string | null;
}

export interface UpdateAttendanceRecordDto {
  date?: string;
  attendedHours?: number;
  absentHours?: number;
  excusedHours?: number;
  note?: string | null;
}

export interface UpdateAttendanceSettingsDto {
  totalWeeks?: number;
  weeklyHours?: number;
  attendanceLimitHours?: number;
}
