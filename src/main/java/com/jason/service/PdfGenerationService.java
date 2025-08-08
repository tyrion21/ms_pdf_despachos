package com.jason.service;

import com.jason.model.FeDteEnvWs;
import com.jason.model.FeDteEnvWsId;
import com.jason.repository.FeDteEnvWsRepository;
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

@Service
public class PdfGenerationService {

        private final FeDteEnvWsRepository repository;
        private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy");

        public PdfGenerationService(FeDteEnvWsRepository repository) {
                this.repository = repository;
        }

        public byte[] generateDispatchGuidePdf(String codEmp, Integer tipoDoc, Float caf) throws IOException {
                FeDteEnvWsId id = new FeDteEnvWsId(codEmp, tipoDoc, caf);
                Optional<FeDteEnvWs> dataOptional = repository.findById(id);

                if (dataOptional.isEmpty()) {
                        throw new RuntimeException("Datos de la Guía de Despacho no encontrados para: " + codEmp + ", "
                                        + tipoDoc + ", " + caf);
                }
                FeDteEnvWs data = dataOptional.get();

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
                                                ? data.getFechaEmision()
                                                                .format(DateTimeFormatter.ofPattern("MMMM dd yyyy"))
                                                : "Jul 25 2025")
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

                // GIRO y COMUNA
                clientDetailsTable.addCell(createInfoCell("GIRO", true, 7, boldFont));
                clientDetailsTable.addCell(createInfoCell(
                                data.getGiroCliente() != null ? data.getGiroCliente()
                                                : "EXPLOTACION FRIGORIFICOS Y VENTA DE PRODUCTOS AGRICOLAS.",
                                false, 7, font));
                clientDetailsTable.addCell(createInfoCell("COMUNA", true, 7, boldFont));
                clientDetailsTable.addCell(createInfoCell(
                                data.getComunaCliente() != null ? data.getComunaCliente() : "PAINE", false, 7, font));

                clientInfoTable.addCell(new Cell().setBorder(Border.NO_BORDER).add(clientDetailsTable).setPadding(3));

                // TIPO DESPACHO
                Cell tipoDespachoCell = new Cell()
                                .setBackgroundColor(new DeviceRgb(230, 230, 230))
                                .setBorder(Border.NO_BORDER)
                                .setBorderTop(new SolidBorder(ColorConstants.BLACK, 1))
                                .setPadding(5);
                Table tipoDespachoTable = new Table(UnitValue.createPercentArray(new float[] { 20f, 80f }));
                tipoDespachoTable.setWidth(UnitValue.createPercentValue(100));
                tipoDespachoTable.addCell(new Cell().setBorder(Border.NO_BORDER)
                                .add(new Paragraph("TIPO DESPACHO").setFont(boldFont).setFontSize(7)));
                tipoDespachoTable.addCell(new Cell().setBorder(Border.NO_BORDER).add(new Paragraph(
                                (data.getTdes() != null ? String.valueOf((int) data.getTdes().floatValue()) : "1")
                                                + " NO ESPECIFICADO")
                                .setFont(font).setFontSize(7)));
                tipoDespachoCell.add(tipoDespachoTable);
                clientInfoTable.addCell(tipoDespachoCell);

                // TIPO TRASLADO
                Cell tipoTrasladoCell = new Cell()
                                .setBorder(Border.NO_BORDER)
                                .setBorderTop(new SolidBorder(ColorConstants.BLACK, 1))
                                .setPadding(5);
                Table tipoTrasladoTable = new Table(UnitValue.createPercentArray(new float[] { 20f, 80f }));
                tipoTrasladoTable.setWidth(UnitValue.createPercentValue(100));
                tipoTrasladoTable.addCell(new Cell().setBorder(Border.NO_BORDER)
                                .add(new Paragraph("TIPO TRASLADO").setFont(boldFont).setFontSize(7)));
                tipoTrasladoTable.addCell(new Cell().setBorder(Border.NO_BORDER).add(new Paragraph(
                                (data.getTras() != null ? String.valueOf((int) data.getTras().floatValue()) : "5")
                                                + " TRASLADOS INTERNOS")
                                .setFont(font).setFontSize(7)));
                tipoTrasladoCell.add(tipoTrasladoTable);
                clientInfoTable.addCell(tipoTrasladoCell);

                // Información de destino y transporte
                Table transportTable = new Table(UnitValue.createPercentArray(new float[] { 15f, 35f, 15f, 35f }));
                transportTable.setWidth(UnitValue.createPercentValue(100));

                // DIRECCION DESTINO y PUERTO EMBARQUE
                transportTable.addCell(createInfoCell("DIRECCION DESTINO", true, 7, boldFont));
                transportTable.addCell(createInfoCell(data.getDireccionCliente() != null ? data.getDireccionCliente()
                                : "RUTA 5 SUR PARCELA 96 A KM 41 PAINE", false, 7, font));
                transportTable.addCell(createInfoCell("PUERTO EMBARQUE", true, 7, boldFont));
                transportTable.addCell(createInfoCell("", false, 7, font));

                // COMUNA DESTINO y PUERTO DESEMBARQUE
                transportTable.addCell(createInfoCell("COMUNA DESTINO", true, 7, boldFont));
                transportTable.addCell(createInfoCell(
                                data.getComunaCliente() != null ? data.getComunaCliente() : "PAINE", false, 7, font));
                transportTable.addCell(createInfoCell("PUERTO DESEMBARQUE", true, 7, boldFont));
                transportTable.addCell(createInfoCell("", false, 7, font));

                // CIUDAD DESTINO y PESO TOTAL BRUTO
                transportTable.addCell(createInfoCell("CIUDAD DESTINO", true, 7, boldFont));
                transportTable.addCell(
                                createInfoCell(data.getCiudadCliente() != null ? data.getCiudadCliente() : "SANTIAGO",
                                                false, 7, font));
                transportTable.addCell(createInfoCell("PESO TOTAL BRUTO", true, 7, boldFont));
                transportTable.addCell(createInfoCell(
                                String.format("%.2f", data.getNeto() != null ? data.getNeto() : 0.00), false, 7, font));

                // MODALIDAD VENTA y PESO TOTAL NETO
                transportTable.addCell(createInfoCell("MODALIDAD VENTA", true, 7, boldFont));
                transportTable.addCell(createInfoCell("", false, 7, font));
                transportTable.addCell(createInfoCell("PESO TOTAL NETO", true, 7, boldFont));
                transportTable.addCell(createInfoCell(
                                String.format("%.0f", data.getTotal() != null ? data.getTotal() : 0), false, 7, font));

                // RUT TRANSPORTISTA y TOTAL BULTOS
                transportTable.addCell(createInfoCell("RUT TRANSPORTISTA", true, 7, boldFont));
                transportTable.addCell(createInfoCell("", false, 7, font));
                transportTable.addCell(createInfoCell("TOTAL BULTOS", true, 7, boldFont));
                transportTable.addCell(createInfoCell("Cajas= 0 Pallets: 0", false, 7, font));

                // CHOFER y EXP.
                transportTable.addCell(createInfoCell("CHOFER", true, 7, boldFont));
                transportTable.addCell(createInfoCell("", false, 7, font));
                transportTable.addCell(createInfoCell("EXP.", true, 7, boldFont));
                transportTable.addCell(createInfoCell("", false, 7, font));

                // PATENTE y M/N
                transportTable.addCell(createInfoCell("PATENTE", true, 7, boldFont));
                transportTable.addCell(createInfoCell("Carro:", false, 7, font));
                transportTable.addCell(createInfoCell("M/N", true, 7, boldFont));
                transportTable.addCell(createInfoCell("", false, 7, font));

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
                productTable.addCell(createProductHeaderCell("CODIGO", boldFont));
                productTable.addCell(createProductHeaderCell("DETALLE", boldFont));
                productTable.addCell(createProductHeaderCell("PRECIO", boldFont));
                productTable.addCell(createProductHeaderCell("CANTIDAD", boldFont));
                productTable.addCell(createProductHeaderCell("TOTAL", boldFont));

                // Datos del producto
                productTable.addCell(createProductDataCell("test2", font));
                productTable.addCell(createProductDataCell("test2", font));
                productTable.addCell(createProductDataCell("0,00", font).setTextAlignment(TextAlignment.RIGHT));
                productTable.addCell(createProductDataCell("1", font).setTextAlignment(TextAlignment.CENTER));
                productTable.addCell(createProductDataCell("0", font).setTextAlignment(TextAlignment.RIGHT));

                // Filas vacías para completar el espacio (menos filas para formato A4)
                for (int i = 0; i < 6; i++) {
                        for (int j = 0; j < 5; j++) {
                                productTable.addCell(createProductDataCell("", font).setMinHeight(12));
                        }
                }

                document.add(productTable);
                document.add(new Paragraph(" ").setFontSize(5));

                // --- 4. SECCIÓN COMBINADA: TIMBRE Y TOTALES ---
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
                totalsTable.addCell(createTotalCell(String.format("%.0f", data.getNeto() != null ? data.getNeto() : 0),
                                false, font));

                totalsTable.addCell(createTotalCell("MONTO EXENTO", false, boldFont));
                totalsTable.addCell(createTotalCell(String.format("%.0f", data.getExen() != null ? data.getExen() : 0),
                                false, font));

                totalsTable.addCell(createTotalCell("IVA 19%", false, boldFont));
                totalsTable.addCell(createTotalCell(String.format("%.0f", data.getIvam() != null ? data.getIvam() : 0),
                                false, font));

                totalsTable.addCell(createTotalCell("TOTAL", true, boldFont)
                                .setBackgroundColor(new DeviceRgb(230, 230, 230)));
                totalsTable.addCell(
                                createTotalCell(String.format("%.0f", data.getTotal() != null ? data.getTotal() : 0),
                                                true, boldFont).setBackgroundColor(new DeviceRgb(230, 230, 230)));

                totalesCell.add(totalsTable);
                combinedTable.addCell(totalesCell);

                document.add(combinedTable);
                document.add(new Paragraph(" ").setFontSize(5));

                // --- 5. TABLA FINAL DE INFORMACIÓN ---
                Table finalInfoTable = new Table(UnitValue.createPercentArray(new float[] { 30f, 35f, 35f }));
                finalInfoTable.setWidth(UnitValue.createPercentValue(100));
                finalInfoTable.setBorder(new SolidBorder(ColorConstants.BLACK, 1));

                // FECHA VENCIMIENTO
                finalInfoTable.addCell(createFinalInfoCell("FECHA VENCIMIENTO", true, boldFont));
                finalInfoTable.addCell(createFinalInfoCell(
                                (data.getFechaVenc() != null ? data.getFechaVenc().format(DATE_FORMATTER)
                                                : "25/07/2025"),
                                false, font));
                finalInfoTable.addCell(createFinalInfoCell("", false, font));

                // MONTO TOTAL
                finalInfoTable.addCell(createFinalInfoCell("MONTO TOTAL", true, boldFont));
                finalInfoTable.addCell(createFinalInfoCell("PESOS.-", false, font));
                finalInfoTable.addCell(createFinalInfoCell("", false, font));

                document.add(finalInfoTable);
                document.add(new Paragraph(" ").setFontSize(5));

                // --- 6. INFORMACIÓN FINAL ---
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
        private Cell createInfoCell(String content, boolean bold, int fontSize, PdfFont font) {
                Paragraph p = new Paragraph(content != null ? content : "").setFont(font).setFontSize(fontSize);
                return new Cell().setBorder(Border.NO_BORDER).add(p).setPadding(1);
        }

        private Cell createProductHeaderCell(String content, PdfFont font) {
                return new Cell()
                                .setBackgroundColor(new DeviceRgb(230, 230, 230))
                                .setBorder(Border.NO_BORDER)
                                .setBorderBottom(new SolidBorder(ColorConstants.BLACK, 1))
                                .add(new Paragraph(content).setFont(font).setFontSize(8))
                                .setTextAlignment(TextAlignment.CENTER)
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