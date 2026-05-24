package com.enything.pdf.dto;

import com.enything.pdf.model.FieldType;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class FieldConfigDto {

    @NotBlank
    private String name;

    @NotNull
    private FieldType type;

    @Min(0)
    private int page;

    private float x;
    private float y;
    private float width;
    private float height;

    private boolean required;
    private boolean multiline;
    private String defaultValue;
}
