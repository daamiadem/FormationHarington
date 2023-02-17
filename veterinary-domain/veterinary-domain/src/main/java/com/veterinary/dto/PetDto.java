package com.veterinary.dto;


import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PetDto {

    private Long id;
    private String name;
    private Float weight;
    private String category;
    private Date entryDate;
    private CustomerDto customerDto;
    private List<TreatmentDto> treatments;
}
