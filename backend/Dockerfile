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

FROM builder AS migrator
CMD ["npm", "run", "db:migrate"]

# --- production stage: sadece derlenmiş çıktı + prod bağımlılıkları ---
FROM node:22-alpine AS runner
WORKDIR /app
ENV NODE_ENV=production

COPY package*.json ./
RUN npm ci --omit=dev

COPY --from=builder /app/dist ./dist

EXPOSE 5000
USER node
HEALTHCHECK --interval=30s --timeout=5s --start-period=20s CMD node -e "fetch('http://127.0.0.1:5000/api/health').then(r=>process.exit(r.ok?0:1)).catch(()=>process.exit(1))"
CMD ["node", "dist/server.js"]
