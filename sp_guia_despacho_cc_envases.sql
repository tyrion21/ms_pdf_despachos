-- =============================================
-- PROCEDIMIENTO ALMACENADO PARA GUIA DE DESPACHO CC ENVASES
-- SYSTEM_ORIGIN: FRUSYSFRPK-CC
-- Basado en el codigo de referencia proporcionado para el proceso 3 (envases)
-- =============================================
CREATE PROCEDURE SP_GUIA_DESPACHO_CC_ENVASES
    @COD_EMP NVARCHAR(4) = 'MER',
    @NUM_FACT NVARCHAR(20)
AS
BEGIN
    SET NOCOUNT ON;
    
    WITH CTE_PARAMETROS AS (
        -- Parámetros base dinámicos
        SELECT 
            @COD_EMP AS COD_EMP,
            @NUM_FACT AS NUM_FACT,
            8 AS COD_TEM, -- COD_TEM fijo en 8
            -- Obtener PLANILLA automáticamente basado en NRO_GUIA
            (SELECT TOP (1) CORRELATIVO_PLAN 
             FROM TIT_CTACTE_ENVASES 
             WHERE NRO_GUIA = @NUM_FACT
               AND COD_EMP = @COD_EMP
               AND SW_ELEC = 1) AS PLANILLA
    ),

    
    SELECT 
        -- Información del documento
        T.NRO_GUIA AS FOLIO_DOCUMENTO,
        T.FECHA AS FECHA_EMISION,
        MAX(T.FECHA) AS FECHA_VENCIMIENTO,
        
        -- Receptor (información de centro de costos)
        NULL AS RUT_RECEPTOR,
        'CENTRO DE COSTOS - ENVASES' AS RAZON_SOCIAL_RECEPTOR,
        NULL AS DIRECCION_RECEPTOR,
        NULL AS CIUDAD_RECEPTOR,
        NULL AS COMUNA_RECEPTOR,
        NULL AS GIRO_RECEPTOR,
        
        -- Exportador
        NULL AS RUT_EXPORTADOR,
        NULL AS NOMBRE_EXPORTADOR,
        
        -- Transportista
        LTRIM(ISNULL(T.RUT_CHOFER,'')) + CASE WHEN LTRIM(ISNULL(T.DV,'')) = '' THEN '' ELSE '-' + LTRIM(ISNULL(T.DV,'')) END AS RUT_TRANSPORTISTA,
        NULL AS NOMBRE_TRANSPORTISTA,
        T.NOMBRE_CHOFER AS CHOFER,
        T.PATENTE,
        
        -- Embarque - campos básicos
        NULL AS NUMERO_CONTENEDOR,
        NULL AS SELLOS,
        NULL AS TERMOGRAFOS,
        NULL AS HORA_PRESENTACION,
        NULL AS CODIGO_PLANTA_SAG,
        NULL AS CODIGO_NAVE,
        NULL AS NOMBRE_NAVE,
        NULL AS PUERTO_EMBARQUE,
        NULL AS PUERTO_DESTINO,
        
        -- Tipo de operación específico para envases
        '3 DESPACHO POR CUENTA DEL EMISOR A OTRAS INSTALACIONES' AS TIPO_DESPACHO,
        '5 TRASLADOS INTERNOS' AS TIPO_TRASLADO,
        
        -- Detalle de items
        ROW_NUMBER() OVER (ORDER BY ENVASE.COD_ENV) AS NRO_LINEA,
        ENVASE.COD_ENV AS CODIGO_PRODUCTO,
        ENVASE.NOM_ENV + (CASE ISNULL(D.LOTE,'') WHEN '' THEN '' ELSE '     LOTE Nº' + ISNULL(D.LOTE,'') END) AS ITEM_DESCRIPCION,
        D.CANTIDAD,
        ENVASE.PRECIO_TRASLADO AS PRECIO_UNITARIO,
        (D.CANTIDAD * ENVASE.PRECIO_TRASLADO) AS TOTAL_LINEA,
        
        -- Totales del documento
        0 AS MONTO_NETO,
        0 AS MONTO_EXENTO,
        (D.CANTIDAD * ENVASE.PRECIO_TRASLADO) * 0.19 AS IVA,
        (D.CANTIDAD * ENVASE.PRECIO_TRASLADO) * 1.19 AS TOTAL_DOCUMENTO,
        SUM(D.CANTIDAD) AS TOTAL_CAJAS,
        COUNT(DISTINCT ENVASE.COD_ENV) AS CANTIDAD_ITEMS,
        0 AS PESO_NETO_TOTAL,
        0 AS PESO_BRUTO_TOTAL,
        COUNT(*) AS PALLETS,
        
        -- Observaciones
        ISNULL(T.OBSERVACION, '') AS OBSERVACION,
        NULL AS COD_CONSIG,
        NULL AS NOMBRE_CONSIGNATARIO,
        
        -- Observaciones formateadas para el documento (CC/Envases)
        'Centro de Costos - Envases: ' + ISNULL(T.OBSERVACION, '') AS OBS_FACT,
        'Chofer: ' + ISNULL(T.NOMBRE_CHOFER, '') + ', Patente: ' + ISNULL(T.PATENTE, '') AS OBS_FACT2,
        'Tipo Operación: Traslados Internos' AS OBS_FACT3
        
    FROM TIT_CTACTE_ENVASES T
    INNER JOIN DETALLE_CTACTE_ENVASES D ON T.COD_EMP = D.COD_EMP 
                                        AND T.COD_TEM = D.COD_TEM 
                                        AND T.ZON = D.ZON 
                                        AND T.NPLANILLA = D.NPLANILLA 
                                        AND T.TIPO_MOV = D.TIPO_MOV
    INNER JOIN ENVASE ON D.COD_ENV = ENVASE.COD_ENV 
                     AND D.COD_TEM = ENVASE.COD_TEM 
                     AND D.COD_EMP = ENVASE.COD_EMP
    CROSS JOIN CTE_PARAMETROS P
    WHERE T.COD_EMP = P.COD_EMP
      AND T.SW_ELEC = 1 
      AND T.CORRELATIVO_PLAN = P.PLANILLA
      AND T.COD_TEM = P.COD_TEM           
    GROUP BY 
        T.NRO_GUIA, T.FECHA, T.OBSERVACION, T.NOMBRE_CHOFER, T.PATENTE, 
        T.RUT_CHOFER, T.DV, ENVASE.COD_ENV, ENVASE.NOM_ENV, D.LOTE, 
        D.CANTIDAD, ENVASE.PRECIO_TRASLADO

END
GO

-- =============================================
-- EJEMPLOS DE USO DEL PROCEDIMIENTO:
-- =============================================

/*
-- Ejemplo 1: Ejecutar para guía específica con empresa por defecto (MER)
EXEC SP_GUIA_DESPACHO_CC_ENVASES @NUM_FACT = '74585'

-- Ejemplo 2: Ejecutar con empresa específica
EXEC SP_GUIA_DESPACHO_CC_ENVASES @COD_EMP = 'MER', @NUM_FACT = '74585'

-- Ejemplo 3: Probar otra guía
EXEC SP_GUIA_DESPACHO_CC_ENVASES @COD_EMP = 'MER', @NUM_FACT = '75001'
*/
