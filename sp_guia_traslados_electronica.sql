-- =============================================
-- PROCEDIMIENTO ALMACENADO SIMPLE PARA GUÍA DE TRASLADOS ELECTRÓNICA
-- SYSTEM_ORIGIN: FRUSYSFRPK-TI
-- Basado en el SP original de traslados proporcionado
-- =============================================
CREATE PROCEDURE SP_GUIA_TRASLADOS_ELECTRONICA
    @COD_EMP NVARCHAR(4) = 'MER',
    @NUM_FACT NVARCHAR(20)
AS
BEGIN
    SET NOCOUNT ON;
    
    SELECT 
        -- Información del documento
        T.GUIA_DES AS FOLIO_DOCUMENTO,
        T.FECHA_DESP AS FECHA_EMISION,
        MAX(T.FECHA_PAC) AS FECHA_VENCIMIENTO,
        
        -- Receptor (campos básicos o nulos)
        NULL AS RUT_RECEPTOR,
        NULL AS RAZON_SOCIAL_RECEPTOR,
        NULL AS DIRECCION_RECEPTOR,
        NULL AS CIUDAD_RECEPTOR,
        NULL AS COMUNA_RECEPTOR,
        NULL AS GIRO_RECEPTOR,
        
        -- Exportador (campos básicos o nulos)
        NULL AS RUT_EXPORTADOR,
        NULL AS NOMBRE_EXPORTADOR,
        
        -- Transporte
        NULL AS RUT_TRANSPORTISTA,
        NULL AS NOMBRE_TRANSPORTISTA,
        T.CHOFER + ' RUT: ' + T.RUT_CHOFER AS CHOFER,
        T.PATENTE AS PATENTE,
        
        -- Embarque
        NULL AS NUMERO_CONTENEDOR,
        NULL AS SELLOS,
        NULL AS TERMOGRAFOS,
        NULL AS HORA_PRESENTACION,
        NULL AS CODIGO_PLANTA_SAG,
        NULL AS CODIGO_NAVE,
        NULL AS NOMBRE_NAVE,
        FO.NOM_FRI AS PUERTO_EMBARQUE,
        FD.NOM_FRI AS PUERTO_DESTINO,
        
        -- Tipo de operación (específico para traslados)
        '1 NO ESPECIFICADO' AS TIPO_DESPACHO,
        '5 TRASLADOS INTERNOS' AS TIPO_TRASLADO,
        
        -- Detalle de items
        ROW_NUMBER() OVER (ORDER BY T.COD_ESP, T.COD_ENVOP) AS NRO_LINEA,
        T.COD_ENVOP AS CODIGO_PRODUCTO,
        'CAJAS ' + E.NOM_ESP + ' ' + T.COD_ENVOP AS ITEM_DESCRIPCION,
        SUM(T.CAJAS) AS CANTIDAD,
        ROUND(ISNULL((
            SELECT ISNULL(AVG(PRECIO * TIPO_CAMBIO), 0) 
            FROM PRECIOS_TRASLADOS 
            WHERE COD_EMP = T.COD_EMP 
                AND COD_TEM = T.COD_TEM 
                AND ZON = T.ZON 
                AND GUIA_DESP = T.GUIA_DESP 
                AND COD_ESP = T.COD_ESP 
                AND COD_ENVOP = T.COD_ENVOP
        ), 0), 0) AS PRECIO_UNITARIO,
        ROUND(ISNULL((
            SELECT ISNULL(AVG(PRECIO * TIPO_CAMBIO), 0) 
            FROM PRECIOS_TRASLADOS 
            WHERE COD_EMP = T.COD_EMP 
                AND COD_TEM = T.COD_TEM 
                AND ZON = T.ZON 
                AND GUIA_DESP = T.GUIA_DESP 
                AND COD_ESP = T.COD_ESP 
                AND COD_ENVOP = T.COD_ENVOP
        ), 0), 0) * SUM(T.CAJAS) AS TOTAL_LINEA,
        
        -- Totales básicos calculados como TOTALES GENERALES (igual en cada fila)
        0 AS MONTO_NETO,
        0 AS MONTO_EXENTO,
        0 AS IVA,
        0 AS TOTAL_DOCUMENTO,
        -- TOTAL_CAJAS: SIEMPRE el gran total de la guía completa
        (SELECT SUM(T_ALL.CAJAS) 
         FROM TRASLADOS T_ALL 
         WHERE T_ALL.COD_EMP = @COD_EMP AND T_ALL.GUIA_DES = @NUM_FACT AND T_ALL.COD_TEM = 8 AND T_ALL.SW_ELEC = 1) AS TOTAL_CAJAS,
        -- CANTIDAD_ITEMS: Total de líneas diferentes de productos
        (SELECT COUNT(DISTINCT T_ALL.COD_ENVOP) 
         FROM TRASLADOS T_ALL 
         WHERE T_ALL.COD_EMP = @COD_EMP AND T_ALL.GUIA_DES = @NUM_FACT AND T_ALL.COD_TEM = 8 AND T_ALL.SW_ELEC = 1) AS CANTIDAD_ITEMS,
        -- PESO_NETO_TOTAL: SIEMPRE el gran total de toda la guía
        (SELECT SUM(EO_ALL.PESO_NETO * T_ALL.CAJAS) 
         FROM TRASLADOS T_ALL 
         INNER JOIN ENVASEOPERACIONAL EO_ALL ON T_ALL.COD_TEM = EO_ALL.COD_TEM AND T_ALL.COD_EMP = EO_ALL.COD_EMP 
                                              AND T_ALL.COD_ESP = EO_ALL.COD_ESP AND T_ALL.COD_ENV = EO_ALL.COD_ENV 
                                              AND T_ALL.COD_EMB = EO_ALL.COD_EMB
         WHERE T_ALL.COD_EMP = @COD_EMP AND T_ALL.GUIA_DES = @NUM_FACT AND T_ALL.COD_TEM = 8 AND T_ALL.SW_ELEC = 1) AS PESO_NETO_TOTAL,
        -- PESO_BRUTO_TOTAL: SIEMPRE el gran total de toda la guía
        (SELECT SUM(EO_ALL.PESO_BRUTO * T_ALL.CAJAS) 
         FROM TRASLADOS T_ALL 
         INNER JOIN ENVASEOPERACIONAL EO_ALL ON T_ALL.COD_TEM = EO_ALL.COD_TEM AND T_ALL.COD_EMP = EO_ALL.COD_EMP 
                                              AND T_ALL.COD_ESP = EO_ALL.COD_ESP AND T_ALL.COD_ENV = EO_ALL.COD_ENV 
                                              AND T_ALL.COD_EMB = EO_ALL.COD_EMB
         WHERE T_ALL.COD_EMP = @COD_EMP AND T_ALL.GUIA_DES = @NUM_FACT AND T_ALL.COD_TEM = 8 AND T_ALL.SW_ELEC = 1) AS PESO_BRUTO_TOTAL,
        -- PALLETS: Total de registros (pallets)
        (SELECT COUNT(*) 
         FROM TRASLADOS T_ALL 
         WHERE T_ALL.COD_EMP = @COD_EMP AND T_ALL.GUIA_DES = @NUM_FACT AND T_ALL.SW_ELEC = 1) AS PALLETS,
        
        -- Observaciones
        T.OBSERVACION AS OBSERVACION,
        NULL AS COD_CONSIG,
        NULL AS NOMBRE_CONSIGNATARIO,
        
        -- Observaciones formateadas
        'Observacion: ' + ISNULL(T.OBSERVACION, '') AS OBS_FACT,
        'Frigorifico Origen: ' + ISNULL(FO.NOM_FRI, '') + ', Frigorifico Destino: ' + ISNULL(FD.NOM_FRI, '') AS OBS_FACT2,
        'Traslado interno entre frigoríficos' AS OBS_FACT3
        
    FROM TRASLADOS T
    INNER JOIN ESPECIE E ON T.COD_ESP = E.COD_ESP 
        AND T.COD_TEM = E.COD_TEM 
        AND T.COD_EMP = E.COD_EMP
    INNER JOIN ENVASEOPERACIONAL EO ON T.COD_TEM = EO.COD_TEM 
        AND T.COD_EMP = EO.COD_EMP 
        AND T.COD_ESP = EO.COD_ESP 
        AND T.COD_ENV = EO.COD_ENV 
        AND T.COD_EMB = EO.COD_EMB
    LEFT JOIN FRIOS FO ON T.COD_EMP = FO.COD_EMP 
        AND T.COD_TEM = FO.COD_TEM 
        AND T.COD_FRI = FO.COD_FRI
    LEFT JOIN FRIOS FD ON T.COD_EMP = FD.COD_EMP 
        AND T.COD_TEM = FD.COD_TEM 
        AND T.COD_FRI_DEST = FD.COD_FRI
    WHERE T.COD_EMP = @COD_EMP 
        AND T.GUIA_DES = @NUM_FACT
        AND T.COD_TEM = 8
        AND T.SW_ELEC = 1
    GROUP BY 
        T.COD_EMP, T.COD_TEM, T.ZON, T.GUIA_DESP, T.COD_ESP, T.COD_ENVOP,
        T.GUIA_DES, T.FECHA_DESP, T.CHOFER, T.RUT_CHOFER, T.PATENTE,
        T.OBSERVACION, E.NOM_ESP, EO.PESO_NETO, EO.PESO_BRUTO,
        FO.NOM_FRI, FD.NOM_FRI, T.COD_EMB  -- Agregamos COD_EMB al GROUP BY
        
END
GO

-- =============================================
-- EJEMPLOS DE USO:
-- =============================================
/*
EXEC SP_GUIA_TRASLADOS_ELECTRONICA @NUM_FACT = '74518'
EXEC SP_GUIA_TRASLADOS_ELECTRONICA @COD_EMP = 'MER', @NUM_FACT = '74516'
*/