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

    public List<DispatchDetailLine> fetchDetail(String codEmp, String numFact) {
        String sql = "EXEC SP_GUIA_DESPACHO_ELECTRONICA @COD_EMP = ?, @NUM_FACT = ?";
        return jdbcTemplate.query(con -> {
            var ps = con.prepareStatement(sql);
            ps.setString(1, codEmp);
            ps.setString(2, numFact);
            return ps;
        }, new DispatchDetailRowMapper());
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