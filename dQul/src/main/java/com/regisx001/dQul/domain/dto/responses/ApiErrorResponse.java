package com.regisx001.dQul.domain.dto.responses;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ApiErrorResponse {

    private int status;
    private String message;
}
