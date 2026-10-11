# =====================================================================
# integrity-core-fr — Frontend Ionic + Angular
# Multi-stage build: compila con Node, sirve el build estático con nginx
# =====================================================================

# ---------- Etapa 1: build ----------
FROM node:24 AS build
WORKDIR /app

# Copiamos primero solo los manifiestos para aprovechar cache de capas
COPY package.json package-lock.json ./
RUN npm ci

# Ahora el resto del código
COPY . .

# Build de producción de Angular (ajusta el nombre del proyecto si tu
# angular.json define uno distinto al default; revisa la carpeta que
# genera "dist/" si el build falla por nombre)
RUN npm run build -- --configuration production

# ---------- Etapa 2: runtime ----------
FROM nginx:alpine AS runtime

# Quita la config default de nginx y pon la nuestra (soporte de rutas SPA)
RUN rm /etc/nginx/conf.d/default.conf
COPY nginx.conf /etc/nginx/conf.d/default.conf

# Angular 22 genera el bundle en la carpeta configurada como outputPath.
COPY --from=build /app/www /usr/share/nginx/html

EXPOSE 80

HEALTHCHECK --interval=30s --timeout=5s --start-period=15s --retries=3 \
  CMD wget -qO- http://localhost:80/ || exit 1

CMD ["nginx", "-g", "daemon off;"]
