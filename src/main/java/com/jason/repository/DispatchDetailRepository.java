package com.jason.repository;

import com.jason.model.DispatchDetailLine;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

@Repository
public class DispatchDetailRepository {

    private final JdbcTemplate jdbcTemplate;

    public DispatchDetailRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public List<DispatchDetailLine> fetchDetail(String codEmp, String numFact, String sysOrigen) {
        System.out.println("=== INICIANDO fetchDetail para " + codEmp + "/" + numFact + " ===");
        
        // Diagnóstico simple sin SP adicionales
        System.out.println("Procesando guía " + codEmp + "/" + numFact + " con sys_origen: " + sysOrigen);
        
        // Normalizar sysOrigen: mayúsculas y quitar guiones para detección
    String originNorm = sysOrigen != null ? sysOrigen.trim().toUpperCase() : "";
    // Remover caracteres no alfanuméricos para determinar sufijo
    String originClean = originNorm.replaceAll("[^A-Z0-9]", "");
        String spName;
        // Determinar SP basándose en sufijo FP (dispatch) o TI (translados) o CC (envases)
        if (originClean.endsWith("FP")) {
            spName = "SP_GUIA_DESPACHO_ELECTRONICA";
            System.out.println("Usando SP de despachos para sys_origen: " + originNorm);
        } else if (originClean.endsWith("TI")) {
            spName = "SP_GUIA_TRASLADOS_ELECTRONICA";
            System.out.println("Usando SP de traslados para sys_origen: " + originNorm);
        } else if (originClean.endsWith("CC")) {
            spName = "SP_GUIA_DESPACHO_CC_ENVASES";
            System.out.println("Usando SP de envases para sys_origen: " + originNorm);
        } else {
            spName = "SP_GUIA_DESPACHO_ELECTRONICA"; // predeterminado
            System.out.println("sys_origen desconocido ('" + sysOrigen + "'), usando SP_GUIA_DESPACHO_ELECTRONICA");
        }
        
        System.out.println("Ejecutando " + spName + "...");
        String sql = "EXEC " + spName + " @COD_EMP = ?, @NUM_FACT = ?";
        List<DispatchDetailLine> result = jdbcTemplate.query(con -> {
            var ps = con.prepareStatement(sql);
            ps.setString(1, codEmp);
            ps.setString(2, numFact);
            return ps;
        }, new DispatchDetailRowMapper());
        
        System.out.println("SP returned " + result.size() + " records for " + codEmp + "/" + numFact);
        if (result.isEmpty()) {
            System.out.println("¡RESULTADO VACÍO! El SP no devolvió datos con " + spName + ".");
            // Fallback: si era FP o CC y no obtuvo detalle, intentar con despacho electrónica
            if ((originClean.endsWith("FP") || originClean.endsWith("CC")) && !"SP_GUIA_DESPACHO_ELECTRONICA".equals(spName)) {
                System.out.println("Fallback: intentando SP_GUIA_DESPACHO_ELECTRONICA para sys_origen: " + originNorm);
                result = jdbcTemplate.query(con -> {
                    var ps2 = con.prepareStatement("EXEC SP_GUIA_DESPACHO_ELECTRONICA @COD_EMP = ?, @NUM_FACT = ?");
                    ps2.setString(1, codEmp);
                    ps2.setString(2, numFact);
                    return ps2;
                }, new DispatchDetailRowMapper());
                System.out.println("Fallback SP_GUIA_DESPACHO_ELECTRONICA returned " + result.size() + " registros.");
            }
        } else {
            for (int i = 0; i < Math.min(3, result.size()); i++) {
                DispatchDetailLine line = result.get(i);
                System.out.println("Línea " + (i+1) + ": " + line.getDescription() + " - Cantidad: " + line.getQuantity());
            }
        }
        
        return result;
    }

    private static class DispatchDetailRowMapper implements RowMapper<DispatchDetailLine> {
        @Override
        public DispatchDetailLine mapRow(ResultSet rs, int rowNum) throws SQLException {
            DispatchDetailLine line = new DispatchDetailLine();

            // Mapeo del detalle
            line.setLineNumber(safeInt(rs, "NRO_LINEA"));
            line.setProductCode(rs.getString("CODIGO_PRODUCTO"));
            line.setDescription(rs.getString("ITEM_DESCRIPCION"));
            line.setQuantity(safeInt(rs, "CANTIDAD"));
            line.setUnitPrice(safeFloat(rs, "PRECIO_UNITARIO"));
            line.setLineTotal(safeFloat(rs, "TOTAL_LINEA"));

            // Mapeo del encabezado
            line.setFolioDocumento(rs.getString("FOLIO_DOCUMENTO"));
            line.setFechaEmision(rs.getString("FECHA_EMISION"));

            // Receptor
            line.setRutReceptor(rs.getString("RUT_RECEPTOR"));
            line.setRazonSocialReceptor(rs.getString("RAZON_SOCIAL_RECEPTOR"));
            line.setDireccionReceptor(rs.getString("DIRECCION_RECEPTOR"));
            line.setCiudadReceptor(rs.getString("CIUDAD_RECEPTOR"));
            line.setComunaReceptor(rs.getString("COMUNA_RECEPTOR"));
            line.setGiroReceptor(rs.getString("GIRO_RECEPTOR"));

            // Exportador
            line.setRutExportador(rs.getString("RUT_EXPORTADOR"));
            line.setNombreExportador(rs.getString("NOMBRE_EXPORTADOR"));

            // Transporte
            line.setRutTransportista(rs.getString("RUT_TRANSPORTISTA"));
            line.setNombreTransportista(rs.getString("NOMBRE_TRANSPORTISTA"));
            line.setChofer(rs.getString("CHOFER"));
            line.setPatente(rs.getString("PATENTE"));

            // Embarque
            line.setNumeroContenedor(rs.getString("NUMERO_CONTENEDOR"));
            line.setSellos(rs.getString("SELLOS"));
            line.setTermografos(rs.getString("TERMOGRAFOS"));
            line.setHoraPresentacion(rs.getString("HORA_PRESENTACION"));
            line.setCodigoPlantaSag(rs.getString("CODIGO_PLANTA_SAG"));
            line.setCodigoNave(rs.getString("CODIGO_NAVE"));
            line.setNombreNave(rs.getString("NOMBRE_NAVE"));
            line.setPuertoEmbarque(rs.getString("PUERTO_EMBARQUE"));
            line.setPuertoDestino(rs.getString("PUERTO_DESTINO"));

            // Tipo de operación
            line.setTipoDespacho(rs.getString("TIPO_DESPACHO"));
            line.setTipoTraslado(rs.getString("TIPO_TRASLADO"));

            // Totales
            line.setMontoNeto(safeFloat(rs, "MONTO_NETO"));
            line.setMontoExento(safeFloat(rs, "MONTO_EXENTO"));
            line.setIva(safeFloat(rs, "IVA"));
            line.setTotalDocumento(safeFloat(rs, "TOTAL_DOCUMENTO"));
            line.setTotalCajas(safeInt(rs, "TOTAL_CAJAS"));
            line.setCantidadItems(safeInt(rs, "CANTIDAD_ITEMS"));
            line.setPesoNetoTotal(safeFloat(rs, "PESO_NETO_TOTAL"));
            line.setPesoBrutoTotal(safeFloat(rs, "PESO_BRUTO_TOTAL"));
            line.setPallets(safeInt(rs, "PALLETS"));
            // total bultos derivado (cajas+pallets) podría calcularse afuera si se requiere

            // Observaciones
            line.setObservacion(rs.getString("OBSERVACION"));
            line.setCodConsig(rs.getString("COD_CONSIG"));
            line.setNombreConsignatario(rs.getString("NOMBRE_CONSIGNATARIO"));
            line.setObsFact(rs.getString("OBS_FACT"));
            line.setObsFact2(rs.getString("OBS_FACT2"));
            line.setObsFact3(rs.getString("OBS_FACT3"));

            return line;
        }

    private Integer safeInt(ResultSet rs, String col) {
            try {
                int v = rs.getInt(col);
                return rs.wasNull() ? null : v;
            } catch (SQLException e) {
                return null;
            }
        }

    private Float safeFloat(ResultSet rs, String col) {
            try {
                float v = rs.getFloat(col);
                return rs.wasNull() ? null : v;
            } catch (SQLException e) {
                return null;
            }
        }
    }
}