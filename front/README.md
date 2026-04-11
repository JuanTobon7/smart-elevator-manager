# Smart Elevator Manager - Frontend

Una interfaz web moderna, interactiva y altamente animada para controlar múltiples elevadores en tiempo real. Diseño elegante y responsivo que simula un verdadero panel de control de elevador corporativo.

## ✨ Características

### 🏢 Selector de Elevadores
- Tarjetas visuales para múltiples elevadores
- Visualización de piso actual con animaciones
- Indicadores de dirección (↑ ↓ ⊗)
- Estado del elevador en tiempo real
- Efecto de levitación y animaciones suaves
- Indicador LED pulsante para elevador seleccionado

### 🎛️ Panel de Control
- **Pantalla LCD Digital**
  - Piso actual en grande (150px)
  - Piso destino
  - Estado del elevador
  - Indicador de dirección animado
  - Barra de capacidad/peso
  - Efecto de escaneo horizontal

- **Teclado de Control**
  - Botones de piso 1-5 (60x60px)
  - Botones ABRIR y CERRAR
  - Botón de EMERGENCIA con animación de respiración
  - Efectos visuales: Press down, ripple, glow
  - Retroalimentación instantánea

### 🎬 Animaciones Principales
- **Selector**: Deslizamiento desde la izquierda (500ms)
- **Panel**: Fade-in con escala (600ms)
- **Tarjetas**: Efecto stagger (100ms entre cada)
- **Números**: Efecto flip al cambiar piso
- **Botones**: Press, scale, ripple, glow
- **Indicadores**: Parpadeo, respiración, pulsación
- **Estados**: Animaciones contextuales según status

### 📱 Responsivo
- Desktop (1920px+)
- Tablet (768px-1200px)
- Mobile (< 768px)
- Layout adaptativo automático

### 🌓 Tema Configurable
- Modo oscuro por defecto (corporativo)
- Toggle para cambiar tema
- Colores optimizados para accesibilidad

## 🚀 Instalación

```bash
# Instalar dependencias
npm install

# Iniciar servidor de desarrollo
npm run dev

# Compilar para producción
npm run build

# Vista previa de producción
npm run preview
```

## 🏗️ Estructura del Proyecto

```
smart-elevator-manager-front/
├── src/
│   ├── components/
│   │   ├── ElevatorSelector.jsx       # Selector de elevadores
│   │   ├── ElevatorSelector.css
│   │   ├── ElevatorPanel.jsx          # Panel principal
│   │   ├── ElevatorPanel.css
│   │   ├── LCDDisplay.jsx             # Pantalla digital
│   │   ├── LCDDisplay.css
│   │   ├── ControlKeyboard.jsx        # Teclado de control
│   │   └── ControlKeyboard.css
│   ├── services/
│   │   └── ElevatorService.js         # Integración con API/SSE
│   ├── App.jsx                        # Componente principal
│   ├── App.css
│   └── main.jsx                       # Punto de entrada
├── index.html
├── vite.config.js
├── package.json
└── .gitignore
```

## 🔌 Integración con Backend

El frontend se conecta a la API REST en `http://localhost:3000/api`

### Endpoints Esperados

```javascript
GET  /api/elevators                    // Lista de elevadores
GET  /api/elevators/:id                // Estado de un elevador
POST /api/elevators/:id/request-floor  // Solicitar piso
POST /api/elevators/:id/open-door      // Abrir puerta
POST /api/elevators/:id/close-door     // Cerrar puerta
```

### Server-Sent Events (SSE)

```javascript
GET /api/elevators/:id/subscribe       // Stream de actualizaciones
```

Formato de datos esperado:

```json
{
  "id": "elev-1",
  "number": 1,
  "currentFloor": 2,
  "destinationFloor": 5,
  "status": "MOVING",
  "direction": "UP",
  "weight": 45
}
```

## 🎨 Paleta de Colores

```css
Fondo: #0d1117 → #1a1f2e (gradiente)
Primario: #2563eb (azul)
Secundario: #6b7280 (gris)
Éxito/Activo: #84cc16 (verde lima)
Acción: #f97316 (naranja)
Emergencia: #ef4444 (rojo)
```

## ⌨️ Atajos de Teclado (Opcional)

Puedes extender la funcionalidad con atajos:

```javascript
// Números 1-5: Solicitar piso
// E: Abrir puerta
// C: Cerrar puerta
// R: Reset/Emergencia
```

## 🎯 Estados del Elevador

- **IDLE**: En reposo (pantalla tranquila)
- **MOVING**: En movimiento (parpadeo, números cambian)
- **DOOR_OPEN**: Puerta abierta (botones iluminados)
- **DOOR_CLOSING**: Cerrando puerta (efecto visual)
- **EMERGENCY**: Error/Emergencia (pantalla roja, alerta)

## 🔊 Sonidos (Opcional)

El sistema incluye un generador de beeps para:
- Click en botones (800Hz, 100ms)
- Extensible para otros eventos

```javascript
// Agregar sonido de ding al llegar
// Agregar alarma en emergencia
// Agregar sonido de cierre de puerta
```

## 📊 Datos Mock

Para desarrollo sin backend, el servicio proporciona datos mock:

```javascript
// ElevatorService.getMockElevators()
// Retorna 3 elevadores de ejemplo
```

## 🔧 Configuración

### Proxy en Desarrollo

```javascript
// vite.config.js
proxy: {
  '/api': 'http://localhost:3000',
  '/events': {
    target: 'http://localhost:3000',
    changeOrigin: true
  }
}
```

### Variables de Entorno

```
VITE_API_BASE_URL=http://localhost:3000
VITE_SSE_ENABLED=true
```

## 📦 Dependencias

- **React 18.2.0**: UI library
- **Vite 4.3.9**: Build tool & dev server
- **Lucide React 0.263.1**: Icon library (opcional)

## 🎓 Criterios de Aceptación

✓ Selector de elevadores visualmente atractivo  
✓ Tarjetas se actualizan en tiempo real  
✓ Panel fácil de usar e intuitivo  
✓ Botones responden instantáneamente  
✓ Animaciones suaves a 60fps  
✓ Diseño responsive en todos los dispositivos  
✓ Conexión correcta con API REST  
✓ Eventos SSE visualizados adecuadamente  
✓ Sensación de calidad profesional  
✓ ¡Divertido y bonito de usar! 😎

## 🚀 Tips de Desarrollo

1. **Animaciones**: Usa CSS transforms para mejor performance
2. **Responsive**: Mobile-first approach
3. **Accesibilidad**: ARIA labels y navegación por teclado
4. **Performance**: Lazy loading de componentes
5. **Debugging**: Abre la consola para ver logs del servicio

## 📝 Licencia

Proyecto académico - Smart Elevator Manager

## 👨‍💻 Autor

Creado como proyecto de universidad avanzada

---

**¡Disfruta controlando elevadores de forma épica! 🚀**
