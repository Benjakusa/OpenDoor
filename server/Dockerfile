FROM node:20-alpine

RUN apk add --no-cache python3 py3-pip ffmpeg && \
    pip3 install --break-system-packages --upgrade yt-dlp

WORKDIR /app
COPY package.json ./
RUN npm install --production
COPY server.js ./

EXPOSE 3000
CMD ["node", "server.js"]
