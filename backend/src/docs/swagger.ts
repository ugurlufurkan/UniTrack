const swaggerDocument = {
  openapi: "3.0.0",

  info: {
    title: "UniTrack API",
    version: "3.0.0",
    description:
      "Üniversite GANO/GPA takip uygulaması için REST API. " +
      "Tüm endpointler (aksi belirtilmedikçe) `/api/v1` altında sunulur; " +
      "geriye dönük uyumluluk için aynı endpointler `/api` altında da " +
      "erişilebilir durumdadır (bkz. `Deprecation` response header'ı).",
  },

  servers: [
    { url: "http://localhost:5000/api/v1", description: "Local (versioned)" },
    { url: "https://api.unitrack.app/api/v1", description: "Production" },
  ],

  tags: [
    { name: "Auth", description: "Kimlik doğrulama, oturum ve hesap yönetimi" },
    { name: "Semester", description: "Dönem (yıl + dönem) yönetimi" },
    { name: "Course", description: "Ders, bileşen ve harf notu yönetimi" },
    { name: "GradeScale", description: "Harf notu aralığı (skala) yönetimi" },
    { name: "GPA", description: "GANO hesaplama" },
    { name: "Statistics", description: "Genel istatistikler" },
    { name: "Dashboard", description: "Ana sayfa özet verisi" },
    { name: "Transcript", description: "Transkript görünümü" },
    { name: "System", description: "Altyapı: health check vb." },
  ],

  components: {
    securitySchemes: {
      bearerAuth: {
        type: "http",
        scheme: "bearer",
        bearerFormat: "JWT",
        description: "`Authorization: Bearer <accessToken>` — access token 15 dakika geçerlidir.",
      },
    },

    schemas: {
      Error: {
        type: "object",
        properties: {
          success: { type: "boolean", example: false },
          message: { type: "string", example: "Geçersiz email veya şifre." },
        },
      },

      ValidationError: {
        type: "object",
        properties: {
          success: { type: "boolean", example: false },
          message: { type: "string", example: "Validation Error" },
          errors: {
            type: "array",
            items: {
              type: "object",
              properties: {
                code: { type: "string", example: "too_small" },
                path: { type: "array", items: { type: "string" }, example: ["credit"] },
                message: { type: "string", example: "Number must be greater than or equal to 1" },
              },
            },
          },
        },
      },

      User: {
        type: "object",
        properties: {
          id: { type: "string", format: "uuid" },
          email: { type: "string", format: "email" },
          name: { type: "string", example: "Furkan Yılmaz" },
          picture: { type: "string", nullable: true, format: "uri" },
          isEmailVerified: { type: "boolean" },
          defaultGradeScale: {
            oneOf: [
              { type: "array", items: { $ref: "#/components/schemas/GradeBand" } },
              { type: "null" },
            ],
          },
          createdAt: { type: "string", format: "date-time" },
          updatedAt: { type: "string", format: "date-time" },
        },
      },

      GradeBand: {
        type: "object",
        required: ["letter", "min", "point"],
        properties: {
          letter: { type: "string", example: "BA" },
          min: { type: "number", minimum: 0, maximum: 100, example: 80 },
          point: { type: "number", minimum: 0, maximum: 4.5, example: 3.5 },
        },
      },

      GradeScale: {
        type: "array",
        minItems: 2,
        maxItems: 20,
        items: { $ref: "#/components/schemas/GradeBand" },
        description:
          "En az 2 aralık, aralıklardan biri min=0 olmalı, aynı min değerine sahip iki aralık olamaz.",
      },

      CourseComponent: {
        type: "object",
        required: ["name", "weight"],
        properties: {
          id: { type: "string", format: "uuid", description: "Güncellemede mevcut bileşeni eşlemek için." },
          name: { type: "string", example: "Vize" },
          weight: { type: "number", minimum: 0, maximum: 100, example: 40 },
          score: { type: "number", minimum: 0, maximum: 100, nullable: true, example: 78 },
        },
      },

      Course: {
        type: "object",
        properties: {
          id: { type: "string", format: "uuid" },
          semesterId: { type: "string", format: "uuid" },
          name: { type: "string", example: "Data Structures" },
          credit: { type: "integer", example: 5 },
          gradeScale: {
            oneOf: [
              { type: "array", items: { $ref: "#/components/schemas/GradeBand" } },
              { type: "null" },
            ],
          },
          average: { type: "number", nullable: true, example: 82.5 },
          letterGrade: { type: "string", nullable: true, example: "BA" },
          gradePoint: { type: "number", nullable: true, example: 3.5 },
          passed: { type: "boolean" },
          createdAt: { type: "string", format: "date-time" },
        },
      },

      Semester: {
        type: "object",
        properties: {
          id: { type: "string", format: "uuid" },
          userId: { type: "string", format: "uuid" },
          year: { type: "integer", example: 2026 },
          term: { type: "string", enum: ["Güz", "Bahar", "Yaz"], example: "Güz" },
          createdAt: { type: "string", format: "date-time" },
        },
      },

      Device: {
        type: "object",
        properties: {
          id: { type: "string", format: "uuid" },
          deviceName: { type: "string", example: "iPhone" },
          deviceType: { type: "string", enum: ["mobile", "tablet", "desktop", "unknown"] },
          userAgent: { type: "string" },
          ipAddress: { type: "string", example: "203.0.113.42" },
          lastUsedAt: { type: "string", format: "date-time" },
          createdAt: { type: "string", format: "date-time" },
        },
      },

      AuthTokens: {
        type: "object",
        properties: {
          success: { type: "boolean", example: true },
          message: { type: "string", example: "Giriş başarılı." },
          user: { $ref: "#/components/schemas/User" },
          accessToken: { type: "string", description: "15 dakika geçerli JWT." },
          refreshToken: { type: "string", description: "30 gün geçerli, tek kullanımlık (rotation) JWT." },
        },
      },

      GpaResult: {
        type: "object",
        properties: {
          gpa: { type: "number", example: 3.12 },
          courses: {
            type: "array",
            items: {
              type: "object",
              properties: {
                name: { type: "string" },
                credit: { type: "integer" },
                average: { type: "number", nullable: true },
                letter: { type: "string", nullable: true },
                point: { type: "number", nullable: true },
                completed: { type: "boolean" },
              },
            },
          },
        },
      },

      StatisticsResult: {
        type: "object",
        properties: {
          totalCourses: { type: "integer", example: 32 },
          totalCredits: { type: "integer", example: 148 },
          overallAverage: { type: "number", example: 78.4 },
          passedCourses: { type: "integer", example: 28 },
          failedCourses: { type: "integer", example: 2 },
          ongoingCourses: { type: "integer", example: 2 },
        },
      },

      DashboardResult: {
        type: "object",
        properties: {
          totalSemesters: { type: "integer", example: 6 },
          totalCourses: { type: "integer", example: 32 },
          totalCredits: { type: "integer", example: 148 },
          passedCourses: { type: "integer", example: 28 },
          failedCourses: { type: "integer", example: 2 },
          ongoingCourses: { type: "integer", example: 2 },
          gpa: { type: "number", example: 3.12 },
        },
      },

      TranscriptEntry: {
        type: "object",
        properties: {
          semesterId: { type: "string", format: "uuid" },
          course: { type: "string", example: "Data Structures" },
          credit: { type: "integer", example: 5 },
          average: { type: "number", nullable: true, example: 82.5 },
          letter: { type: "string", example: "BA" },
          point: { type: "number", nullable: true, example: 3.5 },
        },
      },
    },

    responses: {
      BadRequest: {
        description: "Geçersiz istek (iş kuralı ihlali, ör. \"Bu email zaten kayıtlı.\").",
        content: { "application/json": { schema: { $ref: "#/components/schemas/Error" } } },
      },
      ValidationFailed: {
        description: "Body şema doğrulamasından geçemedi (Zod).",
        content: { "application/json": { schema: { $ref: "#/components/schemas/ValidationError" } } },
      },
      Unauthorized: {
        description: "Access token yok/geçersiz/süresi dolmuş, ya da kimlik bilgileri hatalı.",
        content: { "application/json": { schema: { $ref: "#/components/schemas/Error" } } },
      },
      NotFound: {
        description: "Kayıt bulunamadı (veya başka bir kullanıcıya ait).",
        content: { "application/json": { schema: { $ref: "#/components/schemas/Error" } } },
      },
      Conflict: {
        description: "Kayıt zaten mevcut / çakışma.",
        content: { "application/json": { schema: { $ref: "#/components/schemas/Error" } } },
      },
      TooManyRequests: {
        description: "Rate limit aşıldı.",
        content: {
          "application/json": {
            schema: {
              type: "object",
              properties: {
                success: { type: "boolean", example: false },
                message: { type: "string", example: "Too many authentication attempts, please try again later." },
              },
            },
          },
        },
      },
      InternalError: {
        description: "Beklenmeyen sunucu hatası.",
        content: { "application/json": { schema: { $ref: "#/components/schemas/Error" } } },
      },
    },
  },

  security: [{ bearerAuth: [] }],

  paths: {
    "/health": {
      get: {
        tags: ["System"],
        summary: "API sağlık kontrolü",
        description: "DB bağlantısını da kontrol eder. Versiyonsuzdur, monitoring için `/api/health`'ten sabit erişilir.",
        security: [],
        responses: {
          200: {
            description: "Servis ve DB sağlıklı.",
            content: {
              "application/json": {
                schema: {
                  type: "object",
                  properties: {
                    success: { type: "boolean", example: true },
                    message: { type: "string", example: "UniTrack API is healthy 🚀" },
                    timestamp: { type: "string", format: "date-time" },
                    environment: { type: "string", example: "production" },
                    version: { type: "string", example: "3.0.0" },
                    database: { type: "string", example: "connected" },
                  },
                },
              },
            },
          },
          503: {
            description: "DB bağlantısı yok / servis sağlıksız.",
            content: {
              "application/json": {
                schema: {
                  type: "object",
                  properties: {
                    success: { type: "boolean", example: false },
                    message: { type: "string", example: "Service unhealthy" },
                    database: { type: "string", example: "disconnected" },
                  },
                },
              },
            },
          },
        },
      },
    },

    "/auth/google": {
      post: {
        tags: ["Auth"],
        summary: "Google ile giriş / kayıt",
        description: "Google ID token'ı doğrular; kullanıcı yoksa otomatik oluşturur. Rate limit: 5 istek / 15 dk.",
        security: [],
        requestBody: {
          required: true,
          content: {
            "application/json": {
              schema: {
                type: "object",
                required: ["idToken"],
                properties: { idToken: { type: "string", description: "Google Sign-In'den alınan ID token." } },
              },
            },
          },
        },
        responses: {
          200: {
            description: "Giriş başarılı.",
            content: { "application/json": { schema: { $ref: "#/components/schemas/AuthTokens" } } },
          },
          400: { $ref: "#/components/responses/BadRequest" },
          401: { description: "Google token doğrulanamadı.", content: { "application/json": { schema: { $ref: "#/components/schemas/Error" } } } },
          429: { $ref: "#/components/responses/TooManyRequests" },
        },
      },
    },

    "/auth/login": {
      post: {
        tags: ["Auth"],
        summary: "Email + şifre ile giriş",
        description: "Rate limit: 5 istek / 15 dk. Başarısız denemeler audit log'a `LOGIN_FAILED` olarak düşer.",
        security: [],
        requestBody: {
          required: true,
          content: {
            "application/json": {
              schema: {
                type: "object",
                required: ["email", "password"],
                properties: {
                  email: { type: "string", format: "email" },
                  password: { type: "string", format: "password" },
                },
              },
            },
          },
        },
        responses: {
          200: {
            description: "Giriş başarılı.",
            content: { "application/json": { schema: { $ref: "#/components/schemas/AuthTokens" } } },
          },
          400: { $ref: "#/components/responses/BadRequest" },
          401: { description: "Email veya şifre hatalı.", content: { "application/json": { schema: { $ref: "#/components/schemas/Error" } } } },
          429: { $ref: "#/components/responses/TooManyRequests" },
        },
      },
    },

    "/auth/register": {
      post: {
        tags: ["Auth"],
        summary: "Email + şifre ile kayıt",
        description:
          "Rate limit: 5 istek / 15 dk. Şifre en az 8 karakter olmalı ve şu 5 kriterden en az 4'ünü " +
          "sağlamalı: küçük harf, büyük harf, rakam, özel karakter, 12+ karakter uzunluk.",
        security: [],
        requestBody: {
          required: true,
          content: {
            "application/json": {
              schema: {
                type: "object",
                required: ["email", "password", "name"],
                properties: {
                  email: { type: "string", format: "email" },
                  password: { type: "string", format: "password", example: "Sifrem123!" },
                  name: { type: "string", example: "Furkan Yılmaz" },
                },
              },
            },
          },
        },
        responses: {
          200: {
            description: "Kayıt başarılı, otomatik giriş yapılır.",
            content: { "application/json": { schema: { $ref: "#/components/schemas/AuthTokens" } } },
          },
          400: {
            description: "Eksik alan, geçersiz email formatı, zayıf şifre veya email zaten kayıtlı.",
            content: { "application/json": { schema: { $ref: "#/components/schemas/Error" } } },
          },
          429: { $ref: "#/components/responses/TooManyRequests" },
        },
      },
    },

    "/auth/refresh": {
      post: {
        tags: ["Auth"],
        summary: "Access token yenile (refresh token rotation)",
        description:
          "Refresh token tek kullanımlıktır: her çağrıda eskisi iptal edilip yenisi verilir. " +
          "Zaten iptal edilmiş (daha önce kullanılmış) bir token tekrar gönderilirse, bu çalıntı token " +
          "kullanımı olarak yorumlanır ve kullanıcının TÜM oturumları sonlandırılır.",
        security: [],
        requestBody: {
          required: true,
          content: {
            "application/json": {
              schema: {
                type: "object",
                required: ["refreshToken"],
                properties: { refreshToken: { type: "string" } },
              },
            },
          },
        },
        responses: {
          200: {
            description: "Yeni access + refresh token çifti.",
            content: {
              "application/json": {
                schema: {
                  type: "object",
                  properties: {
                    success: { type: "boolean", example: true },
                    accessToken: { type: "string" },
                    refreshToken: { type: "string" },
                  },
                },
              },
            },
          },
          401: {
            description:
              "Token geçersiz/süresi dolmuş, ya da yeniden kullanım (reuse) tespit edildi — bu durumda " +
              "mesaj \"Güvenlik nedeniyle tüm oturumlar sonlandırıldı.\" olur ve istemci tekrar login olmalıdır.",
            content: { "application/json": { schema: { $ref: "#/components/schemas/Error" } } },
          },
        },
      },
    },

    "/auth/logout": {
      post: {
        tags: ["Auth"],
        summary: "Mevcut cihazdan çıkış yap",
        description: "Sadece gönderilen refresh token'ı iptal eder, diğer cihazları etkilemez.",
        security: [],
        requestBody: {
          required: true,
          content: {
            "application/json": {
              schema: {
                type: "object",
                required: ["refreshToken"],
                properties: { refreshToken: { type: "string" } },
              },
            },
          },
        },
        responses: {
          200: {
            description: "Çıkış yapıldı (token zaten geçersizse de idempotent olarak başarı döner).",
            content: {
              "application/json": {
                schema: { type: "object", properties: { success: { type: "boolean", example: true } } },
              },
            },
          },
        },
      },
    },

    "/auth/logout-all": {
      post: {
        tags: ["Auth"],
        summary: "Tüm cihazlardan çıkış yap",
        description: "Kullanıcının sahip olduğu tüm refresh token'ları iptal eder.",
        responses: {
          200: {
            description: "Tüm oturumlar sonlandırıldı.",
            content: {
              "application/json": {
                schema: { type: "object", properties: { success: { type: "boolean", example: true } } },
              },
            },
          },
          401: { $ref: "#/components/responses/Unauthorized" },
        },
      },
    },

    "/auth/devices": {
      get: {
        tags: ["Auth"],
        summary: "Aktif oturum açık cihazları listele",
        responses: {
          200: {
            description: "Aktif cihaz listesi.",
            content: {
              "application/json": {
                schema: { type: "array", items: { $ref: "#/components/schemas/Device" } },
              },
            },
          },
          401: { $ref: "#/components/responses/Unauthorized" },
        },
      },
    },

    "/auth/devices/{tokenId}": {
      delete: {
        tags: ["Auth"],
        summary: "Belirli bir cihazdan çıkış yap",
        parameters: [
          { name: "tokenId", in: "path", required: true, schema: { type: "string", format: "uuid" }, description: "Device/refresh-token kaydının id'si (bkz. GET /auth/devices)." },
        ],
        responses: {
          200: {
            description: "Cihaz oturumu sonlandırıldı.",
            content: {
              "application/json": {
                schema: { type: "object", properties: { success: { type: "boolean", example: true } } },
              },
            },
          },
          401: { $ref: "#/components/responses/Unauthorized" },
          404: { $ref: "#/components/responses/NotFound" },
        },
      },
    },

    "/auth/verify-email/request": {
      post: {
        tags: ["Auth"],
        summary: "Email doğrulama linki gönder",
        responses: {
          200: {
            description: "Doğrulama emaili gönderildi.",
            content: {
              "application/json": {
                schema: { type: "object", properties: { success: { type: "boolean", example: true }, message: { type: "string" } } },
              },
            },
          },
          401: { $ref: "#/components/responses/Unauthorized" },
        },
      },
    },

    "/auth/verify-email": {
      post: {
        tags: ["Auth"],
        summary: "Email doğrulama token'ını kullan",
        security: [],
        requestBody: {
          required: true,
          content: {
            "application/json": {
              schema: { type: "object", required: ["token"], properties: { token: { type: "string" } } },
            },
          },
        },
        responses: {
          200: {
            description: "Email doğrulandı.",
            content: {
              "application/json": {
                schema: { type: "object", properties: { success: { type: "boolean", example: true }, message: { type: "string" } } },
              },
            },
          },
          400: { description: "Token geçersiz veya süresi dolmuş.", content: { "application/json": { schema: { $ref: "#/components/schemas/Error" } } } },
        },
      },
    },

    "/auth/forgot-password": {
      post: {
        tags: ["Auth"],
        summary: "Şifre sıfırlama emaili iste",
        description: "Rate limit: 3 istek / saat. Kayıtlı olmayan email için de aynı genel mesaj döner (email enumeration'ı önlemek için).",
        security: [],
        requestBody: {
          required: true,
          content: {
            "application/json": {
              schema: { type: "object", required: ["email"], properties: { email: { type: "string", format: "email" } } },
            },
          },
        },
        responses: {
          200: {
            description: "İstek alındı (kullanıcı var/yok bilgisi sızdırılmaz).",
            content: {
              "application/json": {
                schema: { type: "object", properties: { success: { type: "boolean", example: true }, message: { type: "string" } } },
              },
            },
          },
          429: { $ref: "#/components/responses/TooManyRequests" },
        },
      },
    },

    "/auth/reset-password": {
      post: {
        tags: ["Auth"],
        summary: "Şifreyi sıfırlama token'ıyla değiştir",
        description: "Rate limit: 3 istek / saat.",
        security: [],
        requestBody: {
          required: true,
          content: {
            "application/json": {
              schema: {
                type: "object",
                required: ["token", "newPassword"],
                properties: { token: { type: "string" }, newPassword: { type: "string", format: "password" } },
              },
            },
          },
        },
        responses: {
          200: {
            description: "Şifre değiştirildi.",
            content: {
              "application/json": {
                schema: { type: "object", properties: { success: { type: "boolean", example: true }, message: { type: "string" } } },
              },
            },
          },
          400: { description: "Token geçersiz/süresi dolmuş veya yeni şifre yetersiz.", content: { "application/json": { schema: { $ref: "#/components/schemas/Error" } } } },
          429: { $ref: "#/components/responses/TooManyRequests" },
        },
      },
    },

    "/auth/change-password": {
      post: {
        tags: ["Auth"],
        summary: "Oturum açıkken şifre değiştir",
        requestBody: {
          required: true,
          content: {
            "application/json": {
              schema: {
                type: "object",
                required: ["currentPassword", "newPassword"],
                properties: { currentPassword: { type: "string", format: "password" }, newPassword: { type: "string", format: "password" } },
              },
            },
          },
        },
        responses: {
          200: {
            description: "Şifre değiştirildi.",
            content: {
              "application/json": {
                schema: { type: "object", properties: { success: { type: "boolean", example: true }, message: { type: "string" } } },
              },
            },
          },
          400: { description: "Mevcut şifre hatalı veya yeni şifre yetersiz.", content: { "application/json": { schema: { $ref: "#/components/schemas/Error" } } } },
          401: { $ref: "#/components/responses/Unauthorized" },
        },
      },
    },

    "/auth/me": {
      get: {
        tags: ["Auth"],
        summary: "Mevcut kullanıcı (JWT payload'ı)",
        responses: {
          200: {
            description: "Access token'daki payload.",
            content: {
              "application/json": {
                schema: {
                  type: "object",
                  properties: {
                    success: { type: "boolean", example: true },
                    user: {
                      type: "object",
                      properties: { userId: { type: "string", format: "uuid" }, email: { type: "string", format: "email" } },
                    },
                  },
                },
              },
            },
          },
          401: { $ref: "#/components/responses/Unauthorized" },
        },
      },
    },

    "/semesters": {
      get: {
        tags: ["Semester"],
        summary: "Dönemleri listele",
        responses: {
          200: { description: "Kullanıcının tüm dönemleri.", content: { "application/json": { schema: { type: "array", items: { $ref: "#/components/schemas/Semester" } } } } },
          401: { $ref: "#/components/responses/Unauthorized" },
        },
      },
      post: {
        tags: ["Semester"],
        summary: "Dönem oluştur",
        requestBody: {
          required: true,
          content: {
            "application/json": {
              schema: {
                type: "object",
                required: ["year", "term"],
                properties: {
                  year: { type: "integer", minimum: 2020, maximum: 2100, example: 2026 },
                  term: { type: "string", enum: ["Güz", "Bahar", "Yaz"] },
                },
              },
            },
          },
        },
        responses: {
          201: { description: "Dönem oluşturuldu.", content: { "application/json": { schema: { $ref: "#/components/schemas/Semester" } } } },
          400: { $ref: "#/components/responses/ValidationFailed" },
          401: { $ref: "#/components/responses/Unauthorized" },
          409: { description: "Bu yıl+dönem kombinasyonu zaten mevcut.", content: { "application/json": { schema: { $ref: "#/components/schemas/Error" } } } },
        },
      },
    },

    "/semesters/{id}": {
      put: {
        tags: ["Semester"],
        summary: "Dönemi güncelle",
        parameters: [{ name: "id", in: "path", required: true, schema: { type: "string", format: "uuid" } }],
        requestBody: {
          required: true,
          content: {
            "application/json": {
              schema: {
                type: "object",
                required: ["year", "term"],
                properties: {
                  year: { type: "integer", minimum: 2020, maximum: 2100 },
                  term: { type: "string", enum: ["Güz", "Bahar", "Yaz"] },
                },
              },
            },
          },
        },
        responses: {
          200: { description: "Güncellendi.", content: { "application/json": { schema: { $ref: "#/components/schemas/Semester" } } } },
          400: { $ref: "#/components/responses/ValidationFailed" },
          401: { $ref: "#/components/responses/Unauthorized" },
          404: { $ref: "#/components/responses/NotFound" },
        },
      },
      delete: {
        tags: ["Semester"],
        summary: "Dönemi sil",
        description: "Cascade: bu döneme bağlı tüm dersler de silinir.",
        parameters: [{ name: "id", in: "path", required: true, schema: { type: "string", format: "uuid" } }],
        responses: {
          200: { description: "Silindi.", content: { "application/json": { schema: { type: "object", properties: { success: { type: "boolean", example: true } } } } } },
          401: { $ref: "#/components/responses/Unauthorized" },
          404: { $ref: "#/components/responses/NotFound" },
        },
      },
    },

    "/courses": {
      get: {
        tags: ["Course"],
        summary: "Dersleri listele",
        responses: {
          200: { description: "Kullanıcının tüm dersleri.", content: { "application/json": { schema: { type: "array", items: { $ref: "#/components/schemas/Course" } } } } },
          401: { $ref: "#/components/responses/Unauthorized" },
        },
      },
      post: {
        tags: ["Course"],
        summary: "Ders oluştur",
        description: "Bileşen ağırlıklarının (`components[].weight`) toplamı tam olarak %100 olmalı.",
        requestBody: {
          required: true,
          content: {
            "application/json": {
              schema: {
                type: "object",
                required: ["semesterId", "name", "credit", "components"],
                properties: {
                  semesterId: { type: "string", format: "uuid" },
                  name: { type: "string", minLength: 2, maxLength: 150, example: "Data Structures" },
                  credit: { type: "integer", minimum: 1, maximum: 30, example: 5 },
                  components: { type: "array", minItems: 1, items: { $ref: "#/components/schemas/CourseComponent" } },
                  gradeScale: { oneOf: [{ $ref: "#/components/schemas/GradeScale" }, { type: "null" }] },
                },
              },
            },
          },
        },
        responses: {
          201: { description: "Ders oluşturuldu (average/letterGrade/gradePoint otomatik hesaplanır).", content: { "application/json": { schema: { $ref: "#/components/schemas/Course" } } } },
          400: { $ref: "#/components/responses/ValidationFailed" },
          401: { $ref: "#/components/responses/Unauthorized" },
        },
      },
    },

    "/courses/{id}": {
      put: {
        tags: ["Course"],
        summary: "Dersi güncelle",
        description: "Tüm alanlar opsiyoneldir (kısmi güncelleme).",
        parameters: [{ name: "id", in: "path", required: true, schema: { type: "string", format: "uuid" } }],
        requestBody: {
          required: true,
          content: {
            "application/json": {
              schema: {
                type: "object",
                properties: {
                  name: { type: "string", minLength: 2, maxLength: 150 },
                  credit: { type: "integer", minimum: 1, maximum: 30 },
                  components: { type: "array", minItems: 1, items: { $ref: "#/components/schemas/CourseComponent" } },
                  gradeScale: { oneOf: [{ $ref: "#/components/schemas/GradeScale" }, { type: "null" }] },
                },
              },
            },
          },
        },
        responses: {
          200: { description: "Güncellendi.", content: { "application/json": { schema: { $ref: "#/components/schemas/Course" } } } },
          400: { $ref: "#/components/responses/ValidationFailed" },
          401: { $ref: "#/components/responses/Unauthorized" },
          404: { $ref: "#/components/responses/NotFound" },
        },
      },
      delete: {
        tags: ["Course"],
        summary: "Dersi sil",
        parameters: [{ name: "id", in: "path", required: true, schema: { type: "string", format: "uuid" } }],
        responses: {
          200: { description: "Silindi.", content: { "application/json": { schema: { type: "object", properties: { success: { type: "boolean", example: true } } } } } },
          401: { $ref: "#/components/responses/Unauthorized" },
          404: { $ref: "#/components/responses/NotFound" },
        },
      },
    },

    "/grade-scale/default": {
      get: {
        tags: ["GradeScale"],
        summary: "Kullanıcının varsayılan harf notu skalasını getir",
        description: "null dönerse sistem varsayılanı (AA/BA/BB.../FF) kullanılıyor demektir.",
        responses: {
          200: {
            description: "Varsayılan skala.",
            content: {
              "application/json": {
                schema: { oneOf: [{ $ref: "#/components/schemas/GradeScale" }, { type: "null" }] },
              },
            },
          },
          401: { $ref: "#/components/responses/Unauthorized" },
        },
      },
      put: {
        tags: ["GradeScale"],
        summary: "Kullanıcının varsayılan harf notu skalasını ayarla",
        description: "`gradeScale: null` gönderilirse sistem varsayılanına dönülür.",
        requestBody: {
          required: true,
          content: {
            "application/json": {
              schema: {
                type: "object",
                required: ["gradeScale"],
                properties: { gradeScale: { oneOf: [{ $ref: "#/components/schemas/GradeScale" }, { type: "null" }] } },
              },
            },
          },
        },
        responses: {
          200: {
            description: "Güncellendi.",
            content: { "application/json": { schema: { oneOf: [{ $ref: "#/components/schemas/GradeScale" }, { type: "null" }] } } },
          },
          400: { $ref: "#/components/responses/ValidationFailed" },
          401: { $ref: "#/components/responses/Unauthorized" },
        },
      },
    },

    "/gpa": {
      get: {
        tags: ["GPA"],
        summary: "Genel Ağırlıklı Not Ortalamasını hesapla",
        description: "Sadece notu girilmiş (average != null) dersler GANO hesabına dahil edilir.",
        responses: {
          200: { description: "Hesaplanan GANO ve ders kırılımı.", content: { "application/json": { schema: { $ref: "#/components/schemas/GpaResult" } } } },
          401: { $ref: "#/components/responses/Unauthorized" },
        },
      },
    },

    "/statistics": {
      get: {
        tags: ["Statistics"],
        summary: "Genel istatistikleri getir",
        responses: {
          200: { description: "Ders/kredi/geçme-kalma istatistikleri.", content: { "application/json": { schema: { $ref: "#/components/schemas/StatisticsResult" } } } },
          401: { $ref: "#/components/responses/Unauthorized" },
        },
      },
    },

    "/dashboard": {
      get: {
        tags: ["Dashboard"],
        summary: "Ana sayfa özet verisini getir",
        responses: {
          200: { description: "Dashboard özet verisi.", content: { "application/json": { schema: { $ref: "#/components/schemas/DashboardResult" } } } },
          401: { $ref: "#/components/responses/Unauthorized" },
        },
      },
    },

    "/transcript": {
      get: {
        tags: ["Transcript"],
        summary: "Transkript görünümünü getir",
        responses: {
          200: {
            description: "Tüm derslerin transkript formatındaki listesi.",
            content: { "application/json": { schema: { type: "array", items: { $ref: "#/components/schemas/TranscriptEntry" } } } },
          },
          401: { $ref: "#/components/responses/Unauthorized" },
        },
      },
    },
  },
};

export default swaggerDocument;