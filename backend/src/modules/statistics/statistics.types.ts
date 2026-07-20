export interface SemesterGpa {
  semesterId: string;
  semester: string;
  gpa: number;
}

export interface StatisticsResponse {
  totalCourses: number;
  totalCredits: number;
  overallAverage: number;
  passedCourses: number;
  failedCourses: number;
  ongoingCourses: number;
  semesterGpa: SemesterGpa[];
}