package com.example.WaffleBear.file.model;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

@Builder
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class FileInfoDto {

    public static class FileReq{
        @Schema(description = "이것은 물이로다.")
        private String name;

    }

    public static class FileRes{

        private String temp;
    }


}
