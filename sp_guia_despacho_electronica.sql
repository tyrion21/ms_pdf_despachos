-- =============================================
-- PROCEDIMIENTO ALMACENADO PARA GUÍA DE DESPACHO ELECTRÓNICA CON PARÁMETROS DINÁMICOS
-- =============================================
CREATE PROCEDURE SP_GUIA_DESPACHO_ELECTRONICA
    @COD_EMP NVARCHAR(4) = 'MER',
    @NUM_FACT NVARCHAR(20)  -- Cambiado de INT a NVARCHAR para soportar valores como '2004-4'
AS
BEGIN
    SET NOCOUNT ON;
    
    WITH CTE_PARAMETROS AS (
        -- Parámetros base dinámicos
        SELECT 
            @COD_EMP AS COD_EMP,
            @NUM_FACT AS NUM_FACT,
            8 AS COD_TEM, -- COD_TEM fijo en 8
            -- Obtener PLANILLA automáticamente basado en GUIA_DES
            (SELECT TOP (1) CORRELATIVO_PLAN 
             FROM DESPACHOS 
             WHERE GUIA_DES = @NUM_FACT  -- Ya no necesita conversión
               AND COD_EMP = @COD_EMP
               AND SW_ELEC = 1) AS PLANILLA
    ),

    CTE_TERMOGRAFOS AS (
        -- Obtener los termógrafos concatenados
        SELECT 
            P.COD_EMP,
            P.NUM_FACT,
            STRING_AGG(T.NRO_TER, ', ') AS TERMOGRAFOS
        FROM DESPACHOS T
        CROSS JOIN CTE_PARAMETROS P
        WHERE T.COD_EMP = P.COD_EMP 
            AND T.COD_TEM = 8  -- COD_TEM fijo en 8
            AND T.GUIA_DES = P.NUM_FACT  -- Ya no necesita conversión
            AND T.SW_ELEC = 1 
            AND ISNULL(T.NRO_TER,'') <> ''
        GROUP BY P.COD_EMP, P.NUM_FACT
    ),

    CTE_DESPACHOS_UNION AS (
        -- Unión de DESPACHOS y MIXDESPACHOS
        SELECT 
            T.COD_EMP, T.COD_TEM, T.ZON, T.GUIA_DESP, T.COD_ENV, T.COD_ENVOP, 
            T.COD_ESP, T.COD_REC, T.NRO_CONT, T.TIP_TRA, T.COD_SIT, T.CAJAS, 
            T.COD_EMB, T.COD_TRP, T.COD_EXP, T.NRO_EMB, T.SW_ELEC, T.CORRELATIVO_PLAN,
            T.GUIA_DES, T.CHOFER, T.RUT_CHOFER, T.PATENTE, T.OBSERVACION, T.FECHA_DESP,
            T.HORA_PRE, T.SELLOS, T.COD_FRI
        FROM DESPACHOS T
        CROSS JOIN CTE_PARAMETROS P
        WHERE T.COD_EMP = P.COD_EMP 
            AND T.COD_TEM = 8  -- COD_TEM fijo en 8
            AND T.SW_ELEC = 1 
            AND T.CORRELATIVO_PLAN = P.PLANILLA 
            AND T.NRO_MIX = 0
        
        UNION ALL
        
        SELECT 
            T.COD_EMP, T.COD_TEM, T.ZON, T.GUIA_DESP, T.COD_ENV, T.COD_ENVOP, 
            T.COD_ESP, T.COD_REC, T.NRO_CONT, T.TIP_TRA, T.COD_SIT, T.CAJAS, 
            T.COD_EMB, T.COD_TRP, T.COD_EXP, T.NRO_EMB, 1 AS SW_ELEC, T.CORRELATIVO_PLAN,
            T.GUIA_DES, T.CHOFER, T.RUT_CHOFER, T.PATENTE, T.OBSERVACION, T.FECHA_DESP,
            NULL AS HORA_PRE, NULL AS SELLOS, NULL AS COD_FRI
        FROM MIXDESPACHOS T
        CROSS JOIN CTE_PARAMETROS P
        WHERE T.COD_EMP = P.COD_EMP 
            AND T.COD_TEM = 8  -- COD_TEM fijo en 8
            AND STR(T.COD_TEM) + T.ZON + CONVERT(VARCHAR(15), T.GUIA_DESP) IN (
                SELECT STR(COD_TEM) + ZON + CONVERT(VARCHAR(15), GUIA_DESP)
                FROM DESPACHOS 
                WHERE COD_EMP = P.COD_EMP 
                    AND COD_TEM = 8  -- COD_TEM fijo en 8
                    AND SW_ELEC = 1 
                    AND CORRELATIVO_PLAN = P.PLANILLA
            )
    ),

    CTE_DETALLE AS (
        -- Generar el detalle de items agrupados
        SELECT 
            -- Descripción del item
            TRIM(ISNULL(ENV.NOM_ENV, '')) + ' ' + E.NOM_ESP + ' ' + T.COD_ENVOP AS ITEM_DESCRIPCION,
            
            -- Código del producto
            T.COD_ENVOP AS CODIGO_PRODUCTO,
            
            -- Cantidad
            SUM(T.CAJAS) AS CANTIDAD,
            
            -- Precio unitario
            ROUND(ISNULL((
                SELECT ISNULL(AVG(PRECIO * TIPO_CAMBIO), 0) 
                FROM PRECIOS_DESPACHO 
                WHERE COD_EMP = T.COD_EMP 
                    AND COD_TEM = T.COD_TEM 
                    AND ZON = T.ZON 
                    AND PLANILLA = T.GUIA_DESP 
                    AND COD_ESP = T.COD_ESP 
                    AND COD_ENVOP = T.COD_ENVOP
            ), 0), 0) AS PRECIO_UNITARIO,
            
            -- Total por línea
            ROUND(ISNULL((
                SELECT ISNULL(AVG(PRECIO * TIPO_CAMBIO), 0) 
                FROM PRECIOS_DESPACHO 
                WHERE COD_EMP = T.COD_EMP 
                    AND COD_TEM = T.COD_TEM 
                    AND ZON = T.ZON 
                    AND PLANILLA = T.GUIA_DESP 
                    AND COD_ESP = T.COD_ESP 
                    AND COD_ENVOP = T.COD_ENVOP
            ), 0), 0) * SUM(T.CAJAS) AS TOTAL_LINEA,
            
            -- Información adicional para totales
            EO.PESO_NETO * SUM(T.CAJAS) AS PESO_NETO_TOTAL,
            EO.PESO_BRUTO * SUM(T.CAJAS) AS PESO_BRUTO_TOTAL,
            
            -- Orden para el detalle
            ROW_NUMBER() OVER (ORDER BY T.COD_ESP, T.COD_ENVOP) AS NRO_LINEA
            
        FROM CTE_DESPACHOS_UNION T
        INNER JOIN ESPECIE E ON T.COD_ESP = E.COD_ESP 
            AND T.COD_TEM = E.COD_TEM 
            AND T.COD_EMP = E.COD_EMP
        INNER JOIN ENVASEOPERACIONAL EO ON T.COD_TEM = EO.COD_TEM 
            AND T.COD_EMP = EO.COD_EMP 
            AND T.COD_ESP = EO.COD_ESP 
            AND T.COD_ENV = EO.COD_ENV 
            AND T.COD_EMB = EO.COD_EMB
        LEFT JOIN ENVASE ENV ON ENV.COD_ENV = T.COD_ENV 
            AND T.COD_EMP = ENV.COD_EMP 
            AND T.COD_TEM = ENV.COD_TEM
        WHERE T.COD_TEM = 8  -- COD_TEM fijo en 8 adicional en el detalle
        GROUP BY 
            T.COD_EMP, T.COD_TEM, T.ZON, T.GUIA_DESP, T.COD_ESP, T.COD_ENVOP,
            E.NOM_ESP, ENV.NOM_ENV, EO.PESO_NETO, EO.PESO_BRUTO
    ),

    CTE_ENCABEZADO AS (
        -- Información del encabezado - consolidada para evitar duplicados
        SELECT 
            MAX(D.GUIA_DES) AS FOLIO_DOCUMENTO,
            MAX(D.FECHA_DESP) AS FECHA_EMISION,
            
            -- Receptor (Cliente)
            MAX(R.RUT_REC) AS RUT_RECEPTOR,
            MAX(R.NOM_REC) AS RAZON_SOCIAL_RECEPTOR,
            MAX(R.DIR_REC) AS DIRECCION_RECEPTOR,
            MAX(R.CIU_REC) AS CIUDAD_RECEPTOR,
            MAX(R.COD_COM) AS COMUNA_RECEPTOR,
            MAX(R.GIRO) AS GIRO_RECEPTOR,
            
            -- Exportador
            MAX(EX.RUT_EXP) AS RUT_EXPORTADOR,
            MAX(EX.NOM_EXP) AS NOMBRE_EXPORTADOR,
            
            -- Transportista - MANEJO SEGURO DEL RUT
            MAX(CASE 
                WHEN TR.RUT_TRP IS NULL THEN ''
                WHEN LTRIM(RTRIM(CONVERT(VARCHAR(20), TR.RUT_TRP))) LIKE '%-%' 
                    THEN LTRIM(RTRIM(CONVERT(VARCHAR(20), TR.RUT_TRP)))
                WHEN ISNUMERIC(LTRIM(RTRIM(CONVERT(VARCHAR(20), TR.RUT_TRP)))) = 1 
                    THEN LTRIM(RTRIM(CONVERT(VARCHAR(20), TR.RUT_TRP))) + 
                         ISNULL('-' + LTRIM(RTRIM(ISNULL(TR.DV, ''))), '')
                ELSE LTRIM(RTRIM(CONVERT(VARCHAR(20), TR.RUT_TRP)))
            END) AS RUT_TRANSPORTISTA,
            MAX(TR.NOM_TRP) AS NOMBRE_TRANSPORTISTA,
            MAX(D.CHOFER + ' RUT: ' + D.RUT_CHOFER) AS CHOFER,
            MAX(D.PATENTE) AS PATENTE,
            
            -- Información de embarque - tomar los valores no nulos
            MAX(D.NRO_CONT) AS NUMERO_CONTENEDOR,
            MAX(CASE WHEN ISNULL(D.SELLOS, '') <> '' THEN D.SELLOS ELSE NULL END) AS SELLOS,
            MAX(TER.TERMOGRAFOS) AS TERMOGRAFOS,
            MAX(CASE 
                WHEN D.HORA_PRE IS NOT NULL 
                THEN REPLACE(CONVERT(VARCHAR(50), D.HORA_PRE, 120), 'Ene  1 1900 ', '')
                ELSE NULL 
            END) AS HORA_PRESENTACION,
            MAX(CS.CODIGO_PLANTA_SAG) AS CODIGO_PLANTA_SAG,
            
            -- Nave y puertos
            MAX(D.TIP_TRA + '-' + D.COD_SIT) AS CODIGO_NAVE,
            MAX(N.NOM_SIT) AS NOMBRE_NAVE,
            MAX(P.NOM_PUE) AS PUERTO_EMBARQUE,
            MAX(DEST.DESCRIPCION) AS PUERTO_DESTINO,
            
            -- Observaciones
            MAX(D.OBSERVACION) AS OBSERVACION,
            
            -- Tipo de despacho y traslado
            MAX(CASE 
                WHEN ISNULL(N.SW_VENTA_NACIONAL, 0) = 0 
                THEN '8 TRASLADO EXPORTACION (NO VENTA)' 
                ELSE '1 OPERACION CONSTITUYE VENTA' 
            END) AS TIPO_TRASLADO,
            '1 NO ESPECIFICADO' AS TIPO_DESPACHO,
            
            -- Información OEM si existe
            MAX(OE.COD_CONSIG) AS COD_CONSIG,
            MAX(R2.NOM_REC) AS NOMBRE_CONSIGNATARIO
            
        FROM CTE_DESPACHOS_UNION D
        LEFT JOIN RECIBIDORES R ON D.COD_EMP = R.COD_EMP 
            AND D.COD_TEM = R.COD_TEM 
            AND D.COD_REC = R.COD_REC
        LEFT JOIN EXPORTADORES EX ON D.COD_EMP = EX.COD_EMP 
            AND D.COD_TEM = EX.COD_TEM 
            AND D.COD_EXP = EX.COD_EXP
        LEFT JOIN TRANSPORTISTAS TR ON D.COD_EMP = TR.COD_EMP 
            AND D.COD_TEM = TR.COD_TEM 
            AND D.COD_TRP = TR.COD_TRP
        LEFT JOIN NAVES N ON D.COD_EMP = N.COD_EMP 
            AND D.COD_TEM = N.COD_TEM 
            AND D.TIP_TRA = N.TIP_TRA 
            AND D.COD_SIT = N.COD_SIT
        LEFT JOIN PUERTOS P ON N.COD_EMP = P.COD_EMP 
            AND N.COD_TEM = P.COD_TEM 
            AND N.COD_PUE_ZAR = P.COD_PUE
        LEFT JOIN DESTINOS DEST ON N.COD_EMP = DEST.COD_EMP 
            AND N.COD_TEM = DEST.COD_TEM 
            AND N.COD_PUE_ARR = DEST.COD_DES
        LEFT JOIN TIT_OEM OE ON OE.COD_EMP = D.COD_EMP 
            AND OE.COD_TEM = D.COD_TEM 
            AND OE.ZON = D.ZON 
            AND OE.PLANILLA = D.NRO_EMB
        LEFT JOIN RECIBIDORES R2 ON OE.COD_EMP = R2.COD_EMP 
            AND OE.COD_TEM = R2.COD_TEM 
            AND OE.COD_CONSIG = R2.COD_REC
        LEFT JOIN CONFIGURACION_SAG CS ON D.COD_EMP = CS.COD_EMP 
            AND D.COD_TEM = CS.COD_TEM 
            AND D.COD_FRI = CS.COD_FRI
        LEFT JOIN CTE_TERMOGRAFOS TER ON D.COD_EMP = TER.COD_EMP
    ),

    CTE_TOTALES AS (
        -- Calcular totales del documento
        SELECT 
            SUM(TOTAL_LINEA) AS MONTO_NETO,
            0 AS MONTO_EXENTO,
            ROUND(SUM(TOTAL_LINEA) * 0.19, 0) AS IVA,
            ROUND(SUM(TOTAL_LINEA) * 1.19, 0) AS TOTAL_DOCUMENTO,
            SUM(CANTIDAD) AS TOTAL_CAJAS,
            COUNT(DISTINCT CODIGO_PRODUCTO) AS CANTIDAD_ITEMS,
            SUM(PESO_NETO_TOTAL) AS PESO_NETO_TOTAL,
            SUM(PESO_BRUTO_TOTAL) AS PESO_BRUTO_TOTAL
        FROM CTE_DETALLE
    ),

    CTE_PALLETS AS (
        -- Calcular cantidad de pallets
        SELECT 
            P.COD_EMP,
            P.NUM_FACT,
            ISNULL((
                SELECT COUNT(*) 
                FROM DESPACHOS 
                WHERE COD_EMP = P.COD_EMP 
                    AND GUIA_DES = P.NUM_FACT  -- Ya no necesita conversión
                    AND SW_ELEC = 1
            ), 0) AS PALLETS
        FROM CTE_PARAMETROS P
    )

    -- Resultado final combinando encabezado, detalle y totales
    SELECT 
        -- Información del documento
        E.FOLIO_DOCUMENTO,
        E.FECHA_EMISION,
        
        -- Receptor
        E.RUT_RECEPTOR,
        E.RAZON_SOCIAL_RECEPTOR,
        E.DIRECCION_RECEPTOR,
        E.CIUDAD_RECEPTOR,
        E.COMUNA_RECEPTOR,
        E.GIRO_RECEPTOR,
        
        -- Exportador
        E.RUT_EXPORTADOR,
        E.NOMBRE_EXPORTADOR,
        
        -- Transporte
        E.RUT_TRANSPORTISTA,
        E.NOMBRE_TRANSPORTISTA,
        E.CHOFER,
        E.PATENTE,
        
        -- Embarque
        E.NUMERO_CONTENEDOR,
        E.SELLOS,
        E.TERMOGRAFOS,
        E.HORA_PRESENTACION,
        E.CODIGO_PLANTA_SAG,
        E.CODIGO_NAVE,
        E.NOMBRE_NAVE,
        E.PUERTO_EMBARQUE,
        E.PUERTO_DESTINO,
        
        -- Tipo de operación
        E.TIPO_DESPACHO,
        E.TIPO_TRASLADO,
        
        -- Detalle de items
        D.NRO_LINEA,
        D.CODIGO_PRODUCTO,
        D.ITEM_DESCRIPCION,
        D.CANTIDAD,
        D.PRECIO_UNITARIO,
        D.TOTAL_LINEA,
        
        -- Totales
        T.MONTO_NETO,
        T.MONTO_EXENTO,
        T.IVA,
        T.TOTAL_DOCUMENTO,
        T.TOTAL_CAJAS,
        T.CANTIDAD_ITEMS,
        T.PESO_NETO_TOTAL,
        T.PESO_BRUTO_TOTAL,
        PAL.PALLETS,
        
        -- Observaciones
        E.OBSERVACION,
        E.COD_CONSIG,
        E.NOMBRE_CONSIGNATARIO,
        
        -- Observaciones formateadas para el documento
        'Exportador: ' + E.NOMBRE_EXPORTADOR + ' Rut: ' + CONVERT(VARCHAR(20), E.RUT_EXPORTADOR) + 
        ', Recibidor: ' + E.RAZON_SOCIAL_RECEPTOR + ' Rut: ' + CONVERT(VARCHAR(20), E.RUT_RECEPTOR) AS OBS_FACT,
        
        'Contenedor: ' + ISNULL(E.NUMERO_CONTENEDOR, '') + 
        ', Sellos: ' + ISNULL(E.SELLOS, '') + 
        ', Termografos: ' + ISNULL(E.TERMOGRAFOS, '') + 
        ', Hora: ' + ISNULL(E.HORA_PRESENTACION, '') + 
        ', Cod.SAG Planta: ' + ISNULL(E.CODIGO_PLANTA_SAG, '') AS OBS_FACT2,
        
        'Observacion: ' + ISNULL(E.OBSERVACION, '') + 
        CASE 
            WHEN ISNULL(E.COD_CONSIG, '') = '' THEN '' 
            ELSE ' Consignatario: ' + LTRIM(E.NOMBRE_CONSIGNATARIO) 
        END AS OBS_FACT3
        
    FROM CTE_DETALLE D
    LEFT JOIN CTE_ENCABEZADO E ON 1=1  -- Solo hay un encabezado por documento
    CROSS JOIN CTE_TOTALES T
    CROSS JOIN CTE_PALLETS PAL
    
END
GO

-- =============================================
-- EJEMPLOS DE USO DEL PROCEDIMIENTO:
-- =============================================

/*
-- Ejemplo 1: Ejecutar para guía específica con empresa por defecto (MER)
EXEC SP_GUIA_DESPACHO_ELECTRONICA @NUM_FACT = '72699'

-- Ejemplo 2: Ejecutar para guía con formato número-número
EXEC SP_GUIA_DESPACHO_ELECTRONICA @NUM_FACT = '2004-4'

-- Ejemplo 3: Ejecutar para guía específica con empresa diferente
EXEC SP_GUIA_DESPACHO_ELECTRONICA @COD_EMP = 'ABC', @NUM_FACT = '74151'

-- Ejemplo 4: Usar parámetros nombrados
EXEC SP_GUIA_DESPACHO_ELECTRONICA 
    @COD_EMP = 'MER', 
    @NUM_FACT = '72699'
*/
