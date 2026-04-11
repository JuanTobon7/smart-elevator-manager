# Documentación de Animaciones y Diseño

## 🎬 Animaciones Principales

### 1. Entrada de Página (500-600ms)

```
Timeline:
├─ 0ms: Selector desliza desde izquierda (slideInLeft)
├─ 200ms: Panel aparece con fade-in
├─ 0-500ms: Tarjetas de elevadores entran con stagger (100ms cada una)
```

**CSS:**
```css
@keyframes slideInLeft {
  from { opacity: 0; transform: translateX(-100%); }
  to { opacity: 1; transform: translateX(0); }
}

animation: slideInLeft 0.5s ease-out;
```

### 2. Cambio de Elevador (200-300ms)

```
Timeline:
├─ 0-200ms: Panel anterior fade-out
├─ 100-400ms: Nuevo panel slide-in desde abajo
├─ Sincronización de animaciones de estado
```

### 3. Cambio de Piso (300ms)

```
Efecto flip similar a reloj analógico:
├─ 0ms: Start flip
├─ 150ms: Medio flip (rotateX 90deg)
├─ 300ms: End flip
```

**CSS:**
```css
@keyframes flipFloor {
  0% { transform: rotateX(0deg); }
  50% { transform: rotateX(90deg); }
  100% { transform: rotateX(0deg); }
}
```

### 4. Interacción de Botones

```
Secuencia:
├─ Hover (150ms):
│  ├─ Scale: 1 → 1.05
│  ├─ Glow distance: +10px
│  └─ Border color: Gray → Blue
│
├─ Click/Active (200ms):
│  ├─ Scale: 1 → 0.9 (presión)
│  ├─ Ripple effect (onda expandible)
│  └─ Color: Cambio suave
│
└─ Release (150ms):
   └─ Scale: 0.9 → 1 (resorte)
```

### 5. Indicadores LED

```
Parpadeo en pisos solicitados:
├─ 0-400ms: Opaco (1.0)
├─ 400-800ms: Transparente (0.4)
└─ Repeat cada 800ms

Glow cinético:
├─ 0ms: 0 0 4px
├─ 50%: 0 0 12px
└─ 100%: 0 0 4px
```

### 6. Respiración de Dirección

```
Escala y glow pulsante:
├─ 0ms: scale 1.0, glow 10px
├─ 50%: scale 1.15, glow 25px
└─ 100%: scale 1.0, glow 10px
```

### 7. Estados de Elevador

#### IDLE (En reposo)
```
├─ Números: Estáticos
├─ Dirección: ⊗ (sin animación)
├─ Botones: Opacidad 1.0
└─ Pantalla: Color neutro
```

#### MOVING (En movimiento)
```
├─ Números: Parpadean (flicker 1.5s)
├─ Dirección: Respira (breathing 2s)
├─ Piso destino: Intenso glow
├─ Botones: Opacidad 0.5 (deshabilitados)
└─ Estado badge: Parpadea azul
```

#### DOOR_OPEN (Puerta abierta)
```
├─ Estado: "PUERTA ABIERTA" (pulsante)
├─ Botones ABRIR/CERRAR: Iluminados (glow)
├─ Botones piso: Deshabilitados
└─ Estado badge: Parpadea naranja
```

#### EMERGENCY (Emergencia)
```
├─ Pantalla: Rojo (#ef4444)
├─ Texto: Parpadea rápido
├─ Botón Emergencia: Alerta (scale 1.05)
└─ Estado badge: Alerta contínua
```

## 🎨 Paleta Visual

### Colores Base
```css
Primario:      #2563eb (Azul)
Éxito:         #84cc16 (Verde Lima)
Acción:        #f97316 (Naranja)
Emergencia:    #ef4444 (Rojo)
Fondo principal: #0d1117
Fondo secundario: #1a1f2e
Texto principal: #e2e8f0
```

### Glow Effects
```css
--glow-primary: rgba(37, 99, 235, 0.5)    /* Azul con transparencia */
--glow-success: rgba(132, 204, 22, 0.5)   /* Verde con transparencia */
--glow-danger:  rgba(239, 68, 68, 0.5)    /* Rojo con transparencia */
```

## 🔄 Transiciones Suaves

```css
/* Corta - Para feedback inmediato */
--transition-fast: 150ms cubic-bezier(0.4, 0, 0.2, 1)

/* Normal - Para cambios visuales */
--transition-base: 300ms cubic-bezier(0.4, 0, 0.2, 1)

/* Lenta - Para animaciones de entrada/salida */
--transition-slow: 500ms cubic-bezier(0.4, 0, 0.2, 1)
```

## 📏 Tipografía

```css
Body: 'Inter', sans-serif
  Font weight: 300, 400, 500, 600, 700
  
Mono: 'JetBrains Mono', monospace
  Font weight: 400, 600
  Usada en: Números de piso, LCD display, etiquetas
```

## 🎯 Espaciado y Dimensiones

```
Botones:
├─ Pisos: 60x60px (hover 65x65px)
├─ Acción: Flexible, mín 50px alto
└─ Emergencia: Full width

Panel:
├─ Padding: 2rem (desktop), 1rem (tablet), 0.75rem (mobile)
├─ Espacio entre secciones: 2rem
└─ Radio bordes: 20px (panel), 12px (componentes)

Tarjetas:
├─ Ancho: 280-320px (desktop)
├─ Alto: Variable según contenido
├─ Espacio entre: 1rem
└─ Radio bordes: 12px
```

## 🎬 Performance - Optimizaciones

### 1. Usar Transform en lugar de Position
```css
/* ❌ Evitar */
top: 10px; left: 10px; /* Trigger layout recalc */

/* ✅ Usar */
transform: translate(10px);
```

### 2. Will-change para animaciones
```css
.elemento-animado {
  will-change: transform, opacity;
  transform: translateZ(0); /* Hardware acceleration */
}
```

### 3. Debounce de eventos
```javascript
// Aplicado a resize events
const debouncedResize = debounce(() => {
  // Recalcular layout
}, 250)
```

### 4. Lazy Loading
```javascript
// Componentes cargados bajo demanda
const LCDDisplay = lazy(() => import('./LCDDisplay'))
```

## 📱 Breakpoints Responsivos

```css
Desktop:     1920px+     (grid 2 columnas)
Laptop:      1024px      (ajuste de padding)
Tablet:      768px       (layout vertical, grid 1 columna)
Mobile:      480px       (botones más pequeños)

/* Cambios principales en tablet: */
├─ Selector: Horizontal en lugar de vertical
├─ Panel: Columna única
├─ Grid pisos: Dos filas en lugar de 5x1
└─ Font sizes: Reducidas 10-15%
```

## 🌓 Tema Claro (Light Mode)

```css
/* Modificaciones al cambiar tema */
--bg-primary:      #f8f9fa
--bg-secondary:    #ffffff
--text-primary:    #1a1a1a
--text-secondary:  #404040
--primary:         #2563eb (sin cambio)
--accent-success:  #84cc16 (sin cambio)
```

## 🎪 Micro-Interacciones Deliciosas

### 1. Button Ripple Effect
```javascript
// Click genera onda desde punto de click
const ripple = (event) => {
  const rect = event.target.getBoundingClientRect()
  const size = Math.max(rect.width, rect.height)
  const x = event.clientX - rect.left - size / 2
  const y = event.clientY - rect.top - size / 2
  // Animate ripple
}
```

### 2. Smooth Number Transitions
```javascript
// Animar cambio de números con easing
const animateNumber = (from, to, duration) => {
  const steps = 60
  const increment = (to - from) / steps
  // ...
}
```

### 3. Floaty Animation
```css
@keyframes float {
  0%, 100% { transform: translateY(0); }
  50% { transform: translateY(-10px); }
}

/* Aplicado a cards del selector */
animation: float 3s ease-in-out infinite;
```

## 🔊 Sonidos (Audio Context API)

```javascript
const playBeep = (frequencies = [800]) => {
  const audioContext = new AudioContext()
  const osc = audioContext.createOscillator()
  const gain = audioContext.createGain()
  
  osc.frequency.value = frequencies[0]
  gain.gain.setValueAtTime(0.3, audioContext.currentTime)
  gain.gain.exponentialRampToValueAtTime(0.01, audioContext.currentTime + 0.1)
  
  osc.connect(gain)
  gain.connect(audioContext.destination)
  osc.start()
}
```

## 🎓 Casos de Uso y Ejemplos

### 1. Solicitar Piso
```
Usuario hace click en piso 5
├─ 0ms: Botón entra en estado "pressed"
├─ 150ms: Visual feedback (scale 0.9)
├─ 200ms: Botón se relaja (scale 1.0)
├─ 200-400ms: Beep sonoro
├─ 300ms+: Botón pasa a estado "requested" (glow verde)
└─ Request enviado a API
```

### 2. Elevador Llega
```
Server envía evento ARRIVED
├─ 0-200ms: Flash en pantalla
├─ 200-300ms: Botón ABRIR brilla
├─ 300-400ms: Ding sonoro
├─ 500ms+: Estado cambia a "DOOR_OPEN"
└─ Botones piso deshabilitados
```

### 3. Cambio de Elevador
```
Usuario selecciona otro elevador
├─ 0-200ms: Fade out del panel anterior
├─ 100-300ms: Tarjeta anterior pierde glow
├─ 200-300ms: Tarjeta nueva gana glow
├─ 300-500ms: Panel nuevo slide-in y fade-in
└─ Estado sincronizado con servidor
```

---

**Documentación Completa de Animaciones y Diseño**
- **Versión**: 1.0
- **Última actualización**: 2026
- **Proyecto**: Smart Elevator Manager
