package com.jason.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "FE_DTE_ENV_WS")
public class FeDteEnvWs {

    @EmbeddedId
    private FeDteEnvWsId id;

    @Column(name = "RutEmpresa", nullable = false, length = 8)
    private String rutEmpresa;

    @Column(name = "DvEmpresa", nullable = false, length = 1)
    private String dvEmpresa;

    @Column(name = "FechaEmision")
    private LocalDateTime fechaEmision;

    @Column(name = "FechaVenc")
    private LocalDateTime fechaVenc;

    @Column(name = "RutCliente", length = 10)
    private String rutCliente;

    @Column(name = "RazonSocialCliente", length = 100)
    private String razonSocialCliente;

    @Column(name = "GiroCliente", length = 100)
    private String giroCliente;

    @Column(name = "DireccionCliente", length = 400)
    private String direccionCliente;

    @Column(name = "ComunaCliente", length = 100)
    private String comunaCliente;

    @Column(name = "CiudadCliente", length = 100)
    private String ciudadCliente;

    @Column(name = "NETO")
    private Float neto;

    @Column(name = "EXEN")
    private Float exen;

    @Column(name = "IVAP")
    private Float ivap;

    @Column(name = "IVAM")
    private Float ivam;

    @Column(name = "TOTAL")
    private Float total;

    @Column(name = "TDES")
    private Float tdes;

    @Column(name = "TRAS")
    private Float tras;

    @Lob
    @Column(name = "TIMB")
    private byte[] timb;

    @Column(name = "sTIMB", columnDefinition = "NVARCHAR(4000)")
    private String sTimb;

    @Column(name = "npdf", length = 50)
    private String npdf;

    @Column(name = "eenv")
    private Boolean eenv;

    
    @Column(name = "sys_origen", length = 50)
    private String sysOrigen;

    // Getters y Setters
    public FeDteEnvWsId getId() {
        return id;
    }

    public void setId(FeDteEnvWsId id) {
        this.id = id;
    }

    public String getRutEmpresa() {
        return rutEmpresa;
    }

    public void setRutEmpresa(String rutEmpresa) {
        this.rutEmpresa = rutEmpresa;
    }

    public String getDvEmpresa() {
        return dvEmpresa;
    }

    public void setDvEmpresa(String dvEmpresa) {
        this.dvEmpresa = dvEmpresa;
    }

    public LocalDateTime getFechaEmision() {
        return fechaEmision;
    }

    public void setFechaEmision(LocalDateTime fechaEmision) {
        this.fechaEmision = fechaEmision;
    }

    public LocalDateTime getFechaVenc() {
        return fechaVenc;
    }

    public void setFechaVenc(LocalDateTime fechaVenc) {
        this.fechaVenc = fechaVenc;
    }

    public String getRutCliente() {
        return rutCliente;
    }

    public void setRutCliente(String rutCliente) {
        this.rutCliente = rutCliente;
    }

    public String getRazonSocialCliente() {
        return razonSocialCliente;
    }

    public void setRazonSocialCliente(String razonSocialCliente) {
        this.razonSocialCliente = razonSocialCliente;
    }

    public String getGiroCliente() {
        return giroCliente;
    }

    public void setGiroCliente(String giroCliente) {
        this.giroCliente = giroCliente;
    }

    public String getDireccionCliente() {
        return direccionCliente;
    }

    public void setDireccionCliente(String direccionCliente) {
        this.direccionCliente = direccionCliente;
    }

    public String getComunaCliente() {
        return comunaCliente;
    }

    public void setComunaCliente(String comunaCliente) {
        this.comunaCliente = comunaCliente;
    }

    public String getCiudadCliente() {
        return ciudadCliente;
    }

    public void setCiudadCliente(String ciudadCliente) {
        this.ciudadCliente = ciudadCliente;
    }

    public Float getNeto() {
        return neto;
    }

    public void setNeto(Float neto) {
        this.neto = neto;
    }

    public Float getExen() {
        return exen;
    }

    public void setExen(Float exen) {
        this.exen = exen;
    }

    public Float getIvap() {
        return ivap;
    }

    public void setIvap(Float ivap) {
        this.ivap = ivap;
    }

    public Float getIvam() {
        return ivam;
    }

    public void setIvam(Float ivam) {
        this.ivam = ivam;
    }

    public Float getTotal() {
        return total;
    }

    public void setTotal(Float total) {
        this.total = total;
    }

    public Float getTdes() {
        return tdes;
    }

    public void setTdes(Float tdes) {
        this.tdes = tdes;
    }

    public Float getTras() {
        return tras;
    }

    public void setTras(Float tras) {
        this.tras = tras;
    }

    public byte[] getTimb() {
        return timb;
    }

    public void setTimb(byte[] timb) {
        this.timb = timb;
    }

    public String getSTimb() {
        return sTimb;
    }

    public void setSTimb(String sTimb) {
        this.sTimb = sTimb;
    }

    public String getNpdf() {
        return npdf;
    }

    public void setNpdf(String npdf) {
        this.npdf = npdf;
    }

    public Boolean getEenv() {
        return eenv;
    }

    public void setEenv(Boolean eenv) {
        this.eenv = eenv;
    }

    public String getSysOrigen() {
        return sysOrigen;
    }

    public void setSysOrigen(String sysOrigen) {
        this.sysOrigen = sysOrigen;
    }
}