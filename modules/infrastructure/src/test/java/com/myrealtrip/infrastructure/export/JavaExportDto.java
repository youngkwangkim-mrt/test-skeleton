package com.myrealtrip.infrastructure.export;

import com.myrealtrip.infrastructure.export.annotation.ExportColumn;
import com.myrealtrip.infrastructure.export.annotation.ExportSheet;

@ExportSheet(name = "자바DTO", includeIndex = false)
public class JavaExportDto {

    @ExportColumn(header = "이름", order = 1)
    private String name;

    @ExportColumn(header = "나이", order = 2)
    private int age;

    public JavaExportDto(String name, int age) {
        this.name = name;
        this.age = age;
    }

    public String getName() {
        return name;
    }

    public int getAge() {
        return age;
    }
}
