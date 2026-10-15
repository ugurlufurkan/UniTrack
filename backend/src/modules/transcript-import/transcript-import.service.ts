import { PDFParse } from "pdf-parse";

import {
  parseTranscriptText,
  parseTranscriptWithAI,
  gradeToScore,
  isKnownLetterGrade,
  TRANSCRIPT_GRADE_SCALE,
  ParsedSemester,
} from "./transcript-import.parser";

import { and, eq } from "drizzle-orm";
import { db } from "../../db";
import { users, semesters as semesterTable, courses, courseComponents } from "../../db/schema";
import { calculateGrade } from "../../shared/utils/grade.util";
import { AppError } from "../../shared/errors/AppError";
import { env } from "../../config/env";

export interface PreviewCourse {
  name: string;
  credit: number;
  grade: string;
  recognized: boolean; // false ise: harf notu skalada yok (S, MU, DZ vb.), puansız aktarılacak
}

export interface PreviewSemester {
  year: number;
  term: "Güz" | "Bahar" | "Yaz";
  academicYearLabel: string;
  gpa: number | null; // <-- PDF'ten direkt çekilen gerçek dönem ortalaması
  courses: PreviewCourse[];
}

// PDF dosyasını okuyup önizleme (kaydetmeden) döner.
export const parsePdf = async (fileBuffer: Buffer): Promise<PreviewSemester[]> => {
  let text: string;

  try {
    const parser = new PDFParse({ data: fileBuffer });
    const result = await parser.getText();
    text = result.text;
  } catch {
    throw AppError.badRequest(
      "PDF dosyası okunamadı. Dosyanın bozuk olmadığından emin ol."
    );
  }

  let semesters: ParsedSemester[] = [];

  if (env.GEMINI_API_KEY) {
    try {
      // 1. Yol: Yapay Zeka ile akıllı çıkarım
      semesters = await parseTranscriptWithAI(text, env.GEMINI_API_KEY);
    } catch (error: any) {
      if (error.message === "QUOTA_EXCEEDED") {
        throw AppError.tooManyRequests("Sistemin yapay zeka (PDF okuma) günlük limiti dolmuştur. Lütfen yarın tekrar deneyiniz.");
      }
      // AI'dan başka bir hata gelirse eski sisteme düşsün (Fallback)
      semesters = parseTranscriptText(text);
    }
  } else {
    // 2. Yol: Klasik Regex kuralları (Fallback)
    semesters = parseTranscriptText(text);
  }

  if (!semesters.length) {
    throw AppError.badRequest(
      "PDF içinde tanınabilir bir transkript tablosu bulunamadı veya çözümlenemedi."
    );
  }

  return semesters.map((sem) => ({
    year: sem.year,
    term: sem.term,
    academicYearLabel: sem.academicYearLabel,
    gpa: sem.gpa,
    courses: sem.courses.map((c) => ({
      name: c.name,
      credit: c.credit,
      grade: c.grade,
      recognized: isKnownLetterGrade(c.grade),
    })),
  }));
};

export interface ImportCourseInput {
  name: string;
  credit: number;
  grade: string;
}

export interface ImportSemesterInput {
  year: number;
  term: "Güz" | "Bahar" | "Yaz";
  gpa?: number | null;
  courses: ImportCourseInput[];
}

// Kullanıcının onayladığı (gerekirse düzenlediği) veriyi kaydeder.
// Aynı yıl+dönem zaten varsa yeniden oluşturmaz, mevcut olana ders ekler.
export const importSemesters = async (
  userId: string,
  semesters: ImportSemesterInput[]
) => {
  return db.transaction(async (tx) => {
    // Serialize imports for this account across every API instance. A failed
    // course rolls the entire import back, including newly created semesters.
    const [owner] = await tx.select({ id: users.id }).from(users)
      .where(eq(users.id, userId)).for("update");
    if (!owner) throw AppError.unauthorized("Öğrenci hesabı bulunamadı.");
    let importedSemesters = 0;
    let importedCourses = 0;
    let skippedCourses = 0;
    const normalize = (name: string) => name.normalize("NFKC").trim().replace(/\s+/g, " ").toLocaleLowerCase("tr-TR");

    for (const sem of semesters) {
      let [semester] = await tx.select().from(semesterTable).where(and(
        eq(semesterTable.userId, userId), eq(semesterTable.year, sem.year), eq(semesterTable.term, sem.term),
      )).limit(1);
      if (!semester) {
        [semester] = await tx.insert(semesterTable).values({ userId, year: sem.year, term: sem.term }).returning();
        importedSemesters++;
      }
      const existing = await tx.select({ name: courses.name }).from(courses).where(eq(courses.semesterId, semester.id));
      const names = new Set(existing.map(course => normalize(course.name)));
      for (const course of sem.courses) {
        const key = normalize(course.name);
        // Preserve existing/manual grades rather than silently overwriting them.
        if (names.has(key)) { skippedCourses++; continue; }
        const score = gradeToScore(course.grade);
        const grade = calculateGrade([{ weight: 100, score }], [...TRANSCRIPT_GRADE_SCALE]);
        const [created] = await tx.insert(courses).values({
          semesterId: semester.id, name: course.name.trim(), credit: course.credit,
          gradeScale: [...TRANSCRIPT_GRADE_SCALE], ...grade,
        }).returning({ id: courses.id });
        await tx.insert(courseComponents).values({ courseId: created.id, name: "Dönem Sonu Notu", weight: 100, score, sortOrder: 0 });
        names.add(key);
        importedCourses++;
      }
    }
    return { importedSemesters, importedCourses, skippedCourses };
  });
};
