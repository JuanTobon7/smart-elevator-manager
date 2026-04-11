# Docker & Development Environment

## 🐳 Docker Compose Setup

Usa este archivo para ejecutar toda la aplicación localmente:

```yaml
# docker-compose.yml
version: '3.8'

services:
  # Frontend (React + Vite)
  frontend:
    build:
      context: ./front
      dockerfile: Dockerfile
    ports:
      - "5173:5173"
    environment:
      - VITE_API_BASE_URL=http://localhost:3000/api
    volumes:
      - ./front/src:/app/src
      - ./front/index.html:/app/index.html
    depends_on:
      - backend
    command: npm run dev

  # Backend (Node.js - Replace con tu tech stack)
  # Ejemplo: Express.js
  backend:
    build:
      context: ./../back  # O donde esté tu backend
      dockerfile: Dockerfile
    ports:
      - "3000:3000"
    environment:
      - NODE_ENV=development
      - PORT=3000
      - FRONTEND_URL=http://localhost:5173
    volumes:
      - ./../back/src:/app/src
    restart: on-failure

  # Base de datos (opcional)
  database:
    image: postgres:15-alpine
    ports:
      - "5432:5432"
    environment:
      - POSTGRES_USER=elevator
      - POSTGRES_PASSWORD=password123
      - POSTGRES_DB=elevator_manager
    volumes:
      - postgres_data:/var/lib/postgresql/data

volumes:
  postgres_data:
```

## 📦 Dockerfile para Frontend

```dockerfile
# Dockerfile
FROM node:18-alpine AS builder

WORKDIR /app

COPY package*.json ./

RUN npm install

COPY . .

RUN npm run build

# Nginx para servir en producción
FROM nginx:alpine

COPY --from=builder /app/dist /usr/share/nginx/html

COPY nginx.conf /etc/nginx/conf.d/default.conf

EXPOSE 80

CMD ["nginx", "-g", "daemon off;"]
```

## ⚙️ Nginx Configuration

```nginx
# nginx.conf
server {
    listen 80;
    server_name localhost;

    root /usr/share/nginx/html;
    index index.html;

    # SPA routing - Todas las rutas van a index.html
    location / {
        try_files $uri $uri/ /index.html;
    }

    # Proxy a API
    location /api/ {
        proxy_pass http://backend:3000;
        proxy_http_version 1.1;
        proxy_set_header Upgrade $http_upgrade;
        proxy_set_header Connection "upgrade";
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
    }

    # SSE - Cambiar timeouts
    location /api/elevators/subscribe {
        proxy_pass http://backend:3000;
        proxy_http_version 1.1;
        proxy_set_header Connection "";
        proxy_buffering off;
        proxy_cache off;
        proxy_redirect off;
        proxy_set_header Host $host;
    }

    # Cache estático
    location ~* \.(js|css|png|jpg|jpeg|gif|ico|svg|woff|woff2)$ {
        expires 1y;
        add_header Cache-Control "public, immutable";
    }

    # No cachear HTML
    location ~* \.html$ {
        expires -1;
        add_header Cache-Control "no-cache, no-store, must-revalidate";
    }
}
```

## 🚀 Ejecutar con Docker Compose

```bash
# Construir imágenes
docker-compose build

# Iniciar servicios
docker-compose up

# Ver logs
docker-compose logs -f

# Detener
docker-compose down

# Reiniciar
docker-compose restart

# Ejecutar comando específico
docker-compose exec frontend npm run eslint
```

## 📝 .dockerignore

```
node_modules
dist
.git
.gitignore
README.md
.env
.env.local
.DS_Store
npm-debug.log
yarn-error.log
```

## 🔐 Secrets & Environment en Docker

```yaml
# .env.docker
VITE_API_BASE_URL=http://backend:3000/api
VITE_SSE_ENABLED=true
NODE_ENV=development
```

```bash
# Pasar variables al contenedor
docker-compose -f docker-compose.yml --env-file .env.docker up
```

## 📊 Multi-stage Build (Optimizado)

```dockerfile
# Dockerfile optimizado
FROM node:18-alpine AS dependencies
WORKDIR /app
COPY package*.json ./
RUN npm ci --only=production

FROM node:18-alpine AS builder
WORKDIR /app
COPY package*.json ./
RUN npm ci
COPY . .
RUN npm run build

FROM node:18-alpine
WORKDIR /app
COPY --from=dependencies /app/node_modules ./node_modules
COPY --from=builder /app/dist ./dist
COPY package*.json ./
ENV NODE_ENV=production
EXPOSE 3000
CMD ["npm", "start"]
```

## 🔍 Health Check en Docker

```yaml
services:
  backend:
    healthcheck:
      test: ["CMD", "curl", "-f", "http://localhost:3000/api/health"]
      interval: 30s
      timeout: 10s
      retries: 3
      start_period: 40s

  database:
    healthcheck:
      test: ["CMD-SHELL", "pg_isready -U elevator"]
      interval: 10s
      timeout: 5s
      retries: 5
```

## 📈 Logging en Docker

```yaml
services:
  frontend:
    logging:
      driver: "json-file"
      options:
        max-size: "10m"
        max-file: "3"
        labels: "service=frontend"

volumes:
  logs:
    driver: local
```

## 🛠️ Comandos Útiles

```bash
# Construir imagen específica
docker-compose build frontend

# Ver tamaño de imágenes
docker images

# Limpiar recursos no usados
docker system prune -a

# Ver estadísticas de contenedores
docker stats

# Ejecutar bash en contenedor
docker-compose exec frontend sh

# Ver logs de un servicio
docker-compose logs -f backend --tail 50
```

## 📦 Production Deploy

### Con Docker Hub

```bash
# Login
docker login

# Tag
docker tag smart-elevator-front:latest username/smart-elevator-front:latest

# Push
docker push username/smart-elevator-front:latest

# Pull in production
docker pull username/smart-elevator-front:latest
docker run -p 80:80 username/smart-elevator-front:latest
```

### Con Kubernetes (opcional)

```yaml
# deployment.yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: smart-elevator-frontend
spec:
  replicas: 3
  selector:
    matchLabels:
      app: frontend
  template:
    metadata:
      labels:
        app: frontend
    spec:
      containers:
      - name: frontend
        image: username/smart-elevator-front:latest
        ports:
        - containerPort: 80
        resources:
          requests:
            memory: "64Mi"
            cpu: "250m"
          limits:
            memory: "128Mi"
            cpu: "500m"
        livenessProbe:
          httpGet:
            path: /
            port: 80
          initialDelaySeconds: 30
          periodSeconds: 10
```

## 🚨 Troubleshooting

### Puerto ya en uso
```bash
# Cambiar puerto en docker-compose.yml
ports:
  - "5174:5173"  # Host:Container

# O encontrar qué usa el puerto
lsof -i :5173
```

### Contenedor no inicia
```bash
docker-compose logs frontend --tail 100
```

### Cache de npm
```bash
docker-compose build --no-cache frontend
```

### Permisos en volumes
```bash
# Desde host (Linux)
sudo chown -R $USER:$USER ./src
```

---

**Docker Guide v1.0**  
**Proyecto**: Smart Elevator Manager
