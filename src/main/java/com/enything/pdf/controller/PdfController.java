package com.enything.pdf.controller;

import com.enything.pdf.dto.PdfTemplateDto;
import com.enything.pdf.exception.BadRequestException;
import com.enything.pdf.service.PdfFormService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Valid;
import jakarta.validation.Validator;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@RestController
@RequestMapping("/api/pdf")
public class PdfController {

    private final PdfFormService pdfFormService;
    private final ObjectMapper objectMapper;
    private final Validator validator;

    public PdfController(PdfFormService pdfFormService, ObjectMapper objectMapper, Validator validator) {
        this.pdfFormService = pdfFormService;
        this.objectMapper = objectMapper;
        this.validator = validator;
    }

    @PostMapping(value = "/convert", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<byte[]> convertToFillable(
            @RequestPart("file") MultipartFile file,
            @RequestPart("template") String templateJson) {

        if (file == null || file.isEmpty()) {
            throw new BadRequestException("PDF file must not be empty");
        }

        PdfTemplateDto template;
        try {
            template = objectMapper.readValue(templateJson, PdfTemplateDto.class);
        } catch (JsonProcessingException ex) {
            throw new BadRequestException("Invalid template JSON", ex);
        }

        var violations = validator.validate(template);
        if (!violations.isEmpty()) {
            String message = violations.stream()
                .map(this::toMessage)
                .sorted()
                .findFirst()
                .orElse("Template validation failed");
            throw new BadRequestException(message);
        }

        byte[] output;
        try {
            output = pdfFormService.makeFillable(file.getBytes(), template);
        } catch (IOException ex) {
            throw new BadRequestException("Unable to read uploaded PDF file", ex);
        }

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_PDF);
        headers.setContentDisposition(ContentDisposition.attachment()
                .filename("fillable-output.pdf")
                .build());

        return ResponseEntity.ok()
                .headers(headers)
                .body(output);
    }

    private String toMessage(ConstraintViolation<PdfTemplateDto> violation) {
        return violation.getPropertyPath() + ": " + violation.getMessage();
    }

    @PostMapping("/template/export")
    public ResponseEntity<PdfTemplateDto> exportTemplate(@Valid @RequestBody PdfTemplateDto template) {
        return ResponseEntity.ok(template);
    }

    @GetMapping("/generate")
    public String generatePdf() {
        // Logic to generate PDF goes here
        return "PDF generated successfully!";
    }


}
