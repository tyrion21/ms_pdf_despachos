package com.jason.service;

import com.jason.model.FeDteEnvWs;
import com.jason.model.FeDteEnvWsId;
import com.jason.repository.FeDteEnvWsRepository;
import com.jason.repository.DispatchDetailRepository;
import com.jason.model.DispatchDetailLine;
import com.itextpdf.kernel.colors.ColorConstants;
import com.itextpdf.kernel.colors.DeviceRgb;
import com.itextpdf.kernel.font.PdfFont;
import com.itextpdf.kernel.font.PdfFontFactory;
import com.itextpdf.kernel.geom.PageSize;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.borders.Border;
import com.itextpdf.layout.borders.SolidBorder;
import com.itextpdf.layout.element.Cell;
import com.itextpdf.layout.element.Image;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.element.Table;
import com.itextpdf.layout.properties.TextAlignment;
import com.itextpdf.layout.properties.UnitValue;
import com.itextpdf.layout.properties.VerticalAlignment;
import com.itextpdf.barcodes.BarcodePDF417;
import com.itextpdf.io.font.constants.StandardFonts;
import com.itextpdf.io.image.ImageDataFactory;

import org.w3c.dom.Element;
import org.xml.sax.InputSource;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.ByteArrayOutputStream;
import java.io.StringReader;
import java.io.InputStream;
import org.springframework.stereotype.Service;
import java.io.IOException;
import java.time.format.DateTimeFormatter;
import java.util.Optional;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;

@Service
public class PdfGenerationService {

        private final FeDteEnvWsRepository repository;
        private final DispatchDetailRepository detailRepository;
        private static final DateTimeFormatter DATE_FORMAT_EMISION = DateTimeFormatter.ofPattern("MMM dd yyyy", Locale.ENGLISH);
        private static final DateTimeFormatter DATE_FORMAT_VENC = DateTimeFormatter.ofPattern("dd-MM-yyyy");
        private static final DecimalFormat MONEY_FORMAT;
        static {
                DecimalFormatSymbols sym = new DecimalFormatSymbols(new Locale("es","CL"));
                sym.setDecimalSeparator(',');
                sym.setGroupingSeparator('.');
                MONEY_FORMAT = new DecimalFormat("#,##0", sym);
        }

        public PdfGenerationService(FeDteEnvWsRepository repository, DispatchDetailRepository detailRepository) {
                this.repository = repository;
                this.detailRepository = detailRepository;
        }

        public byte[] generateDispatchGuidePdf(String codEmp, Integer tipoDoc, Float caf) throws IOException {
                FeDteEnvWsId id = new FeDteEnvWsId(codEmp, tipoDoc, caf);
                Optional<FeDteEnvWs> dataOptional = repository.findById(id);

                if (dataOptional.isEmpty()) {
                        throw new RuntimeException("Datos de la Guía de Despacho no encontrados para: " + codEmp + ", "
                                        + tipoDoc + ", " + caf);
                }
                FeDteEnvWs data = dataOptional.get();

                // Obtener detalle desde SP (NUM_FACT asumido = caf)
                String numFact = String.valueOf(caf != null ? caf.intValue() : 0);
                java.util.List<DispatchDetailLine> detailLines = java.util.Collections.emptyList();
                try {
                        detailLines = detailRepository.fetchDetail(codEmp, numFact, data.getSysOrigen());
                } catch (Exception e) {
                        System.err.println("Error obteniendo detalle del SP: " + e.getMessage());
                }

                // Derivar datos adicionales desde primera línea del detalle (si existe)
                DispatchDetailLine headerLine = (detailLines != null && !detailLines.isEmpty()) ? detailLines.get(0) : null;
                String puertoEmbarque = headerLine != null ? safe(headerLine.getPuertoEmbarque()) : "";
                String puertoDestino = headerLine != null ? safe(headerLine.getPuertoDestino()) : "";
                String observacion = headerLine != null ? safe(headerLine.getObservacion()) : "";
                int totalCajasVal = headerLine != null && headerLine.getTotalCajas()!=null ? headerLine.getTotalCajas() : (detailLines!=null?detailLines.stream().map(l-> l.getQuantity()==null?0:l.getQuantity()).reduce(0,Integer::sum):0);
                int palletsVal = headerLine != null && headerLine.getPallets()!=null ? headerLine.getPallets() : 0;
                String totalBultosTexto = "Cajas= " + totalCajasVal + " Pallets: " + palletsVal;
                float pesoNetoTotalCalc = headerLine != null && headerLine.getPesoNetoTotal()!=null ? headerLine.getPesoNetoTotal() : 0f;
                float pesoBrutoTotalCalc = headerLine != null && headerLine.getPesoBrutoTotal()!=null ? headerLine.getPesoBrutoTotal() : 0f;
                String rutTransportista = headerLine != null ? safe(headerLine.getRutTransportista()) : "";
                String chofer = headerLine != null ? safe(headerLine.getChofer()) : "";
                // codigo_nave se usará como EXP y M/N
                String exp = headerLine != null ? safe(headerLine.getCodigoNave()) : "";
                String montoNetoMN = exp;
                String patente = headerLine != null ? safe(headerLine.getPatente()) : "";

                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                PdfWriter writer = new PdfWriter(baos);
                PdfDocument pdf = new PdfDocument(writer);
                Document document = new Document(pdf, PageSize.A4);

                // Configurar márgenes más pequeños para aprovechar el espacio
                document.setMargins(15, 15, 15, 15);

                // Crear fuente por defecto
                PdfFont font = PdfFontFactory.createFont(StandardFonts.HELVETICA);
                PdfFont boldFont = PdfFontFactory.createFont(StandardFonts.HELVETICA_BOLD);

                // --- 1. ENCABEZADO PRINCIPAL (3 columnas) ---
                Table mainHeaderTable = new Table(UnitValue.createPercentArray(new float[] { 25f, 50f, 25f }));
                mainHeaderTable.setWidth(UnitValue.createPercentValue(100));

                // Columna izquierda: LOGO
                Cell leftCell = new Cell().setBorder(Border.NO_BORDER)
                                .setTextAlignment(TextAlignment.LEFT)
                                .setVerticalAlignment(VerticalAlignment.TOP)
                                .setPadding(0);

                try {
                        ClassLoader classLoader = getClass().getClassLoader();
                        InputStream logoStream = classLoader.getResourceAsStream("templates/LogoQuelen.png");
                        if (logoStream != null) {
                                byte[] logoBytes = logoStream.readAllBytes();
                                Image logo = new Image(ImageDataFactory.create(logoBytes));
                                logo.scaleAbsolute(80, 60);
                                logo.setHorizontalAlignment(com.itextpdf.layout.properties.HorizontalAlignment.LEFT);
                                leftCell.add(logo);
                                logoStream.close();
                        }
                } catch (Exception e) {
                        System.err.println("No se pudo cargar el logo: " + e.getMessage());
                }
                mainHeaderTable.addCell(leftCell);

                // Columna central: Información de la empresa
                Cell centerCell = new Cell().setBorder(Border.NO_BORDER)
                                .setTextAlignment(TextAlignment.CENTER)
                                .setVerticalAlignment(VerticalAlignment.TOP)
                                .setPadding(0);

                centerCell.add(new Paragraph("PACKING MERQUEN SPA").setFont(boldFont).setFontSize(10)
                                .setMarginBottom(1));
                centerCell.add(new Paragraph("GIRO: EXPLOTACIÓN FRIGORÍFICOS Y VENTA DE PRODUCTOS").setFont(font)
                                .setFontSize(7).setMarginBottom(0));
                centerCell.add(new Paragraph("AGRÍCOLAS").setFont(font).setFontSize(7).setMarginBottom(1));
                centerCell.add(new Paragraph("Dirección: Panamericana Sur km 41").setFont(font).setFontSize(7)
                                .setMarginBottom(1));
                centerCell.add(new Paragraph("PAINE").setFont(font).setFontSize(7).setMarginBottom(1));
                centerCell.add(new Paragraph("Fono: +56232512020 / merquen@quelenexport.cl").setFont(font)
                                .setFontSize(7).setMarginBottom(1));
                centerCell.add(new Paragraph("WEB: www.quelenexport.cl").setFont(font).setFontSize(7)
                                .setMarginBottom(1));
                centerCell.add(new Paragraph("Sucursal: Ruta 5 Sur Parcela 96 A KM 41, Paine.").setFont(font)
                                .setFontSize(7));

                mainHeaderTable.addCell(centerCell);

                // Columna derecha: Cuadro del documento
                Cell rightCell = new Cell().setBorder(Border.NO_BORDER)
                                .setTextAlignment(TextAlignment.CENTER)
                                .setVerticalAlignment(VerticalAlignment.TOP)
                                .setPadding(0);

                // Crear tabla interna para el cuadro del documento con borde rojo único
                Table docTable = new Table(UnitValue.createPercentArray(new float[] { 1f }));
                docTable.setWidth(UnitValue.createPercentValue(100));
                docTable.setBorder(new SolidBorder(new DeviceRgb(255, 0, 0), 2));

                // Una sola celda con todo el contenido
                Cell docCell = new Cell()
                                .setBorder(Border.NO_BORDER)
                                .setTextAlignment(TextAlignment.CENTER)
                                .setPadding(10);

                docCell.add(new Paragraph(data.getRutEmpresa() + "-" + data.getDvEmpresa()).setFont(boldFont)
                                .setFontSize(10).setMarginBottom(3));
                docCell.add(new Paragraph("GUIA DE DESPACHO ELECTRONICA").setFont(boldFont).setFontSize(8)
                                .setMarginBottom(3));
                docCell.add(new Paragraph("NRO: " + data.getId().getCaf().intValue()).setFont(boldFont).setFontSize(9)
                                .setFontColor(new DeviceRgb(255, 0, 0)).setMarginBottom(3));
                docCell.add(new Paragraph("S.I.I. - LAS CONDES").setFont(font).setFontSize(7));

                docTable.addCell(docCell);

                rightCell.add(docTable);
                mainHeaderTable.addCell(rightCell);

                document.add(mainHeaderTable);
                document.add(new Paragraph(" ").setFontSize(5));

                // --- 2. TABLA DE INFORMACIÓN DEL CLIENTE ---
                Table clientInfoTable = new Table(UnitValue.createPercentArray(new float[] { 1f }));
                clientInfoTable.setWidth(UnitValue.createPercentValue(100));
                clientInfoTable.setBorder(new SolidBorder(ColorConstants.BLACK, 1));

                // FECHA EMISION
                Cell fechaEmisionCell = new Cell()
                                .setBackgroundColor(new DeviceRgb(230, 230, 230))
                                .setBorder(Border.NO_BORDER)
                                .setPadding(5);
                Table fechaTable = new Table(UnitValue.createPercentArray(new float[] { 20f, 80f }));
                fechaTable.setWidth(UnitValue.createPercentValue(100));
                fechaTable.addCell(new Cell().setBorder(Border.NO_BORDER)
                                .add(new Paragraph("FECHA EMISION").setFont(boldFont).setFontSize(7)));
                fechaTable.addCell(new Cell().setBorder(Border.NO_BORDER).add(new Paragraph(
                                data.getFechaEmision() != null
                                                ? data.getFechaEmision().format(DATE_FORMAT_EMISION)
                                                : "Nov 24 2024")
                                .setFont(font).setFontSize(7)));
                fechaEmisionCell.add(fechaTable);
                clientInfoTable.addCell(fechaEmisionCell);

                // Información del cliente en dos columnas
                Table clientDetailsTable = new Table(UnitValue.createPercentArray(new float[] { 15f, 35f, 15f, 35f }));
                clientDetailsTable.setWidth(UnitValue.createPercentValue(100));

                // SEÑOR(ES) y RUT
                clientDetailsTable.addCell(createInfoCell("SEÑOR(ES)", true, 7, boldFont));
                clientDetailsTable.addCell(
                                createInfoCell(data.getRazonSocialCliente() != null ? data.getRazonSocialCliente()
                                                : "PACKING MERQUEN SPA", false, 7, font));
                clientDetailsTable.addCell(createInfoCell("RUT", true, 7, boldFont));
                clientDetailsTable.addCell(createInfoCell(
                                data.getRutCliente() != null ? data.getRutCliente() : "77015053-1", false, 7, font));

                // DIRECCION y CIUDAD
                clientDetailsTable.addCell(createInfoCell("DIRECCION", true, 7, boldFont));
                clientDetailsTable
                                .addCell(createInfoCell(
                                                data.getDireccionCliente() != null ? data.getDireccionCliente()
                                                                : "RUTA 5 SUR PARCELA 96 A KM 41 PAINE",
                                                false, 7, font));
                clientDetailsTable.addCell(createInfoCell("CIUDAD", true, 7, boldFont));
                clientDetailsTable.addCell(
                                createInfoCell(data.getCiudadCliente() != null ? data.getCiudadCliente() : "SANTIAGO",
                                                false, 7, font));

                // GIRO y COMUNAv
                clientDetailsTable.addCell(createInfoCell("GIRO", true, 7, boldFont));
                clientDetailsTable.addCell(createInfoCell(
                                data.getGiroCliente() != null ? data.getGiroCliente()
                                                : "EXPLOTACION FRIGORIFICOS Y VENTA DE PRODUCTOS AGRICOLAS.",
                                false, 7, font));
                clientDetailsTable.addCell(createInfoCell("COMUNA", true, 7, boldFont));
                clientDetailsTable.addCell(createInfoCell(
                                data.getComunaCliente() != null ? data.getComunaCliente() : "PAINE", false, 7, font));

                // Agregar fila con TIPO DESPACHO y TIPO TRASLADO dentro de la misma tabla de detalles
                clientDetailsTable.addCell(createInfoCell("TIPO DESPACHO", true, 7, boldFont));
                clientDetailsTable.addCell(createInfoCell((data.getTdes()!=null?String.valueOf((int)data.getTdes().floatValue()):"1") + " NO ESPECIFICADO", false, 7, font));
                String tipoTrasladoValor = headerLine != null && safe(headerLine.getTipoTraslado()).length() > 0
                                ? headerLine.getTipoTraslado()
                                : (data.getTras() != null ? String.valueOf((int) data.getTras().floatValue()) : "5");
                clientDetailsTable.addCell(createInfoCell("TIPO TRASLADO", true, 7, boldFont));
                clientDetailsTable.addCell(createInfoCell(tipoTrasladoValor + " TRASLADOS INTERNOS", false, 7, font));

                clientInfoTable.addCell(new Cell().setBorder(Border.NO_BORDER).add(clientDetailsTable).setPadding(3));

                // Información de destino y transporte
                Table transportTable = new Table(UnitValue.createPercentArray(new float[] { 15f, 35f, 15f, 35f }));
                transportTable.setWidth(UnitValue.createPercentValue(100));

                // DIRECCION DESTINO y PUERTO EMBARQUE
                transportTable.addCell(createInfoCell("DIRECCION DESTINO", true, 7, boldFont));
                transportTable.addCell(createInfoCell(data.getDireccionCliente() != null ? data.getDireccionCliente()
                                : "RUTA 5 SUR PARCELA 96 A KM 41 PAINE", false, 7, font));
                transportTable.addCell(createInfoCell("PUERTO EMBARQUE", true, 7, boldFont));
                transportTable.addCell(createInfoCell(puertoEmbarque, false, 7, font));

                // COMUNA DESTINO y PUERTO DESEMBARQUE
                transportTable.addCell(createInfoCell("COMUNA DESTINO", true, 7, boldFont));
                transportTable.addCell(createInfoCell(
                                data.getComunaCliente() != null ? data.getComunaCliente() : "PAINE", false, 7, font));
                transportTable.addCell(createInfoCell("PUERTO DESEMBARQUE", true, 7, boldFont));
                transportTable.addCell(createInfoCell(puertoDestino, false, 7, font));

                // CIUDAD DESTINO y PESO TOTAL BRUTO
                transportTable.addCell(createInfoCell("CIUDAD DESTINO", true, 7, boldFont));
                transportTable.addCell(
                                createInfoCell(data.getCiudadCliente() != null ? data.getCiudadCliente() : "SANTIAGO",
                                                false, 7, font));
                transportTable.addCell(createInfoCell("PESO TOTAL BRUTO", true, 7, boldFont));
                transportTable.addCell(createInfoCell(String.format("%.2f", pesoBrutoTotalCalc), false, 7, font));

                // MODALIDAD VENTA y PESO TOTAL NETO
                transportTable.addCell(createInfoCell("MODALIDAD VENTA", true, 7, boldFont));
                transportTable.addCell(createInfoCell("", false, 7, font));
                transportTable.addCell(createInfoCell("PESO TOTAL NETO", true, 7, boldFont));
                transportTable.addCell(createInfoCell(String.format("%.2f", pesoNetoTotalCalc), false, 7, font));

                // RUT TRANSPORTISTA y TOTAL BULTOS
                transportTable.addCell(createInfoCell("RUT TRANSPORTISTA", true, 7, boldFont));
                transportTable.addCell(createInfoCell(rutTransportista, false, 7, font));
                transportTable.addCell(createInfoCell("TOTAL BULTOS", true, 7, boldFont));
                transportTable.addCell(createInfoCell(totalBultosTexto, false, 7, font));

                // CHOFER y EXP.
                transportTable.addCell(createInfoCell("CHOFER", true, 7, boldFont));
                transportTable.addCell(createInfoCell(chofer, false, 7, font));
                transportTable.addCell(createInfoCell("EXP.", true, 7, boldFont));
                transportTable.addCell(createInfoCell(exp, false, 7, font));

                // PATENTE y M/N
                transportTable.addCell(createInfoCell("PATENTE", true, 7, boldFont));
                transportTable.addCell(createInfoCell(patente + " Carro:", false, 7, font));
                transportTable.addCell(createInfoCell("M/N", true, 7, boldFont));
                transportTable.addCell(createInfoCell(montoNetoMN, false, 7, font));

                clientInfoTable.addCell(new Cell().setBorder(Border.NO_BORDER)
                                .setBorderTop(new SolidBorder(ColorConstants.BLACK, 1)).add(transportTable)
                                .setPadding(3));

                document.add(clientInfoTable);
                document.add(new Paragraph(" ").setFontSize(5));

                // --- 3. TABLA DE PRODUCTOS ---
                Table productTable = new Table(
                                UnitValue.createPercentArray(new float[] { 15f, 45f, 15f, 12.5f, 12.5f }));
                productTable.setWidth(UnitValue.createPercentValue(100));
                productTable.setBorder(new SolidBorder(ColorConstants.BLACK, 1));

                // Headers con fondo gris
                productTable.addCell(createProductHeaderCell("CODIGO", boldFont, TextAlignment.LEFT));
                productTable.addCell(createProductHeaderCell("DETALLE", boldFont, TextAlignment.LEFT));
                productTable.addCell(createProductHeaderCell("PRECIO", boldFont, TextAlignment.RIGHT));
                productTable.addCell(createProductHeaderCell("CANTIDAD", boldFont, TextAlignment.CENTER));
                productTable.addCell(createProductHeaderCell("TOTAL", boldFont, TextAlignment.RIGHT));

                // Datos dinámicos del producto desde SP
                if (detailLines != null && !detailLines.isEmpty()) {
                        for (DispatchDetailLine l : detailLines) {
                                productTable.addCell(createProductDataCell(safe(l.getProductCode()), font));
                                productTable.addCell(createProductDataCell(safe(l.getDescription()), font));
                                productTable.addCell(createProductDataCell(
                                                l.getUnitPrice() != null ? formatMoneyFlexible(l.getUnitPrice()) : "",
                                                font).setTextAlignment(TextAlignment.RIGHT));
                                productTable.addCell(createProductDataCell(
                                                l.getQuantity() != null ? String.valueOf(l.getQuantity()) : "",
                                                font).setTextAlignment(TextAlignment.CENTER));
                                productTable.addCell(createProductDataCell(
                                                l.getLineTotal() != null ? formatMoneyFlexible(l.getLineTotal()) : "",
                                                font).setTextAlignment(TextAlignment.RIGHT));
                        }
                }
                // Filas vacías para completar (mínimo 6 filas total visibles)
                int currentRows = detailLines != null ? detailLines.size() : 0;
                int minRows = 6;
                for (int i = currentRows; i < minRows; i++) {
                        for (int j = 0; j < 5; j++) {
                                productTable.addCell(createProductDataCell("", font).setMinHeight(12));
                        }
                }

                document.add(productTable);
                document.add(new Paragraph(" ").setFontSize(5));

                
                
                // --- 4. OBSERVACIONES (antes del timbre) ---
                String obsBlockEarly = buildObservationBlock(headerLine, observacion);
                if(!obsBlockEarly.isBlank()){
                        document.add(new Paragraph(obsBlockEarly).setFont(font).setFontSize(7));
                        document.add(new Paragraph(" ").setFontSize(4));
                }

                // --- 5. SECCIÓN COMBINADA: TIMBRE Y TOTALES ---
                Table combinedTable = new Table(UnitValue.createPercentArray(new float[] { 50f, 50f }));
                combinedTable.setWidth(UnitValue.createPercentValue(100));

                // Columna izquierda: TIMBRE ELECTRÓNICO
                Cell timbreCell = new Cell().setBorder(Border.NO_BORDER)
                                .setTextAlignment(TextAlignment.LEFT)
                                .setVerticalAlignment(VerticalAlignment.BOTTOM)
                                .setPadding(5);

                String tedXml = data.getSTimb();
                if (tedXml != null && !tedXml.isEmpty()) {
                        try {
                                String barcodeContent = parseTedForQrContent(tedXml);
                                BarcodePDF417 barcode = new BarcodePDF417();
                                barcode.setCode(barcodeContent);
                                // Configuración para hacer el código ancho con más altura
                                barcode.setCodeColumns(20); // Columnas para formato ancho
                                barcode.setCodeRows(5); // Más filas para darle altura
                                barcode.setOptions(BarcodePDF417.PDF417_FIXED_RECTANGLE);
                                barcode.setAspectRatio(0.3f); // Rectangular
                                barcode.setYHeight(3); // Altura de módulo normal

                                Image barcodeImage = new Image(barcode.createFormXObject(pdf));
                                // 1cm = aproximadamente 28 puntos, pero le damos un poco más de ancho
                                barcodeImage.scaleAbsolute(200, 40); // Ancho y con 1cm+ de altura
                                barcodeImage.setHorizontalAlignment(
                                                com.itextpdf.layout.properties.HorizontalAlignment.LEFT);
                                timbreCell.add(barcodeImage);
                        } catch (Exception e) {
                                System.err.println("Error al generar el código PDF417 del timbre: " + e.getMessage());
                        }
                }

                // Texto del timbre alineado a la izquierda
                timbreCell.add(new Paragraph("Timbre Electronico SII").setFont(font).setFontSize(6)
                                .setTextAlignment(TextAlignment.LEFT)
                                .setMarginTop(2).setMarginBottom(0));
                timbreCell.add(new Paragraph("Res 80 de 22/08/2014 - Verifique Documento: www.sii.cl").setFont(font)
                                .setFontSize(6)
                                .setTextAlignment(TextAlignment.LEFT)
                                .setMarginTop(0));

                combinedTable.addCell(timbreCell);

                // Columna derecha: TOTALES
                Cell totalesCell = new Cell().setBorder(Border.NO_BORDER)
                                .setTextAlignment(TextAlignment.RIGHT)
                                .setVerticalAlignment(VerticalAlignment.TOP)
                                .setPadding(5);

                Table totalsTable = new Table(UnitValue.createPercentArray(new float[] { 60f, 40f }));
                totalsTable.setWidth(UnitValue.createPercentValue(60));
                totalsTable.setHorizontalAlignment(com.itextpdf.layout.properties.HorizontalAlignment.RIGHT);
                totalsTable.setBorder(new SolidBorder(ColorConstants.BLACK, 1));

                totalsTable.addCell(createTotalCell("MONTO NETO", false, boldFont));
                totalsTable.addCell(createTotalCell(formatMoneyFlexible(data.getNeto()), false, font));

                totalsTable.addCell(createTotalCell("MONTO EXENTO", false, boldFont));
                totalsTable.addCell(createTotalCell(formatMoneyFlexible(data.getExen()), false, font));

                totalsTable.addCell(createTotalCell("IVA 19%", false, boldFont));
                totalsTable.addCell(createTotalCell(formatMoneyFlexible(data.getIvam()), false, font));

                totalsTable.addCell(createTotalCell("TOTAL", true, boldFont)
                                .setBackgroundColor(new DeviceRgb(230, 230, 230)));
                totalsTable.addCell(createTotalCell(formatMoneyFlexible(data.getTotal()), true, boldFont)
                                .setBackgroundColor(new DeviceRgb(230, 230, 230)));

                totalesCell.add(totalsTable);
                combinedTable.addCell(totalesCell);

                document.add(combinedTable);
                document.add(new Paragraph(" ").setFontSize(5));

                // --- 6. TABLA FINAL DE INFORMACIÓN ---
                Table finalInfoTable = new Table(UnitValue.createPercentArray(new float[] { 30f, 35f, 35f }));
                finalInfoTable.setWidth(UnitValue.createPercentValue(100));
                finalInfoTable.setBorder(new SolidBorder(ColorConstants.BLACK, 1));

                // FECHA VENCIMIENTO
                finalInfoTable.addCell(createFinalInfoCell("FECHA VENCIMIENTO", true, boldFont));
                // FECHA VENCIMIENTO debe reflejar siempre la fecha de emisión
                finalInfoTable.addCell(createFinalInfoCell(
                                (data.getFechaEmision() != null ? data.getFechaEmision().format(DATE_FORMAT_VENC)
                                                : "--"),
                                false, font));
                finalInfoTable.addCell(createFinalInfoCell("", false, font));

                // MONTO TOTAL
                finalInfoTable.addCell(createFinalInfoCell("MONTO TOTAL", true, boldFont));
                finalInfoTable.addCell(createFinalInfoCell(numberToWordsPesos(data.getTotal()), false, font));
                finalInfoTable.addCell(createFinalInfoCell("", false, font));

                document.add(finalInfoTable);
                document.add(new Paragraph(" ").setFontSize(5));

                // --- 7. INFORMACIÓN FINAL ---
                Table finalTextTable = new Table(UnitValue.createPercentArray(new float[] { 50f, 50f }));
                finalTextTable.setWidth(UnitValue.createPercentValue(100));
                finalTextTable.addCell(new Cell().setBorder(Border.NO_BORDER)
                                .setTextAlignment(TextAlignment.LEFT)
                                .add(new Paragraph("Documento Creado por www.sdt.cl").setFont(font).setFontSize(7))
                                .setPadding(0));
                finalTextTable.addCell(new Cell().setBorder(Border.NO_BORDER)
                                .setTextAlignment(TextAlignment.RIGHT)
                                .add(new Paragraph("ORIGINAL").setFont(boldFont).setFontSize(9))
                                .setPadding(0));
                document.add(finalTextTable);

                document.close();
                return baos.toByteArray();
        }

        // Métodos auxiliares
        private String safe(String v){ return v==null?"":v; }
        @SuppressWarnings("unused")
        private String formatMoney(Number n){
                if(n==null) return "0";
                try { return MONEY_FORMAT.format(n.doubleValue()); } catch(Exception e){ return String.valueOf(n); }
        }

        private String formatMoneyFlexible(Number n){
                if(n==null) return "0";
                double v = n.doubleValue();
                // Si tiene parte decimal distinta de 0, mostrar con coma decimal
                boolean hasDecimals = Math.abs(v - Math.rint(v)) > 0.0001;
                String base = MONEY_FORMAT.format(Math.floor(v));
                if(hasDecimals){
                        // obtener decimales con dos dígitos
                        String decimals = new java.text.DecimalFormat("00").format(Math.round((v - Math.floor(v))*100));
                        return base + "," + decimals;
                }
                return base;
        }

        private String buildObservationBlock(DispatchDetailLine headerLine, String observacionBase){
                StringBuilder sb = new StringBuilder();
                if(headerLine!=null){
                        sb.append("Exportador: ").append(safe(headerLine.getNombreExportador()))
                                .append(" Rut: ").append(safe(headerLine.getRutExportador()))
                                .append("  Recibidor: ").append(safe(headerLine.getRazonSocialReceptor()))
                                .append(" Rut: ").append(safe(headerLine.getRutReceptor()));
                        // Hora sin fecha si viene con componente fecha
                        String hora = safe(headerLine.getHoraPresentacion());
                        if(hora.contains(" ")){ // form "yyyy-MM-dd HH:mm:ss" o similar
                                hora = hora.substring(hora.lastIndexOf(' ')+1); // solo HH:mm:ss
                        }
                        if(hora.startsWith("1900") || hora.startsWith("00:")) hora = ""; // descartar valores dummy
                        sb.append("\nContenedor: ").append(safe(headerLine.getNumeroContenedor()))
                                .append("  Sellos: ").append(safe(headerLine.getSellos()))
                                .append("  Termógrafos: ").append(safe(headerLine.getTermografos()))
                                .append(hora.isEmpty()?"":"  Hora: ").append(hora)
                                .append("  Planta SAG: ").append(safe(headerLine.getCodigoPlantaSag()));
                        String obsAll = observacionBase==null?"":observacionBase;
                        if(safe(headerLine.getObsFact()).length()>0) obsAll += (obsAll.isEmpty()?"":" ")+ headerLine.getObsFact();
                        if(safe(headerLine.getObsFact2()).length()>0) obsAll += (obsAll.isEmpty()?"":" ")+ headerLine.getObsFact2();
                        if(safe(headerLine.getObsFact3()).length()>0) obsAll += (obsAll.isEmpty()?"":" ")+ headerLine.getObsFact3();
                        if(!obsAll.isEmpty()){
                                // Normalizar saltos y espacios
                                obsAll = obsAll.replace('\r',' ').replace('\n',' ').replaceAll("\\s+", " ").trim();
                                // Si existe etiqueta OBSERVACION: tomar solo el contenido posterior
                                java.util.regex.Matcher m = java.util.regex.Pattern.compile("(?i)OBSERVACION:\\s*(.*)").matcher(obsAll);
                                if(m.find()){
                                        obsAll = m.group(1).trim();
                                }
                                // Eliminar cualquier bloque duplicado que vuelva a empezar con EXPORTADOR:
                                int dup = obsAll.toUpperCase().indexOf("EXPORTADOR:");
                                if(dup >=0){
                                        // Tomar solo lo anterior al duplicado si contiene DUS antes; sino intentar rescatar después de 'Observacion:' ya hecho
                                        obsAll = obsAll.substring(0, dup).trim();
                                }
                                // Unir / CONTACTO si quedó separado por barras o espacios raros
                                obsAll = obsAll.replaceAll("(?i)(DUS\\s[0-9\\-]+)\\s*/\\s*CONTACTO", "$1 / CONTACTO");
                                // Asegurar que después de DUS no se genera corte: ya sin \n
                                sb.append("\n").append(obsAll);
                        }
                }
                return sb.toString();
        }

        private String numberToWordsPesos(Float total){
                long value = total==null?0:Math.round(Math.floor(total));
                if(value==0) return "CERO PESOS.-";
                return toSpanish(value).toUpperCase() + " PESOS.-";
        }

        private String toSpanish(long n){
                if(n==0) return "cero";
                if(n<0) return "menos " + toSpanish(-n);
                String[] unidades = {"","uno","dos","tres","cuatro","cinco","seis","siete","ocho","nueve","diez","once","doce","trece","catorce","quince","dieciséis","diecisiete","dieciocho","diecinueve"};
                String[] decenas = {"","diez","veinte","treinta","cuarenta","cincuenta","sesenta","setenta","ochenta","noventa"};
                String[] centenas = {"","cien","doscientos","trescientos","cuatrocientos","quinientos","seiscientos","setecientos","ochocientos","novecientos"};
                if(n<20) return unidades[(int)n];
                if(n<100){
                        int d=(int)(n/10);int u=(int)(n%10);
                        if(n<30){
                                if(n==20) return "veinte";
                                return "veinti" + (u==0?"":(u==2?"dós":u==3?"trés":unidades[u]));
                        }
                        return decenas[d] + (u==0?"":" y "+unidades[u]);
                }
                if(n<1000){
                        int c=(int)(n/100); int r=(int)(n%100);
                        if(n==100) return "cien";
                        return (centenas[c] + (r==0?"":" "+ toSpanish(r))).trim();
                }
                if(n<1_000_000){
                        long miles = n/1000; long r = n%1000;
                        String pref = miles==1?"mil":toSpanish(miles)+" mil";
                        return (pref + (r==0?"":" "+toSpanish(r))).trim();
                }
                if(n<1_000_000_000){
                        long millones = n/1_000_000; long r = n%1_000_000;
                        String pref = millones==1?"un millón":toSpanish(millones)+" millones";
                        return (pref + (r==0?"":" "+toSpanish(r))).trim();
                }
                long milesMillones = n/1_000_000_000; long r = n%1_000_000_000;
                String pref = toSpanish(milesMillones)+" mil millones";
                return (pref + (r==0?"":" "+toSpanish(r))).trim();
        }
        private Cell createInfoCell(String content, boolean bold, int fontSize, PdfFont font) {
                Paragraph p = new Paragraph(content != null ? content : "").setFont(font).setFontSize(fontSize);
                return new Cell().setBorder(Border.NO_BORDER).add(p).setPadding(1);
        }

        private Cell createProductHeaderCell(String content, PdfFont font, TextAlignment align) {
                return new Cell()
                                .setBackgroundColor(new DeviceRgb(230, 230, 230))
                                .setBorder(Border.NO_BORDER)
                                .setBorderBottom(new SolidBorder(ColorConstants.BLACK, 1))
                                .add(new Paragraph(content).setFont(font).setFontSize(8))
                                .setTextAlignment(align)
                                .setPadding(3);
        }

        private Cell createProductDataCell(String content, PdfFont font) {
                return new Cell()
                                .setBorder(Border.NO_BORDER)
                                .add(new Paragraph(content != null ? content : "").setFont(font).setFontSize(7))
                                .setPadding(2);
        }

        private Cell createTotalCell(String content, boolean bold, PdfFont font) {
                return new Cell()
                                .setBorder(Border.NO_BORDER)
                                .add(new Paragraph(content).setFont(font).setFontSize(8))
                                .setTextAlignment(TextAlignment.RIGHT)
                                .setPadding(3);
        }

        private Cell createFinalInfoCell(String content, boolean bold, PdfFont font) {
                return new Cell()
                                .setBackgroundColor(bold ? new DeviceRgb(230, 230, 230) : null)
                                .setBorder(Border.NO_BORDER)
                                .setBorderRight(new SolidBorder(ColorConstants.BLACK, 1))
                                .add(new Paragraph(content != null ? content : "").setFont(font).setFontSize(7))
                                .setTextAlignment(TextAlignment.LEFT)
                                .setPadding(3);
        }

        private String parseTedForQrContent(String tedXml) {
                try {
                        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
                        DocumentBuilder builder = factory.newDocumentBuilder();
                        org.w3c.dom.Document doc = builder.parse(new InputSource(new StringReader(tedXml)));
                        doc.getDocumentElement().normalize();

                        Element ddElement = (Element) doc.getElementsByTagName("DD").item(0);
                        if (ddElement != null) {
                                ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
                                javax.xml.transform.TransformerFactory transformerFactory = javax.xml.transform.TransformerFactory
                                                .newInstance();
                                javax.xml.transform.Transformer transformer = transformerFactory.newTransformer();
                                transformer.setOutputProperty(javax.xml.transform.OutputKeys.OMIT_XML_DECLARATION,
                                                "yes");
                                transformer.transform(new javax.xml.transform.dom.DOMSource(ddElement),
                                                new javax.xml.transform.stream.StreamResult(outputStream));
                                return outputStream.toString("UTF-8");
                        }
                } catch (Exception e) {
                        System.err.println("Error parsing sTIMB XML: " + e.getMessage());
                }
                return tedXml;
        }
}