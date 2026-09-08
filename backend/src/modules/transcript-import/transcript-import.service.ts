import { PDFParse } from "pdf-parse";

import {
  parseTranscriptText,
  parseTranscriptWithAI,
  gradeToScore,
  isKnownLetterGrade,
  TRANSCRIPT_GRADE_SCALE,
  ParsedSemester,
} from "./transcript-import.parser";

import * as semesterRepository from "../semester/semester.repository";
import * as courseRepository from "../course/course.repository";
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
  const createdCourses: { semesterYear: number; semesterTerm: string; courseName: string }[] = [];

  for (const sem of semesters) {
    let semesterId: string;

    const existing = await semesterRepository.exists(userId, sem.year, sem.term);

    if (existing) {
      const all = await semesterRepository.findAll(userId);
      const match = all.find((s) => s.year === sem.year && s.term === sem.term);
      semesterId = match!.id;
    } else {
      // Repository fonksiyonunu bozmadan orijinal haliyle (sadece 3 parametre ile) çağırıyoruz:
      const created = await semesterRepository.create(userId, sem.year, sem.term);
      semesterId = created.id;
    }

    for (const course of sem.courses) {
      const score = gradeToScore(course.grade);

      await courseRepository.create(userId, {
        semesterId,
        name: course.name,
        credit: course.credit,
        gradeScale: [...TRANSCRIPT_GRADE_SCALE],
        components: [
          {
            name: "Dönem Sonu Notu",
            weight: 100,
            score,
          },
        ],
      });

      createdCourses.push({
        semesterYear: sem.year,
        semesterTerm: sem.term,
        courseName: course.name,
      });
    }
  }

  return {
    importedSemesters: semesters.length,
    importedCourses: createdCourses.length,
  };
};