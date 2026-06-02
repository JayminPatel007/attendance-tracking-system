# syntax=docker/dockerfile:1

# --- Stage 1: build the Angular app into static files ---------------------
FROM node:20-alpine AS build
WORKDIR /app
# Install deps first (cached unless package*.json changes).
COPY package.json package-lock.json ./
RUN npm ci
# Then the source, and build. `npm run build` builds the shared libraries
# first (build:libs) and then the `web` app into dist/web/browser.
COPY . .
RUN npm run build

# --- Stage 2: serve the static files with nginx --------------------------
FROM nginx:alpine
COPY nginx.conf /etc/nginx/conf.d/default.conf
COPY --from=build /app/dist/web/browser /usr/share/nginx/html
EXPOSE 80
