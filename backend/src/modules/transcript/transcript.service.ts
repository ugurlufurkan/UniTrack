import * as courseRepository from "../course/course.repository";

export const getTranscript = async (userId: string) => {
  const courses = await courseRepository.findAll(userId);

  const transcript = courses.map((course) => ({
    semesterId: course.semesterId,
    course: course.name,
    credit: course.credit,
    average: course.average,
    // Notu henuz girilmemis dersler transkriptte "Devam Ediyor" olarak gorunur.
    letter: course.letterGrade ?? "Devam Ediyor",
    point: course.gradePoint,
  }));

  return transcript;
};
