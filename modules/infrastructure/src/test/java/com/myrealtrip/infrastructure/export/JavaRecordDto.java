package com.myrealtrip.infrastructure.export;

import com.myrealtrip.infrastructure.export.annotation.ExportColumn;
import com.myrealtrip.infrastructure.export.annotation.ExportSheet;

@ExportSheet(name = "자바레코드", includeIndex = false)
public record JavaRecordDto(
    @ExportColumn(header = "상품명", order = 1)
    String productName,

    @ExportColumn(header = "가격", order = 2, format = "#,##0")
    int price,

    @ExportColumn(header = "재고", order = 3)
    int stock,

    @ExportColumn(header = "판매중", order = 4)
    boolean onSale
) {
}
