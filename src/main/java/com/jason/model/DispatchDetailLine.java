package com.jason.model;

/**
 * Representa una línea de detalle completa devuelta por el
 * SP_GUIA_DESPACHO_ELECTRONICA
 */
public class DispatchDetailLine {
    // Campos existentes del detalle
    private Integer lineNumber; // NRO_LINEA
    private String productCode; // CODIGO_PRODUCTO
    private String description; // ITEM_DESCRIPCION
    private Integer quantity; // CANTIDAD
    private Float unitPrice; // PRECIO_UNITARIO
    private Float lineTotal; // TOTAL_LINEA

    // NUEVOS CAMPOS DEL ENCABEZADO (se repiten en cada línea)
    private String folioDocumento; // FOLIO_DOCUMENTO
    private String fechaEmision; // FECHA_EMISION

    // Receptor
    private String rutReceptor; // RUT_RECEPTOR
    private String razonSocialReceptor; // RAZON_SOCIAL_RECEPTOR
    private String direccionReceptor; // DIRECCION_RECEPTOR
    private String ciudadReceptor; // CIUDAD_RECEPTOR
    private String comunaReceptor; // COMUNA_RECEPTOR
    private String giroReceptor; // GIRO_RECEPTOR

    // Exportador
    private String rutExportador; // RUT_EXPORTADOR
    private String nombreExportador; // NOMBRE_EXPORTADOR

    // Transporte
    private String rutTransportista; // RUT_TRANSPORTISTA
    private String nombreTransportista; // NOMBRE_TRANSPORTISTA
    private String chofer; // CHOFER
    private String patente; // PATENTE

    // Embarque
    private String numeroContenedor; // NUMERO_CONTENEDOR
    private String sellos; // SELLOS
    private String termografos; // TERMOGRAFOS
    private String horaPresentacion; // HORA_PRESENTACION
    private String codigoPlantaSag; // CODIGO_PLANTA_SAG
    private String codigoNave; // CODIGO_NAVE
    private String nombreNave; // NOMBRE_NAVE
    private String puertoEmbarque; // PUERTO_EMBARQUE
    private String puertoDestino; // PUERTO_DESTINO

    // Tipo de operación
    private String tipoDespacho; // TIPO_DESPACHO
    private String tipoTraslado; // TIPO_TRASLADO

    // Totales (se repiten en cada línea)
    private Float montoNeto; // MONTO_NETO
    private Float montoExento; // MONTO_EXENTO
    private Float iva; // IVA
    private Float totalDocumento; // TOTAL_DOCUMENTO
    private Integer totalCajas; // TOTAL_CAJAS
    private Integer cantidadItems; // CANTIDAD_ITEMS
    private Float pesoNetoTotal; // PESO_NETO_TOTAL
    private Float pesoBrutoTotal; // PESO_BRUTO_TOTAL
    private Integer pallets; // PALLETS

    // Observaciones
    private String observacion; // OBSERVACION
    private String codConsig; // COD_CONSIG
    private String nombreConsignatario; // NOMBRE_CONSIGNATARIO
    private String obsFact; // OBS_FACT
    private String obsFact2; // OBS_FACT2
    private String obsFact3; // OBS_FACT3

    // Getters y setters para todos los campos
    public Integer getLineNumber() {
        return lineNumber;
    }

    public void setLineNumber(Integer lineNumber) {
        this.lineNumber = lineNumber;
    }

    public String getProductCode() {
        return productCode;
    }

    public void setProductCode(String productCode) {
        this.productCode = productCode;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Integer getQuantity() {
        return quantity;
    }

    public void setQuantity(Integer quantity) {
        this.quantity = quantity;
    }

    public Float getUnitPrice() {
        return unitPrice;
    }

    public void setUnitPrice(Float unitPrice) {
        this.unitPrice = unitPrice;
    }

    public Float getLineTotal() {
        return lineTotal;
    }

    public void setLineTotal(Float lineTotal) {
        this.lineTotal = lineTotal;
    }

    // Nuevos getters y setters
    public String getFolioDocumento() {
        return folioDocumento;
    }

    public void setFolioDocumento(String folioDocumento) {
        this.folioDocumento = folioDocumento;
    }

    public String getFechaEmision() {
        return fechaEmision;
    }

    public void setFechaEmision(String fechaEmision) {
        this.fechaEmision = fechaEmision;
    }

    public String getRutReceptor() {
        return rutReceptor;
    }

    public void setRutReceptor(String rutReceptor) {
        this.rutReceptor = rutReceptor;
    }

    public String getRazonSocialReceptor() {
        return razonSocialReceptor;
    }

    public void setRazonSocialReceptor(String razonSocialReceptor) {
        this.razonSocialReceptor = razonSocialReceptor;
    }

    public String getDireccionReceptor() {
        return direccionReceptor;
    }

    public void setDireccionReceptor(String direccionReceptor) {
        this.direccionReceptor = direccionReceptor;
    }

    public String getCiudadReceptor() {
        return ciudadReceptor;
    }

    public void setCiudadReceptor(String ciudadReceptor) {
        this.ciudadReceptor = ciudadReceptor;
    }

    public String getComunaReceptor() {
        return comunaReceptor;
    }

    public void setComunaReceptor(String comunaReceptor) {
        this.comunaReceptor = comunaReceptor;
    }

    public String getGiroReceptor() {
        return giroReceptor;
    }

    public void setGiroReceptor(String giroReceptor) {
        this.giroReceptor = giroReceptor;
    }

    public String getRutExportador() {
        return rutExportador;
    }

    public void setRutExportador(String rutExportador) {
        this.rutExportador = rutExportador;
    }

    public String getNombreExportador() {
        return nombreExportador;
    }

    public void setNombreExportador(String nombreExportador) {
        this.nombreExportador = nombreExportador;
    }

    public String getRutTransportista() {
        return rutTransportista;
    }

    public void setRutTransportista(String rutTransportista) {
        this.rutTransportista = rutTransportista;
    }

    public String getNombreTransportista() {
        return nombreTransportista;
    }

    public void setNombreTransportista(String nombreTransportista) {
        this.nombreTransportista = nombreTransportista;
    }

    public String getChofer() {
        return chofer;
    }

    public void setChofer(String chofer) {
        this.chofer = chofer;
    }

    public String getPatente() {
        return patente;
    }

    public void setPatente(String patente) {
        this.patente = patente;
    }

    public String getNumeroContenedor() {
        return numeroContenedor;
    }

    public void setNumeroContenedor(String numeroContenedor) {
        this.numeroContenedor = numeroContenedor;
    }

    public String getSellos() {
        return sellos;
    }

    public void setSellos(String sellos) {
        this.sellos = sellos;
    }

    public String getTermografos() {
        return termografos;
    }

    public void setTermografos(String termografos) {
        this.termografos = termografos;
    }

    public String getHoraPresentacion() {
        return horaPresentacion;
    }

    public void setHoraPresentacion(String horaPresentacion) {
        this.horaPresentacion = horaPresentacion;
    }

    public String getCodigoPlantaSag() {
        return codigoPlantaSag;
    }

    public void setCodigoPlantaSag(String codigoPlantaSag) {
        this.codigoPlantaSag = codigoPlantaSag;
    }

    public String getCodigoNave() {
        return codigoNave;
    }

    public void setCodigoNave(String codigoNave) {
        this.codigoNave = codigoNave;
    }

    public String getNombreNave() {
        return nombreNave;
    }

    public void setNombreNave(String nombreNave) {
        this.nombreNave = nombreNave;
    }

    public String getPuertoEmbarque() {
        return puertoEmbarque;
    }

    public void setPuertoEmbarque(String puertoEmbarque) {
        this.puertoEmbarque = puertoEmbarque;
    }

    public String getPuertoDestino() {
        return puertoDestino;
    }

    public void setPuertoDestino(String puertoDestino) {
        this.puertoDestino = puertoDestino;
    }

    public String getTipoDespacho() {
        return tipoDespacho;
    }

    public void setTipoDespacho(String tipoDespacho) {
        this.tipoDespacho = tipoDespacho;
    }

    public String getTipoTraslado() {
        return tipoTraslado;
    }

    public void setTipoTraslado(String tipoTraslado) {
        this.tipoTraslado = tipoTraslado;
    }

    public Float getMontoNeto() {
        return montoNeto;
    }

    public void setMontoNeto(Float montoNeto) {
        this.montoNeto = montoNeto;
    }

    public Float getMontoExento() {
        return montoExento;
    }

    public void setMontoExento(Float montoExento) {
        this.montoExento = montoExento;
    }

    public Float getIva() {
        return iva;
    }

    public void setIva(Float iva) {
        this.iva = iva;
    }

    public Float getTotalDocumento() {
        return totalDocumento;
    }

    public void setTotalDocumento(Float totalDocumento) {
        this.totalDocumento = totalDocumento;
    }

    public Integer getTotalCajas() {
        return totalCajas;
    }

    public void setTotalCajas(Integer totalCajas) {
        this.totalCajas = totalCajas;
    }

    public Integer getCantidadItems() {
        return cantidadItems;
    }

    public void setCantidadItems(Integer cantidadItems) {
        this.cantidadItems = cantidadItems;
    }

    public Float getPesoNetoTotal() {
        return pesoNetoTotal;
    }

    public void setPesoNetoTotal(Float pesoNetoTotal) {
        this.pesoNetoTotal = pesoNetoTotal;
    }

    public Float getPesoBrutoTotal() {
        return pesoBrutoTotal;
    }

    public void setPesoBrutoTotal(Float pesoBrutoTotal) {
        this.pesoBrutoTotal = pesoBrutoTotal;
    }

    public Integer getPallets() {
        return pallets;
    }

    public void setPallets(Integer pallets) {
        this.pallets = pallets;
    }

    public String getObservacion() {
        return observacion;
    }

    public void setObservacion(String observacion) {
        this.observacion = observacion;
    }

    public String getCodConsig() {
        return codConsig;
    }

    public void setCodConsig(String codConsig) {
        this.codConsig = codConsig;
    }

    public String getNombreConsignatario() {
        return nombreConsignatario;
    }

    public void setNombreConsignatario(String nombreConsignatario) {
        this.nombreConsignatario = nombreConsignatario;
    }

    public String getObsFact() {
        return obsFact;
    }

    public void setObsFact(String obsFact) {
        this.obsFact = obsFact;
    }

    public String getObsFact2() {
        return obsFact2;
    }

    public void setObsFact2(String obsFact2) {
        this.obsFact2 = obsFact2;
    }

    public String getObsFact3() {
        return obsFact3;
    }

    public void setObsFact3(String obsFact3) {
        this.obsFact3 = obsFact3;
    }
}