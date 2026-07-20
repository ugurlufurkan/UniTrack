# UniTrack backend — production image.
# Render / Railway / Fly.io hepsi bir Dockerfile'ı otomatik algılayıp
# deploy edebilir; repoyu bağlayıp env değişkenlerini (bkz. .env.example)
# panelden girmen yeterli.

FROM node:22-alpine AS builder
WORKDIR /app

COPY package*.json ./
RUN npm ci

COPY . .
RUN npm run build

# --- production stage: sadece derlenmiş çıktı + prod bağımlılıkları ---
FROM node:22-alpine AS runner
WORKDIR /app
ENV NODE_ENV=production

COPY package*.json ./
RUN npm ci --omit=dev

COPY --from=builder /app/dist ./dist

EXPOSE 5000
CMD ["node", "dist/server.js"]
