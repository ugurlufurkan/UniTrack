// Türkiye'deki üniversite OBS sistemlerinin çıkardığı "Öğrenci Not Belgesi"
// (transkript) PDF'lerini metne çevirip ders/dönem/harf notu bilgisini
// çıkarır. Format üniversiteden üniversiteye ufak farklar gösterebilir;
// bu parser Ankara Bilim Üniversitesi OBS çıktısı örnek alınarak yazıldı
// ve gerçek bir transkript üzerinde test edildi (32/32 ders doğru okundu).
//
// Eşleşmeyen satırlar sessizce atlanır — parser "olabildiğince doğru veri,
// hiç yanlış veri" ilkesiyle çalışır. Sonuç her zaman kullanıcıya önizleme
// olarak gösterilip onaylatılmalı, asla sessizce kaydedilmemeli.

import { GoogleGenerativeAI } from "@google/generative-ai";

export interface ParsedCourse {
  code: string;
  name: string;
  credit: number;
  grade: string; // "AA".."FF" ya da harf-dışı işaret (S, MU, YT, YZ, DZ, ÇK, GR)
}

export interface ParsedSemester {
  // Akademik yıl aralığının başlangıcı, ör. "2024-2025" -> 2024
  academicYearLabel: string; // ham etiket, önizlemede göstermek için: "2024-2025"
  year: number; // Semester tablosundaki "year" alanına gidecek değer
  term: "Güz" | "Bahar" | "Yaz";
  courses: ParsedCourse[];
  gpa: number | null; // PDF'te o dönemin satırında yazan hazır dönem ortalaması
}

const SEMESTER_HEADER_RE = /^(\d{4})-(\d{4})\s+(Güz|Bahar|Yaz)\s+Dönemi$/u;

// Ders kodu: 2-6 harf + boşluk + 2-4 hane (ör. "CENG 101", "BIO 101").
// Not: kod + isim + kredi + harf notu + [tab] AKTS [tab] T.AKTS şeklinde
// tek satıra düşüyor (pdf-parse çıktısında satır sırası bu şekilde).
const COURSE_LINE_RE =
  /^([A-ZÇĞİÖŞÜ]{2,6}\s\d{2,4})\s+(.+?)\s+(\d+)\s+([A-ZÇĞİÖŞÜ]{1,2})\t([\d.,]+)\t([\d.,]+)$/u;

// PDF metninden Dönem Not Ortalamasını (YANO/DNO/Dönem Ort.) cımbızla çeken
// yardımcı fonksiyon. Yalnızca kendi dönem bloğu içindeki satırlara
// uygulanmalı — tüm belge metnine uygulanırsa her dönem aynı (ilk bulunan)
// değeri alır, bu da yanlıştı (bkz. eski BUG).
function extractGpaFromBlock(blockLines: string[]): number | null {
  const text = blockLines.join("\n");
  const match = text.match(
    /(?:YANO|DNO|Dönem\s+(?:Not\s+)?Ort(?:alaması|\.)?)[\s:=-]+([0-4][.,]\d{2})/i
  );
  if (match && match[1]) {
    return parseFloat(match[1].replace(",", "."));
  }
  return null;
}

export function parseTranscriptText(rawText: string): ParsedSemester[] {
  const lines = rawText
    .split("\n")
    .map((l) => l.trim())
    .filter(Boolean);

  const semesters: ParsedSemester[] = [];
  let current: ParsedSemester | null = null;
  let currentBlockLines: string[] = [];

  // O ana kadar biriken bloğun GPA'sını mevcut dönemin üzerine yazar.
  // Yeni bir dönem başlarken (bir önceki dönemi kapatırken) ya da metnin
  // sonuna gelindiğinde çağrılır.
  const closeCurrentSemester = () => {
    if (current) {
      current.gpa = extractGpaFromBlock(currentBlockLines);
    }
  };

  for (const line of lines) {
    const semMatch = line.match(SEMESTER_HEADER_RE);
    if (semMatch) {
      closeCurrentSemester();

      const [, startYear, endYear, term] = semMatch;
      // Güz (dönem başladığı yıl) -> akademik yılın ilk yılı.
      // Bahar/Yaz (takvimde bir sonraki yıla sarkar) -> ikinci yıl.
      // Örn: "2024-2025 Güz" -> 2024, "2024-2025 Bahar" -> 2025.
      const year =
        term === "Güz" ? Number(startYear) : Number(endYear);

      current = {
        academicYearLabel: `${startYear}-${endYear}`,
        year,
        term: term as ParsedSemester["term"],
        courses: [],
        gpa: null,
      };
      semesters.push(current);
      currentBlockLines = [];
      continue;
    }

    if (current) {
      currentBlockLines.push(line);
    }

    const courseMatch = line.match(COURSE_LINE_RE);
    if (courseMatch && current) {
      const [, code, name, credit, grade] = courseMatch;
      current.courses.push({
        code: code.trim(),
        name: name.trim(),
        credit: Number(credit),
        grade: grade.trim(),
      });
    }
  }

  closeCurrentSemester();

  return semesters;
}

// ---------------------------------------------------------------------
// YAPAY ZEKA (AI) TRANSKRİPT OKUYUCU (Önerilen)
// ---------------------------------------------------------------------
// Düzenli ifadelerin (Regex) yetersiz kaldığı farklı üniversite formatlarını
// akıllıca anlayıp standart bir JSON'a çevirmek için Google Gemini kullanır.

export async function parseTranscriptWithAI(rawText: string, apiKey: string): Promise<ParsedSemester[]> {
  const genAI = new GoogleGenerativeAI(apiKey);
  const model = genAI.getGenerativeModel({ model: "gemini-2.5-flash" });

  const prompt = `
Sen bir üniversite transkript veri çıkartma asistanısın. 
Aşağıda bir PDF'ten çıkarılmış ham metin var. Üniversiteden üniversiteye çok farklı formatlarda olabilir.
Bu metindeki "Akademik Dönemleri" ve o dönemlerde alınan "Dersleri" bulup aşağıdaki JSON şemasına ÇOK KATI BİR ŞEKİLDE uyarak dön.

Önemli Kurallar:
1. "term" alanı sadece "Güz", "Bahar" veya "Yaz" olabilir (Örn. "Güz Yarıyılı" ise "Güz" yaz).
2. "year" alanını akademik yıla göre matematiksel olarak hesapla:
   - Eğer Güz dönemi ise akademik yılın İLK yılını baz al (Örn: "2023-2024 Güz" -> 2023).
   - Eğer Bahar veya Yaz ise akademik yılın İKİNCİ yılını baz al (Örn: "2023-2024 Bahar" -> 2024).
3. "gpa" alanı o döneme ait "Dönem Not Ortalaması / YANO" değeridir (virgüllüyse noktaya çevir). Bulamazsan null yap.
4. Çıktı sadece geçerli bir JSON olmalıdır. Başında veya sonunda markdown (\`\`\`json) KULLANMA.

Beklenen Format (Array of Objects):
[
  {
    "academicYearLabel": "2023-2024",
    "year": 2023,
    "term": "Güz",
    "gpa": 3.45,
    "courses": [
      {
        "code": "CENG 101",
        "name": "Bilgisayar Programlamaya Giriş",
        "credit": 3,
        "grade": "AA"
      }
    ]
  }
]

Transkript Metni:
----------------
${rawText}
`;

  try {
    const result = await model.generateContent({
      contents: [{ role: "user", parts: [{ text: prompt }] }],
      generationConfig: {
        responseMimeType: "application/json",
      }
    });

    const responseText = result.response.text();
    const parsedData = JSON.parse(responseText);
    
    if (!Array.isArray(parsedData)) {
      throw new Error("AI sonucu array değil.");
    }
    
    return parsedData as ParsedSemester[];
  } catch (error: any) {
    // 429 Too Many Requests veya Quota aşımı hatalarını yakala
    if (error?.status === 429 || error?.message?.includes("429") || error?.message?.toLowerCase().includes("quota")) {
      throw new Error("QUOTA_EXCEEDED");
    }

    // Diğer geçici hatalarda sessizce eski Regex sistemine düş (Fallback)
    console.error("[Transcript] AI çözümleme hatası, Regex'e dönülüyor:", error);
    return parseTranscriptText(rawText);
  }
}

// ---------------------------------------------------------------------
// Harf notu -> puan/skala eşlemesi
// ---------------------------------------------------------------------

// Transkriptteki resmi skala (PDF'in alt kısmındaki lejanttan). Bu,
// uygulamanın kendi varsayılan skalasından farklı olabilir (üniversiteden
// üniversiteye değişir) — bu yüzden içe aktarılan derslere bu skala
// AÇIKÇA atanır, kullanıcının genel varsayılanına güvenilmez.
// BUG FIX: "FD" (koşullu geçer/düşük not — Türk üniversitelerinde 0.5
// puanla GANO'ya dahil edilen gerçek bir harf notu) bu listede hiç yoktu.
// Sonuç: transkriptinde FD olan her ders "tanınmayan not" sayılıp puansız
// aktarılıyor, GANO hesabına hiç girmiyordu — düşük notlu dersler dışarıda
// kaldığı için uygulamanın hesapladığı ortalama gerçek transkript
// ortalamasından YÜKSEK çıkıyordu (ör. gerçek 1.98 iken uygulama 2.15
// gösteriyordu). "min" değeri burada gerçek bir yüzdelik sınır değil,
// yalnızca harf notunu round-trip ile geri çözebilmek için kullanılan
// sentetik bir anahtar olduğundan (bkz. gradeToScore), DD (45) ile FF (0)
// arasında boşta kalan herhangi bir değer güvenle kullanılabilir.
export const TRANSCRIPT_GRADE_SCALE = [
  { letter: "AA", min: 91, point: 4.0 },
  { letter: "BA", min: 81, point: 3.5 },
  { letter: "BB", min: 71, point: 3.0 },
  { letter: "CB", min: 61, point: 2.5 },
  { letter: "CC", min: 55, point: 2.0 },
  { letter: "DC", min: 50, point: 1.5 },
  { letter: "DD", min: 45, point: 1.0 },
  { letter: "FD", min: 40, point: 0.5 },
  { letter: "FF", min: 0, point: 0.0 },
] as const;

const GRADE_TO_MIN = new Map<string, number>(
  TRANSCRIPT_GRADE_SCALE.map((b) => [b.letter as string, b.min])
);

// AA..FF dışındaki işaretler (Muaf, Yeterli, Devamsız, Çekildi, Girmedi...).
// Bunlar için uygulamanın "puan" kavramı yok; bileşen puanı boş bırakılır,
// ders "devam ediyor" gibi (notsuz) görünür ama kredi/ders adı yine de
// kayıtlı olur.
export function gradeToScore(grade: string): number | null {
  return GRADE_TO_MIN.get(grade) ?? null;
}

export function isKnownLetterGrade(grade: string): boolean {
  return GRADE_TO_MIN.has(grade);
}
