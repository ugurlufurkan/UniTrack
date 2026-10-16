FROM postgres:17-alpine
RUN apk add --no-cache age
COPY backup.sh restore-check.sh /scripts/
RUN chmod 755 /scripts/*.sh
RUN mkdir /status && chmod 755 /status
CMD ["/scripts/backup.sh"]
