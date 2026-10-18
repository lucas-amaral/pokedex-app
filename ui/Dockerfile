# syntax=docker/dockerfile:1

# Build stage: type-check and bundle the app into static files.
FROM node:22-alpine AS build
WORKDIR /app

# Dependencies first, so they are installed again only when the lock file changes.
COPY package.json package-lock.json ./
RUN --mount=type=cache,target=/root/.npm npm ci --no-audit --no-fund

COPY . .
RUN npm run build

# Runtime stage: nginx serves the files and forwards /api to the service, like the Vite proxy does in development.
FROM nginx:1.29-alpine
COPY nginx.conf.template /etc/nginx/templates/default.conf.template
COPY --from=build /app/dist /usr/share/nginx/html

# Where the service is reachable from this container; the nginx image fills it into the template at startup.
ENV API_UPSTREAM=http://service:8080
EXPOSE 80
