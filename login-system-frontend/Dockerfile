# ── Stage 1: Build ──────────────────────────────────────────────────────────
FROM node:20-alpine AS build

WORKDIR /app

# Cache node_modules layer
COPY package.json package-lock.json* ./
RUN npm install --legacy-peer-deps

COPY . .

# REACT_APP_* vars are baked in at build time by CRA
ARG REACT_APP_API_URL
ENV REACT_APP_API_URL=${REACT_APP_API_URL}

RUN npm run build

# ── Stage 2: Serve ───────────────────────────────────────────────────────────
FROM nginx:1.27-alpine AS runtime

# Remove default nginx static assets
RUN rm -rf /usr/share/nginx/html/*

COPY --from=build /app/build /usr/share/nginx/html
COPY nginx.conf /etc/nginx/conf.d/default.conf

EXPOSE 3000

CMD ["nginx", "-g", "daemon off;"]
