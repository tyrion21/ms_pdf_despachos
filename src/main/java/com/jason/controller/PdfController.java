package com.jason.controller;

import com.jason.service.PdfGenerationService;
import com.jason.repository.FeDteEnvWsRepository;
import com.jason.model.FeDteEnvWs;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestParam;

import java.io.IOException;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;

@RestController
@RequestMapping("/api/pdf")
public class PdfController {

    private final PdfGenerationService pdfService;
    private final FeDteEnvWsRepository repository;

    public PdfController(PdfGenerationService pdfService, FeDteEnvWsRepository repository) {
        this.pdfService = pdfService;
        this.repository = repository;
    }

    @GetMapping("/dispatch-guide/{codEmp}/{tipoDoc}/{caf}")
    public ResponseEntity<byte[]> getDispatchGuidePdf(
            @PathVariable String codEmp,
            @PathVariable Integer tipoDoc,
            @PathVariable Float caf) {
        try {
            byte[] pdfBytes = pdfService.generateDispatchGuidePdf(codEmp, tipoDoc, caf);
            // Guardar/overwrite en disco
            try {
                String baseFileName = "guia_despacho_" + codEmp + "_" + tipoDoc + "_" + (int)caf.floatValue();
                String fileName = baseFileName + ".pdf"; // nombre que puede cambiar si existe
                java.nio.file.Path outDir = java.nio.file.Paths.get("generated-pdf");
                java.nio.file.Files.createDirectories(outDir);
                java.nio.file.Path outFile = outDir.resolve(fileName);
                int counter = 1;
                while (java.nio.file.Files.exists(outFile)) {
                    fileName = baseFileName + " (" + counter + ").pdf";
                    outFile = outDir.resolve(fileName);
                    counter++;
                }
                java.nio.file.Files.write(outFile, pdfBytes, java.nio.file.StandardOpenOption.CREATE_NEW);
                System.out.println("PDF guardado en: " + outFile.toAbsolutePath());
                // Ajustar header con el nombre final versionado
                HttpHeaders headers = new HttpHeaders();
                headers.setContentType(MediaType.APPLICATION_PDF);
                headers.setContentDispositionFormData("filename", fileName);
                headers.setCacheControl("must-revalidate, post-check=0, pre-check=0");
                System.out.println("Nombre de descarga: " + fileName);
                System.out.println("Generando PDF para CAF: " + caf);
                return new ResponseEntity<>(pdfBytes, headers, HttpStatus.OK);
            } catch (Exception storeEx) {
                System.err.println("Advertencia: no se pudo almacenar copia local del PDF: " + storeEx.getMessage());
            }
            // Si falló el almacenamiento, aún devolvemos el PDF con nombre base
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_PDF);
            String fallbackName = "guia_despacho_" + codEmp + "_" + tipoDoc + "_" + (int) caf.floatValue() + ".pdf";
            headers.setContentDispositionFormData("filename", fallbackName);
            headers.setCacheControl("must-revalidate, post-check=0, pre-check=0");
            System.out.println("Generando PDF (sin copia local) para CAF: " + caf);
            return new ResponseEntity<>(pdfBytes, headers, HttpStatus.OK);
        } catch (RuntimeException e) {
            // Error si no se encuentran los datos
            System.err.println("Error al obtener datos: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(null); // 404
        } catch (IOException e) {
            // Error al generar el PDF
            e.printStackTrace();
            System.err.println("Error al generar el PDF: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null); // 500
        } catch (Exception e) {
            // Otros errores inesperados
            e.printStackTrace();
            System.err.println("Error inesperado: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null); // 500
        }
    }

    /**
     * Endpoint para listar todas las guías disponibles para una empresa y tipo de documento
     */
    @GetMapping("/dispatch-guides/list")
    public ResponseEntity<List<Map<String, Object>>> listDispatchGuides(
            @RequestParam String codEmp,
            @RequestParam Integer tipoDoc) {
        try {
            List<FeDteEnvWs> guias = repository.findAll(); // Aquí deberías filtrar por codEmp y tipoDoc
            List<Map<String, Object>> response = new ArrayList<>();
            
            for (FeDteEnvWs guia : guias) {
                if (guia.getId().getCodEmp().equals(codEmp) && 
                    guia.getId().getCodTipodoc().equals(tipoDoc)) {
                    
                    Map<String, Object> guiaInfo = new HashMap<>();
                    guiaInfo.put("codEmp", guia.getId().getCodEmp());
                    guiaInfo.put("caf", guia.getId().getCaf());
                    guiaInfo.put("rutCliente", guia.getRutCliente());
                    guiaInfo.put("razonSocial", guia.getRazonSocialCliente());
                    guiaInfo.put("fechaEmision", guia.getFechaEmision());
                    guiaInfo.put("total", guia.getTotal());
                    guiaInfo.put("npdf", guia.getNpdf());
                    guiaInfo.put("eenv", guia.getEenv());
                    guiaInfo.put("pdfUrl", "/api/pdf/dispatch-guide/" + codEmp + "/" + tipoDoc + "/" + guia.getId().getCaf());
                    
                    response.add(guiaInfo);
                }
            }
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            System.err.println("Error al listar guías: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
        }
    }

    /**
     * Endpoint para listar todas las guías disponibles (para el frontend)
     * Filtra las guías con sys_origen = "FRUSYSFRPK-FP" o "FRUSYSFRPK-TI"
     */
    @GetMapping("/dispatch-guides/all")
    public ResponseEntity<List<Map<String, Object>>> listAllDispatchGuides() {
        try {
            List<FeDteEnvWs> guias = repository.findAll();
            List<Map<String, Object>> response = new ArrayList<>();
            
            // Log para debugging
            System.out.println("=== DEBUGGING FILTROS ===");
            System.out.println("Total guías encontradas: " + guias.size());
            
            int countFP = 0, countTI = 0, countOther = 0;
            for (FeDteEnvWs guia : guias) {
                String sysOrigen = guia.getSysOrigen();
                if ("FRUSYSFRPK-FP".equals(sysOrigen)) {
                    countFP++;
                } else if ("FRUSYSFRPK-TI".equals(sysOrigen)) {
                    countTI++;
                } else {
                    countOther++;
                }
                
                // Filtrar guías con sys_origen válidos para ambos tipos
                if ("FRUSYSFRPK-FP".equals(sysOrigen) || "FRUSYSFRPK-TI".equals(sysOrigen)) {
                    Map<String, Object> guiaInfo = new HashMap<>();
                    guiaInfo.put("codEmp", guia.getId().getCodEmp());
                    guiaInfo.put("tipoDoc", guia.getId().getCodTipodoc());
                    guiaInfo.put("caf", guia.getId().getCaf());
                    guiaInfo.put("rutCliente", guia.getRutCliente());
                    guiaInfo.put("razonSocial", guia.getRazonSocialCliente());
                    guiaInfo.put("fechaEmision", guia.getFechaEmision());
                    guiaInfo.put("total", guia.getTotal());
                    guiaInfo.put("npdf", guia.getNpdf());
                    guiaInfo.put("eenv", guia.getEenv());
                    guiaInfo.put("sysOrigen", guia.getSysOrigen());
                    guiaInfo.put("pdfUrl", "/api/pdf/dispatch-guide/" + guia.getId().getCodEmp() + "/" + guia.getId().getCodTipodoc() + "/" + guia.getId().getCaf());
                    
                    response.add(guiaInfo);
                }
            }
            
            System.out.println("Conteo por sys_origen:");
            System.out.println("FRUSYSFRPK-FP: " + countFP);
            System.out.println("FRUSYSFRPK-TI: " + countTI);
            System.out.println("Otros: " + countOther);
            System.out.println("Guías devueltas: " + response.size());
            System.out.println("=== FIN DEBUGGING ===");
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            System.err.println("Error al listar todas las guías: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
        }
    }

    /**
     * Endpoint para obtener información de una guía específica sin generar el PDF
     */
    @GetMapping("/dispatch-guide/info/{codEmp}/{tipoDoc}/{caf}")
    public ResponseEntity<Map<String, Object>> getDispatchGuideInfo(
            @PathVariable String codEmp,
            @PathVariable Integer tipoDoc,
            @PathVariable Float caf) {
        try {
            com.jason.model.FeDteEnvWsId id = new com.jason.model.FeDteEnvWsId(codEmp, tipoDoc, caf);
            var guiaOpt = repository.findById(id);
            
            if (guiaOpt.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(null);
            }
            
            FeDteEnvWs guia = guiaOpt.get();
            Map<String, Object> info = new HashMap<>();
            info.put("caf", caf);
            info.put("codEmp", codEmp);
            info.put("tipoDoc", tipoDoc);
            info.put("rutEmpresa", guia.getRutEmpresa() + "-" + guia.getDvEmpresa());
            info.put("rutCliente", guia.getRutCliente());
            info.put("razonSocial", guia.getRazonSocialCliente());
            info.put("fechaEmision", guia.getFechaEmision());
            info.put("total", guia.getTotal());
            info.put("neto", guia.getNeto());
            info.put("iva", guia.getIvap());
            
            return ResponseEntity.ok(info);
        } catch (Exception e) {
            System.err.println("Error al obtener información: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
        }
    }
}