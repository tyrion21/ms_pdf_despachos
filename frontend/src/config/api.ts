// Configuración de API
export const API_CONFIG = {
  // URL base del API - puede ser sobrescrita por variable de entorno
  baseURL: process.env.NEXT_PUBLIC_API_URL || "http://192.168.7.26:8090",
  // baseURL: process.env.NEXT_PUBLIC_API_URL || "http://localhost:8080",

  // Endpoints específicos
  endpoints: {
    dispatchGuides: "/api/pdf/dispatch-guides/all",
    generatePdf: (codEmp: string, tipoDoc: number, caf: number) =>
      `/api/pdf/dispatch-guide/${codEmp}/${tipoDoc}/${caf}`,
    health: "/actuator/health",
  },
};

// Función helper para construir URLs completas
export const buildApiUrl = (endpoint: string): string => {
  // Si ya es una URL completa, devolverla tal como está
  if (endpoint.startsWith('http')) {
    return endpoint
  }
  
  // Siempre usar URL completa para evitar problemas de proxy
  return `${API_CONFIG.baseURL}${endpoint}`
}

// URLs específicas para usar en el código
export const API_URLS = {
  dispatchGuides: buildApiUrl(API_CONFIG.endpoints.dispatchGuides),
  health: buildApiUrl(API_CONFIG.endpoints.health)
}