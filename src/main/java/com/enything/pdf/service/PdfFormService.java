package com.enything.pdf.service;

import com.enything.pdf.dto.FieldConfigDto;
import com.enything.pdf.dto.PdfTemplateDto;
import com.enything.pdf.exception.BadRequestException;
import com.enything.pdf.exception.PdfProcessingException;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.cos.COSName;
import org.apache.pdfbox.pdmodel.graphics.color.PDColor;
import org.apache.pdfbox.pdmodel.graphics.color.PDDeviceRGB;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDResources;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.interactive.annotation.PDBorderStyleDictionary;
import org.apache.pdfbox.pdmodel.interactive.annotation.PDAnnotationWidget;
import org.apache.pdfbox.pdmodel.interactive.form.PDAcroForm;
import org.apache.pdfbox.pdmodel.interactive.form.PDCheckBox;
import org.apache.pdfbox.pdmodel.interactive.form.PDTextField;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;

@Service
public class PdfFormService {

    private static final Logger log = LoggerFactory.getLogger(PdfFormService.class);

    public byte[] makeFillable(byte[] originalPdf, PdfTemplateDto template) {
        try (PDDocument document = Loader.loadPDF(originalPdf);
             ByteArrayOutputStream baos = new ByteArrayOutputStream()) {

            if (template == null || template.getFields() == null || template.getFields().isEmpty()) {
                throw new BadRequestException("Template fields must not be empty");
            }

            PDAcroForm acroForm = new PDAcroForm(document);
            document.getDocumentCatalog().setAcroForm(acroForm);
            acroForm.setNeedAppearances(false);
            ensureDefaultAppearance(acroForm);

            List<FieldConfigDto> fields = template.getFields();
            int addedCount = 0;
            for (FieldConfigDto field : fields) {
                validatePage(document, field.getPage());

                switch (field.getType()) {
                    case TEXT -> {
                        addTextField(document, acroForm, field);
                        addedCount++;
                    }
                    case CHECKBOX -> {
                        addCheckboxField(document, acroForm, field);
                        addedCount++;
                    }
                    default -> throw new BadRequestException("Unsupported field type: " + field.getType());
                }
            }

            // Build field appearances server-side so common PDF viewers render values reliably.
            acroForm.refreshAppearances();

            log.info("PDF fillable conversion completed. Added {} fields. AcroForm now has {} root fields.",
                addedCount,
                acroForm.getFields().size());

            document.save(baos);
            return baos.toByteArray();
        } catch (BadRequestException ex) {
            throw ex;
        } catch (IOException ex) {
            throw new PdfProcessingException("Failed to create fillable PDF", ex);
        }
    }

    private void validatePage(PDDocument document, int pageIndex) {
        if (pageIndex < 0 || pageIndex >= document.getNumberOfPages()) {
            throw new BadRequestException("Invalid page index: " + pageIndex);
        }
    }

    private void addTextField(PDDocument document, PDAcroForm acroForm, FieldConfigDto cfg) throws IOException {
        PDPage page = document.getPage(cfg.getPage());
        warnIfOutOfPage(cfg, page);

        PDTextField textField = new PDTextField(acroForm);
        textField.setPartialName(cfg.getName());
        textField.setDefaultAppearance("/Helv 10 Tf 0 g");

        if (cfg.isMultiline()) {
            textField.setMultiline(true);
        }

        PDAnnotationWidget widget = textField.getWidgets().get(0);
        configureWidget(widget, page, cfg);
        styleWidget(widget);

        page.getAnnotations().add(widget);
        acroForm.getFields().add(textField);

        if (cfg.getDefaultValue() != null) {
            textField.setValue(cfg.getDefaultValue());
        }
    }

    private void ensureDefaultAppearance(PDAcroForm acroForm) {
        if (acroForm.getDefaultResources() == null) {
            acroForm.setDefaultResources(new PDResources());
        }

        acroForm.getDefaultResources().put(
            COSName.getPDFName("Helv"),
            new PDType1Font(Standard14Fonts.FontName.HELVETICA)
        );

        if (acroForm.getDefaultAppearance() == null || acroForm.getDefaultAppearance().isBlank()) {
            acroForm.setDefaultAppearance("/Helv 10 Tf 0 g");
        }
    }

    private void addCheckboxField(PDDocument document, PDAcroForm acroForm, FieldConfigDto cfg) throws IOException {
        PDPage page = document.getPage(cfg.getPage());
        warnIfOutOfPage(cfg, page);

        PDCheckBox checkBox = new PDCheckBox(acroForm);
        checkBox.setPartialName(cfg.getName());

        PDAnnotationWidget widget = checkBox.getWidgets().get(0);
        configureWidget(widget, page, cfg);
        styleWidget(widget);

        page.getAnnotations().add(widget);
        acroForm.getFields().add(checkBox);

        if ("true".equalsIgnoreCase(cfg.getDefaultValue())) {
            try {
                checkBox.check();
            } catch (IOException ignored) {
            }
        }
    }

    private void styleWidget(PDAnnotationWidget widget) {
        PDBorderStyleDictionary border = new PDBorderStyleDictionary();
        border.setWidth(1);
        widget.setBorderStyle(border);
        widget.setColor(new PDColor(new float[]{0f, 0f, 0f}, PDDeviceRGB.INSTANCE));
    }

    private void configureWidget(PDAnnotationWidget widget, PDPage page, FieldConfigDto cfg) {
        PDRectangle rect = new PDRectangle(cfg.getX(), cfg.getY(), cfg.getWidth(), cfg.getHeight());
        widget.setRectangle(rect);
        widget.setPage(page);
        widget.setPrinted(true);
    }

    private void warnIfOutOfPage(FieldConfigDto cfg, PDPage page) {
        PDRectangle box = page.getCropBox() != null ? page.getCropBox() : page.getMediaBox();
        float pageWidth = box.getWidth();
        float pageHeight = box.getHeight();
        boolean outside = cfg.getX() < 0
            || cfg.getY() < 0
            || cfg.getWidth() <= 0
            || cfg.getHeight() <= 0
            || cfg.getX() + cfg.getWidth() > pageWidth
            || cfg.getY() + cfg.getHeight() > pageHeight;

        if (outside) {
            log.warn("Field '{}' may be outside visible page area. page={} rect=[x={}, y={}, w={}, h={}] pageSize=[w={}, h={}]",
                cfg.getName(),
                cfg.getPage(),
                cfg.getX(),
                cfg.getY(),
                cfg.getWidth(),
                cfg.getHeight(),
                pageWidth,
                pageHeight);
        }
    }
}
