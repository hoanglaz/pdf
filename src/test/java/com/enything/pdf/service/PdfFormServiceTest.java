package com.enything.pdf.service;

import com.enything.pdf.dto.FieldConfigDto;
import com.enything.pdf.dto.PdfTemplateDto;
import com.enything.pdf.model.FieldType;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.interactive.form.PDAcroForm;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class PdfFormServiceTest {

    private final PdfFormService pdfFormService = new PdfFormService();

    @Test
    void makeFillable_shouldAddFieldsAndAnnotations() throws Exception {
        byte[] sourcePdf = createBlankPdf();
        PdfTemplateDto template = buildTemplate();

        byte[] output = pdfFormService.makeFillable(sourcePdf, template);

        try (PDDocument doc = Loader.loadPDF(output)) {
            PDAcroForm acroForm = doc.getDocumentCatalog().getAcroForm();
            assertNotNull(acroForm, "AcroForm should be present in output PDF");
            assertEquals(2, acroForm.getFields().size(), "Expected two root fields in output");
            assertNotNull(acroForm.getField("customerName"), "Text field should exist");
            assertNotNull(acroForm.getField("acceptedTerms"), "Checkbox field should exist");

            PDPage page0 = doc.getPage(0);
            assertEquals(2, page0.getAnnotations().size(), "Expected two widget annotations on page 0");
        }
    }

    private byte[] createBlankPdf() throws Exception {
        try (PDDocument doc = new PDDocument();
             ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            doc.addPage(new PDPage(PDRectangle.A4));
            doc.save(baos);
            return baos.toByteArray();
        }
    }

    private PdfTemplateDto buildTemplate() {
        FieldConfigDto text = new FieldConfigDto();
        text.setName("customerName");
        text.setType(FieldType.TEXT);
        text.setPage(0);
        text.setX(120);
        text.setY(650);
        text.setWidth(200);
        text.setHeight(24);
        text.setMultiline(false);
        text.setDefaultValue("customer demo");

        FieldConfigDto checkbox = new FieldConfigDto();
        checkbox.setName("acceptedTerms");
        checkbox.setType(FieldType.CHECKBOX);
        checkbox.setPage(0);
        checkbox.setX(120);
        checkbox.setY(600);
        checkbox.setWidth(18);
        checkbox.setHeight(18);
        checkbox.setDefaultValue("true");

        PdfTemplateDto template = new PdfTemplateDto();
        template.setTemplateName("customer-contract");
        template.setVersion(1);
        template.setFields(List.of(text, checkbox));
        return template;
    }
}