# Guía de Desarrollo - Smart Elevator Manager Frontend

## 🚀 Comenzar a Desarrollar

### Instalación Inicial

```bash
# 1. Instalar dependencias
npm install

# 2. Asegurar que Vite está disponible
npx vite --version

# 3. Iniciar servidor de desarrollo
npm run dev

# 4. El servidor estará en http://localhost:5173
```

## 📁 Estructura del Proyecto

```
src/
├── components/
│   ├── ElevatorSelector.jsx      - Selector de elevadores (tarjetas)
│   ├── ElevatorSelector.css
│   ├── ElevatorPanel.jsx         - Panel principal (LCD + Teclado)
│   ├── ElevatorPanel.css
│   ├── LCDDisplay.jsx            - Pantalla digital
│   ├── LCDDisplay.css
│   ├── ControlKeyboard.jsx       - Teclado de control
│   └── ControlKeyboard.css
├── hooks/
│   ├── useToast.jsx              - Hook para notificaciones (ejemplo)
│   └── Toast.css
├── services/
│   └── ElevatorService.js        - Integración API/SSE
├── App.jsx                       - Componente raíz
├── App.css                       - Estilos globales
└── main.jsx                      - Entry point
```

## 🔧 Extensiones Comunes

### 1. Agregar Notificaciones Toast

```javascript
// En App.jsx
import useToast from './hooks/useToast'

function App() {
  const { Toast, addToast } = useToast()

  const handleSuccess = () => {
    addToast('¡Éxito!', 'success', 3000)
  }

  return (
    <>
      {/* Tu aplicación */}
      <Toast />
    </>
  )
}
```

### 2. Agregar Sonidos Adicionales

```javascript
// En ControlKeyboard.jsx - Ampliar playBeep()

const playSound = (type) => {
  const sounds = {
    beep: { freq: 800, duration: 0.1 },
    ding: { freq: 1200, duration: 0.3 },
    alert: { freq: 600, duration: 0.2 }
  }

  const config = sounds[type] || sounds.beep
  const audioContext = new (window.AudioContext || window.webkitAudioContext)()
  const osc = audioContext.createOscillator()
  const gain = audioContext.createGain()

  osc.frequency.value = config.freq
  gain.gain.setValueAtTime(0.3, audioContext.currentTime)
  gain.gain.exponentialRampToValueAtTime(0.01, audioContext.currentTime + config.duration)

  osc.connect(gain)
  gain.connect(audioContext.destination)
  osc.start()
  osc.stop(audioContext.currentTime + config.duration)
}
```

### 3. Agregar Persistencia (LocalStorage)

```javascript
// En App.jsx
import { useEffect, useState } from 'react'

function App() {
  const [selectedElevatorId, setSelectedElevatorId] = useState(() => {
    return localStorage.getItem('lastElevator') || null
  })

  useEffect(() => {
    if (selectedElevatorId) {
      localStorage.setItem('lastElevator', selectedElevatorId)
    }
  }, [selectedElevatorId])

  // ...
}
```

### 4. Agregar Modo Oscuro/Claro con Persistencia

```javascript
// En App.jsx
const [theme, setTheme] = useState(() => {
  return localStorage.getItem('theme') || 'dark'
})

useEffect(() => {
  localStorage.setItem('theme', theme)
  document.documentElement.setAttribute('data-theme', theme)
}, [theme])

// En App.css - Agregar:
:root[data-theme='light'] {
  --bg-primary: #f8f9fa;
  --text-primary: #1a1a1a;
  /* ... */
}
```

### 5. Agregar Keyboard Shortcuts

```javascript
// Hook personalizado
function useKeyboardShortcuts(callbacks) {
  useEffect(() => {
    const handleKeyPress = (e) => {
      const key = e.key.toUpperCase()
      if (callbacks[key]) {
        callbacks[key]()
      }
    }

    window.addEventListener('keydown', handleKeyPress)
    return () => window.removeEventListener('keydown', handleKeyPress)
  }, [callbacks])
}

// Usar en App.jsx
useKeyboardShortcuts({
  '1': () => handleFloorRequest(1),
  '2': () => handleFloorRequest(2),
  '3': () => handleFloorRequest(3),
  '4': () => handleFloorRequest(4),
  '5': () => handleFloorRequest(5),
  'E': handleOpenDoor,
  'C': handleCloseDoor,
})
```

## 🎨 Personalizar Estilos

### Cambiar Paleta de Colores

```css
/* En App.css - Modificar :root */
:root {
  --primary: #00ff00;           /* Tu color primario */
  --accent-success: #00ff00;    /* Verde */
  --accent-warning: #ffaa00;    /* Naranja */
  --accent-danger: #ff0000;     /* Rojo */
}
```

### Agregar Tema de Marca Corporativa

```css
/* Crear nuevo tema en App.css */
:root.brand-theme {
  --primary: #003366;           /* Azul corporativo */
  --secondary: #006699;
  --accent-success: #00cc33;
  --bg-primary: #f0f5f9;
  --text-primary: #003366;
}
```

## 📊 Estadísticas y Monitoreo

### Agregar Logs de Performance

```javascript
// En Performance.js
export const logPerformance = () => {
  if (window.performance) {
    const metrics = {
      FCP: performance.getEntriesByName('first-contentful-paint')[0],
      LCP: performance.getEntriesByName('largest-contentful-paint')[0],
      FID: performance.getEntriesByName('first-input')[0],
      CLS: performance.getEntriesByName('layout-shift')[0]
    }
    console.table(metrics)
  }
}
```

### Agregar Analytics

```javascript
// En App.jsx
useEffect(() => {
  // Trackear selección de elevador
  trackEvent('elevator_selected', { elevatorId: selectedElevatorId })
}, [selectedElevatorId])

useEffect(() => {
  // Trackear solicitud de piso
  trackEvent('floor_requested', { floor, elevatorId: selectedElevatorId })
}, [])
```

## 🧪 Testing

### Setup Jest + React Testing Library

```bash
npm install -D @testing-library/react @testing-library/jest-dom jest
```

```javascript
// Example test: ElevatorSelector.test.jsx
import { render, screen } from '@testing-library/react'
import ElevatorSelector from './ElevatorSelector'

describe('ElevatorSelector', () => {
  const mockElevators = [
    { id: '1', number: 1, currentFloor: 2, status: 'IDLE' }
  ]

  it('renders elevator cards', () => {
    render(
      <ElevatorSelector
        elevators={mockElevators}
        selectedId="1"
        onSelect={() => {}}
      />
    )
    expect(screen.getByText('Elevator 1')).toBeInTheDocument()
  })
})
```

## 🔗 Integración con Backend

### Cambiar URL de Base

```javascript
// En ElevatorService.js
static BASE_URL = process.env.VITE_API_BASE_URL || '/api'

// En .env.local
VITE_API_BASE_URL=https://api.example.com
```

### Autenticación

```javascript
// En ElevatorService.js - Agregar headers
static async getElevators() {
  const headers = {
    'Authorization': `Bearer ${localStorage.getItem('token')}`
  }
  const response = await fetch(`${this.BASE_URL}/elevators`, { headers })
  return await response.json()
}
```

### Error Handling Mejorado

```javascript
// En App.jsx
const [error, setError] = useState(null)

useEffect(() => {
  ElevatorService.getElevators()
    .catch(err => {
      setError(err.message)
      console.error('Fatal error:', err)
    })
}, [])

if (error) {
  return <ErrorBoundary message={error} />
}
```

## 📱 Testing Responsive

### Simular dispositivos en desarrollo

```javascript
// app.jsx - Agregar helpers de debug
useEffect(() => {
  const removeResizeDebug = () => {
    const size = `${window.innerWidth}x${window.innerHeight}`
    console.log('Viewport:', size)
  }
  window.addEventListener('resize', removeResizeDebug)
  return () => window.removeEventListener('resize', removeResizeDebug)
}, [])
```

### Crear componente de debug

```javascript
function DeviceDebug() {
  if (process.env.NODE_ENV !== 'development') return null

  return (
    <div style={{
      position: 'fixed',
      top: 0,
      left: 0,
      padding: '10px',
      background: 'rgba(0,0,0,0.8)',
      color: '#fff',
      fontSize: '12px',
      zIndex: 9999
    }}>
      {window.innerWidth}x{window.innerHeight}
    </div>
  )
}
```

## 🚀 Deploy

### Build para Producción

```bash
npm run build
```

### Servir archivos estáticos

```bash
# HTTP Server simple
npx http-server dist

# O usar cualquier CDN (Vercel, Netlify, GitHub Pages, etc)
```

### Optimizaciones para Producción

```javascript
// vite.config.js
export default configDef({
  build: {
    minify: 'terser',
    sourcemap: false,
    rollupOptions: {
      output: {
        manualChunks: {
          vendor: ['react', 'react-dom']
        }
      }
    }
  }
})
```

## 🐛 Debugging

### Usar React DevTools

```bash
# Instalar extensión en navegador
# Chrome: React DevTools
# Firefox: React DevTools
```

### Usar Vite Debug

```bash
# Terminal mostrará logs detallados
npm run dev -- --debug
```

### Logs Personalizados

```javascript
// En cualquier componente
const DEBUG = process.env.NODE_ENV === 'development'

DEBUG && console.log('Debugging info:', data)
```

## 📚 Recursos Útiles

- **React Docs**: https://react.dev
- **Vite Docs**: https://vitejs.dev
- **MDN Web Docs**: https://developer.mozilla.org
- **CSS Tricks**: https://css-tricks.com
- **Can I Use**: https://caniuse.com

## ✅ Checklist Pre-Deploy

- [ ] Probar en todos los navegadores principales
- [ ] Verificar responsive en móvil/tablet
- [ ] Revisar performance (Lighthouse)
- [ ] Validar accesibilidad (WCAG)
- [ ] Verificar SSE connection
- [ ] Testing en diferentes conexiones (Fast/Slow 3G)
- [ ] Build y revisar tamaño de bundle
- [ ] Cache-busting para archivos estáticos
- [ ] HTTPS configuration
- [ ] CORS headers correctos

## 🎯 Próximas Mejoras Sugeridas

1. **Analytics**: Integrar con Google Analytics o similar
2. **PWA**: Hacer la app progressive
3. **Offline Mode**: Funcionalidad cuando no hay conexión
4. **Multi-idioma**: i18n
5. **Temas adicionales**: Sistema de temas más extenso
6. **Mapeo Visual**: Mostrar layout del edificio en 3D
7. **Historial**: Ver historial de viajes
8. **Predicción**: ML para predecir siguiente piso
9. **Alertas**: Notificaciones push
10. **Real-time collab**: Múltiples usuarios en mismo panel

---

**Última actualización**: 2026  
**Versión**: 1.0  
**Proyecto**: Smart Elevator Manager
