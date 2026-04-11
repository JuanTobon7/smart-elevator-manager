# ⚡ Quick Start Guide - Smart Elevator Manager

## 🚀 Comenzar en 5 minutos

### 1️⃣ Instalar Dependencias
```bash
npm install
```

### 2️⃣ Iniciar Servidor de Desarrollo
```bash
npm run dev
```

La aplicación estará en: **http://localhost:5173**

### 3️⃣ Conectar Backend (Opcional - Datos Mock)
El frontend incluye datos mock para desarrollo sin backend.

Para conectar tu backend:
1. Edita `vite.config.js` - Cambia la URL del proxy
2. Edita `.env.local` - Agrega tu URL de API

## 🎯 Qué Ves

```
┌─────────────────────────────────────────────────┐
│  HEADER: Smart Elevator Manager  [🌙 Theme]     │
├────────────────┬─────────────────────────────────┤
│  Selector      │                                  │
│  Elevadores    │   Panel de Control               │
│  - Card 1      │   ┌─────────────┐                │
│  - Card 2  ✓   │   │  LCD Display │                │
│  - Card 3      │   │  PISO 2      │                │
│                │   └─────────────┘                │
│                │                                  │
│                │   ┌─────────────┬─────────────┐  │
│                │   │ 5 │ 4 │ 3 │  │ 2 │ 1 │    │  │
│                │   ├─────────────┤                │
│                │   │ ABRIR │ CERRAR              │
│                │   ├─────────────┤                │
│                │   │  ⚠️ EMERGENCIA             │
│                │   └─────────────┘                │
└─────────────────────────────────────────────────┘
```

## 📁 Estructura de Archivos Importante

```
front/
├── src/
│   ├── components/      ← Componentes React
│   ├── services/        ← Integración API
│   ├── hooks/           ← Custom hooks (Toast)
│   ├── App.jsx          ← Componente principal
│   └── main.jsx         ← Entry point
├── index.html           ← HTML base
├── vite.config.js       ← Configuración Vite
├── package.json         ← Dependencias
└── README.md            ← Documentación completa
```

## ⌨️ Teclas de Desarrollo

- **Números 1-5** (en teclado): Solicitar piso (si implementas)
- **E**: Abrir puerta
- **C**: Cerrar puerta
- **R**: Reset/Emergencia

## 🎨 Cambiar Colores

Edita `src/App.css` - Variables en `:root`:

```css
:root {
  --primary: #2563eb;        /* Cambiar aquí */
  --accent-success: #84cc16; /* Y aquí */
  --accent-warning: #f97316;
  --accent-danger: #ef4444;
}
```

## 🌓 Toggle Tema Claro/Oscuro

Click en el botón ☀️/🌙 en la esquina superior derecha.

## 📱 Ver en Móvil

```bash
# Nota tu IP local
ipconfig getifaddr en0  # Mac
hostname -I              # Linux
ipconfig                 # Windows

# Abre en móvil
http://<tu-ip>:5173
```

## 🔌 Conectar Backend

### Opción A: Proxy en Desarrollo
```javascript
// vite.config.js
proxy: {
  '/api': 'http://tu-backend:3000'
}
```

### Opción B: URL Directa
```javascript
// src/services/ElevatorService.js
static BASE_URL = 'http://tu-backend:3000/api'
```

### Opción C: Variable de Ambiente
```bash
# .env.local
VITE_API_BASE_URL=http://tu-backend:3000/api
```

---

```javascript
// Usar en servicio
static BASE_URL = import.meta.env.VITE_API_BASE_URL || '/api'
```

## 📊 Datos Mock Disponibles

El frontend incluye 3 elevadores mock:
- **Elevator 1**: Piso 2, IDLE
- **Elevator 2**: Piso 5 → 3, MOVING DOWN
- **Elevator 3**: Piso 1 → 4, MOVING UP

## 🐸 El Flujo del Usuario

```
1. Página carga
   └─ Carga elevadores
   └─ Muestra selector

2. Usuario selecciona elevador
   └─ Panel muestra estado en LCD
   └─ Se conecta a SSE para actualizaciones

3. Usuario hace click en botón (piso 5)
   └─ Botón se ilumina
   └─ Se envía solicitud a API
   └─ Beep de confirmación

4. Elevador llega
   └─ Pantalla LCD se actualiza
   └─ Botones ABRIR/CERRAR se iluminan
   └─ Puerta se abre (animación)

5. Elevador se mueve
   └─ Números parpadean
   └─ Dirección respira
   └─ SSE envía updates en tiempo real
```

## 🐛 Debugging

### Ver Logs en Consola
```bash
# Abre DevTools (F12), ve a Console tab
```

### Inspeccionar Componentes React
```bash
# Chrome: Instala "React DevTools" extension
# Firefox: Instala "React DevTools" add-on
# Abre DevTools, ve a "Components" tab
```

### Verificar Conexión SSE
```javascript
// En consola del navegador
eventSource = new EventSource('/api/elevators/elev-1/subscribe')
eventSource.onmessage = (e) => console.log('SSE:', e.data)
```

## ⚡ Performance Tips

### Build Rápido
```bash
npm run build -- --mode development
```

### Analizar Bundle
```bash
npm install --save-dev rollup-plugin-visualizer
# Revisa el tamaño de tus módulos
```

## 🎬 Animaciones Personalizadas

Edita CSS en componentes:
- `ElevatorSelector.css` - Tarjetas
- `LCDDisplay.css` - Pantalla
- `ControlKeyboard.css` - Botones

## 🔊 Añadir Sonidos Reales

```javascript
// src/services/AudioService.js - Crear este archivo
export class AudioService {
  static playBeep() { /* ... */ }
  static playDing() { /* ... */ }
  static playAlert() { /* ... */ }
}

// Usar en componentes
import { AudioService } from '../services/AudioService'
AudioService.playBeep()
```

## 📦 Buildear para Producción

```bash
npm run build
# Output: dist/

# Servir localmente
npm run preview
```

## 🚀 Deploy Opciones

- **Vercel** (Para Next.js/SPA): Drag & drop `dist/`
- **Netlify**: Drag & drop `dist/`
- **GitHub Pages**: Push a rama `gh-pages`
- **AWS S3 + CloudFront**: Upload `dist/`
- **Docker**: Ver DOCKER_GUIDE.md

## 📚 Documentación Completa

```
├── README.md                  ← Visión general
├── QUICKSTART.md             ← Este archivo
├── DEVELOPMENT.md            ← Guía de desarrollo avanzada
├── ANIMATION_DESIGN_GUIDE.md ← Detalles de animaciones
├── API_REFERENCE.md          ← Specificación API
└── DOCKER_GUIDE.md           ← Docker & deployment
```

## ❓ Preguntas Frecuentes

**¿Por qué no cargan los datos?**
- Asegúrate que el backend está corriendo en puerto 3000
- Verifica la URL en `vite.config.js`

**¿Cómo agregar más pisos?**
- Edita `ControlKeyboard.jsx` - array `floors`
- Actualiza CSS en `ControlKeyboard.css`
- Backend debe soportar esos pisos

**¿Los botones no responden?**
- Abre DevTools (F12) → Console
- Busca error rojo
- Verifica que API esté corriendo

**¿Cómo cambiar el nombre?**
- `index.html` - `<title>`
- `App.jsx` - `app-title`
- `package.json` - `name`

## ✅ Checklist Inicial

- [ ] `npm install` corrió sin errores
- [ ] `npm run dev` inició servidor
- [ ] Puedo ver la UI en http://localhost:5173
- [ ] Selector de elevadores muestra 3 elevadores
- [ ] Puedo hacer click en tarjetas (cambian color)
- [ ] Puedo hacer click en botones de piso (se iluminan)
- [ ] Tema oscuro/claro funciona

## 🎉 ¡Listo!

Ya tienes:
✅ UI moderna y animada
✅ Componentes reutilizables
✅ Sistema de estados
✅ Integración SSE (preparada)
✅ Datos mock para desarrollo
✅ Documentación completa

**Próximo paso**: Conecta tu backend y ¡disfruta!

---

**Time to awesome**: ~30 segundos ⚡
**Quality level**: 🌟🌟🌟🌟🌟

**Happy coding! 🚀**
