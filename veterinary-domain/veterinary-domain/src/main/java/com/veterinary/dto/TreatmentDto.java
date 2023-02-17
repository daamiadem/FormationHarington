package com.veterinary.dto;


import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TreatmentDto {

    private Long id;
    private Float price;
    private Date date;
    private String status;
    private String type;
}
