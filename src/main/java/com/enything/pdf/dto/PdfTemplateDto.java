package com.enything.pdf.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Setter
@Getter
public class PdfTemplateDto {

    @NotBlank
    private String templateName;

    private Integer version = 1;

    @Valid
    @NotEmpty
    private List<FieldConfigDto> fields;

}
