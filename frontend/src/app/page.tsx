'use client'

import { useState, useEffect } from 'react'
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card'
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from '@/components/ui/table'
import { Button } from '@/components/ui/button'
import { Badge } from '@/components/ui/badge'
import { Input } from '@/components/ui/input'
import { Download, FileText, Calendar, Building, User, Search, X } from 'lucide-react'
import { format } from 'date-fns'
import { es } from 'date-fns/locale'

interface DispatchGuide {
  codEmp: string;
  tipoDoc: number;
  caf: number;
  rutCliente: string;
  razonSocial: string;
  fechaEmision: string;
  total: number;
  npdf: string | null;
  sysOrigen: string;
  pdfUrl: string;
}

export default function DispatchGuidesPage() {
  const [guides, setGuides] = useState<DispatchGuide[]>([])
  const [filteredGuides, setFilteredGuides] = useState<DispatchGuide[]>([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)
  const [searchTerm, setSearchTerm] = useState('')

  useEffect(() => {
    fetchGuides()
  }, [])

  const fetchGuides = async () => {
    try {
      setLoading(true)
      const response = await fetch('http://localhost:8080/api/pdf/dispatch-guides/all')
      if (!response.ok) {
        throw new Error('Error al cargar las guías')
      }
      const data = await response.json()
      
      // El backend ya filtra por sys_origen = "FRUSYSFRPK-FP" y "FRUSYSFRPK-TI"
      // Ordenar por fecha de emisión (más reciente primero) y luego por CAF (descendente)
      const sortedData = data.sort((a: DispatchGuide, b: DispatchGuide) => {
        const dateA = new Date(a.fechaEmision).getTime()
        const dateB = new Date(b.fechaEmision).getTime()
        
        if (dateA !== dateB) {
          return dateB - dateA // Más reciente primero
        }
        
        return b.caf - a.caf // CAF más alto primero si la fecha es igual
      })
      
      // Tomar solo las últimas 100 guías
      const limitedData = sortedData.slice(0, 100)
      setGuides(limitedData)
      setFilteredGuides(limitedData)
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Error desconocido')
    } finally {
      setLoading(false)
    }
  }

  // Función para filtrar guías basado en el término de búsqueda
  const filterGuides = (searchTerm: string) => {
    if (!searchTerm.trim()) {
      setFilteredGuides(guides)
      return
    }

    const filtered = guides.filter((guide) => {
      const search = searchTerm.toLowerCase()
      return (
        guide.caf.toString().includes(search) ||
        guide.rutCliente.toLowerCase().includes(search) ||
        guide.razonSocial.toLowerCase().includes(search) ||
        guide.codEmp.toLowerCase().includes(search) ||
        format(new Date(guide.fechaEmision), 'dd/MM/yyyy').includes(search)
      )
    })
    setFilteredGuides(filtered)
  }

  // Manejar cambios en el input de búsqueda
  const handleSearchChange = (value: string) => {
    setSearchTerm(value)
    filterGuides(value)
  }

  // Limpiar búsqueda
  const clearSearch = () => {
    setSearchTerm('')
    setFilteredGuides(guides)
  }

  // Actualizar filteredGuides cuando guides cambie
  useEffect(() => {
    filterGuides(searchTerm)
  }, [guides])

  const handleDownloadPdf = async (guide: DispatchGuide) => {
    try {
      // Mostrar mensaje apropiado según el estado del PDF
      if (!guide.npdf) {
        console.log('Generando PDF para la guía:', guide.caf)
        // Opcional: mostrar un indicador de carga aquí
      }
      
      const response = await fetch(`http://localhost:8080${guide.pdfUrl}`)
      if (!response.ok) {
        if (response.status === 404) {
          throw new Error('PDF no encontrado. Verifique que la guía existe.')
        } else if (response.status === 500) {
          throw new Error('Error interno del servidor al generar el PDF.')
        } else {
          throw new Error(`Error al procesar la solicitud: ${response.status}`)
        }
      }
      
      const blob = await response.blob()
      const url = window.URL.createObjectURL(blob)
      const a = document.createElement('a')
      a.style.display = 'none'
      a.href = url
      
      // Extraer nombre del archivo del header Content-Disposition si existe
      const contentDisposition = response.headers.get('Content-Disposition')
      let filename = `guia_despacho_${guide.codEmp}_${guide.tipoDoc}_${guide.caf}.pdf`
      
      if (contentDisposition) {
        const filenameMatch = contentDisposition.match(/filename[^;=\n]*=((['"]).*?\2|[^;\n]*)/)
        if (filenameMatch) {
          filename = filenameMatch[1].replace(/['"]/g, '')
        }
      }
      
      a.download = filename
      document.body.appendChild(a)
      a.click()
      window.URL.revokeObjectURL(url)
      document.body.removeChild(a)
      
      // Si el PDF se generó exitosamente, actualizar la lista para reflejar el cambio
      if (!guide.npdf) {
        console.log('PDF generado exitosamente. Actualizando lista...')
        // Opcional: refrescar la lista para mostrar el estado actualizado
        setTimeout(() => {
          fetchGuides()
        }, 1000)
      }
    } catch (err) {
      console.error('Error al procesar PDF:', err)
      const errorMessage = err instanceof Error ? err.message : 'Error desconocido al procesar el PDF'
      alert(errorMessage)
    }
  }

  const formatCurrency = (amount: number) => {
    return new Intl.NumberFormat('es-CL', {
      style: 'currency',
      currency: 'CLP',
      minimumFractionDigits: 0,
    }).format(amount)
  }

  const formatDate = (dateString: string) => {
    try {
      const date = new Date(dateString)
      return format(date, 'dd/MM/yyyy HH:mm', { locale: es })
    } catch {
      return dateString
    }
  }

  if (loading) {
    return (
      <div className="container mx-auto p-6">
        <div className="flex items-center justify-center h-64">
          <div className="text-lg">Cargando guías de despacho...</div>
        </div>
      </div>
    )
  }

  if (error) {
    return (
      <div className="container mx-auto p-6">
        <Card className="border-red-200">
          <CardContent className="p-6">
            <div className="text-red-600">Error: {error}</div>
            <Button onClick={fetchGuides} className="mt-4">
              Reintentar
            </Button>
          </CardContent>
        </Card>
      </div>
    )
  }

  return (
    <div className="container mx-auto p-6">
      <Card>
        <CardHeader>
          <CardTitle className="flex items-center gap-2">
            <FileText className="h-6 w-6" />
            Guías de Despacho y Traslados Electrónicos
          </CardTitle>
          <p className="text-muted-foreground">
            Mostrando {filteredGuides.length} de {guides.length} guías de despacho (FP) y traslados (TI) - máximo 100
          </p>
        </CardHeader>
        <CardContent>
          {/* Buscador */}
          <div className="mb-6">
            <div className="relative">
              <Search className="absolute left-3 top-1/2 transform -translate-y-1/2 h-4 w-4 text-muted-foreground" />
              <Input
                placeholder="Buscar por CAF, RUT, Razón Social, Empresa o Fecha (dd/mm/yyyy)..."
                value={searchTerm}
                onChange={(e: React.ChangeEvent<HTMLInputElement>) => handleSearchChange(e.target.value)}
                className="pl-10 pr-10"
              />
              {searchTerm && (
                <Button
                  variant="ghost"
                  size="sm"
                  onClick={clearSearch}
                  className="absolute right-1 top-1/2 transform -translate-y-1/2 h-8 w-8 p-0"
                >
                  <X className="h-4 w-4" />
                </Button>
              )}
            </div>
            {searchTerm && (
              <p className="text-sm text-muted-foreground mt-2">
                {filteredGuides.length} resultado{filteredGuides.length !== 1 ? 's' : ''} encontrado{filteredGuides.length !== 1 ? 's' : ''} para "{searchTerm}"
              </p>
            )}
          </div>

          <div className="rounded-md border">
            <Table>
              <TableHeader>
                <TableRow>
                  <TableHead className="w-[100px]">
                    <div className="flex items-center gap-1">
                      <Building className="h-4 w-4" />
                      Empresa
                    </div>
                  </TableHead>
                  <TableHead className="w-[120px]">
                    <div className="flex items-center gap-1">
                      <User className="h-4 w-4" />
                      RUT Cliente
                    </div>
                  </TableHead>
                  <TableHead className="w-[100px]">CAF</TableHead>
                  <TableHead>Razón Social</TableHead>
                  <TableHead className="w-[140px]">
                    <div className="flex items-center gap-1">
                      <Calendar className="h-4 w-4" />
                      Fecha Emisión
                    </div>
                  </TableHead>
                  <TableHead className="w-[120px] text-right">Total</TableHead>
                  <TableHead className="w-[100px]">NPDF</TableHead>
                  <TableHead className="w-[120px]">Sistema Origen</TableHead>
                  <TableHead className="w-[100px] text-center">Acciones</TableHead>
                </TableRow>
              </TableHeader>
              <TableBody>
                {filteredGuides.map((guide) => {
                  // Determinar el estado de la guía para aplicar estilos
                  const isNotGenerated = !guide.npdf
                  
                  // Clases para la fila según el estado
                  const rowClasses = isNotGenerated 
                    ? "bg-red-50 hover:bg-red-100 border-red-200" 
                    : ""
                  
                  return (
                    <TableRow key={`${guide.codEmp}-${guide.tipoDoc}-${guide.caf}`} className={rowClasses}>
                      <TableCell className="font-medium">{guide.codEmp}</TableCell>
                      <TableCell className="font-mono text-sm">{guide.rutCliente}</TableCell>
                      <TableCell className="font-mono">{guide.caf}</TableCell>
                      <TableCell className="max-w-[200px] truncate" title={guide.razonSocial}>
                        {guide.razonSocial}
                      </TableCell>
                      <TableCell className="text-sm">
                        {formatDate(guide.fechaEmision)}
                      </TableCell>
                      <TableCell className="text-right font-medium">
                        {formatCurrency(guide.total)}
                      </TableCell>
                      <TableCell>
                        {guide.npdf ? (
                          <Badge variant="outline" className="font-mono text-xs">
                            PDF Generado
                          </Badge>
                        ) : (
                          <Badge variant="destructive" className="text-xs">
                            Guía no generada
                          </Badge>
                        )}
                      </TableCell>
                      <TableCell className="font-mono text-xs">
                        <Badge 
                          variant={guide.sysOrigen === 'FRUSYSFRPK-FP' ? 'default' : 'outline'} 
                          className="text-xs"
                        >
                          {guide.sysOrigen === 'FRUSYSFRPK-FP' ? 'Despacho' : 'Traslado'}
                        </Badge>
                      </TableCell>
                      <TableCell>
                        <div className="flex justify-center">
                          <Button
                            size="sm"
                            variant="outline"
                            onClick={() => handleDownloadPdf(guide)}
                            className="h-8 w-8 p-0"
                            title={guide.npdf ? "Descargar PDF" : "Generar y Descargar PDF"}
                          >
                            <Download className="h-4 w-4" />
                          </Button>
                        </div>
                      </TableCell>
                    </TableRow>
                  )
                })}
              </TableBody>
            </Table>
          </div>
          
          {/* Mensaje cuando no hay guías */}
          {guides.length === 0 && !loading && (
            <div className="text-center py-8 text-muted-foreground">
              No se encontraron guías de despacho ni traslados
            </div>
          )}
          
          {/* Mensaje cuando no hay resultados de búsqueda */}
          {filteredGuides.length === 0 && guides.length > 0 && searchTerm && (
            <div className="text-center py-8 text-muted-foreground">
              <Search className="h-8 w-8 mx-auto mb-2 opacity-50" />
              <p>No se encontraron guías que coincidan con "{searchTerm}"</p>
              <Button 
                variant="outline" 
                size="sm" 
                onClick={clearSearch}
                className="mt-2"
              >
                Limpiar búsqueda
              </Button>
            </div>
          )}
        </CardContent>
      </Card>
    </div>
  )
}
