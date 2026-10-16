FROM node:22-alpine
WORKDIR /app
COPY monitor.mjs ./
RUN mkdir /state && chown node:node /state
USER node
CMD ["node", "monitor.mjs"]
