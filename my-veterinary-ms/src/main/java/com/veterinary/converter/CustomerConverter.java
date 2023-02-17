package com.veterinary.converter;

import com.veterinary.dto.CustomerDto;
import com.veterinary.model.Customer;
import lombok.Data;
import org.springframework.core.convert.converter.Converter;

import java.util.Objects;

@Data(staticConstructor = "newInstance")
public class CustomerConverter implements Converter<Customer, CustomerDto> {

    @Override
    public CustomerDto convert(Customer customer) {
        if (Objects.isNull(customer)) {
            return null;
        }
        return CustomerDto.builder()
                .id(customer.getId())
                .firstname(customer.getFirstname())
                .lastname(customer.getLastname())
                .idNumber(customer.getIdNumber())
                .mail(customer.getMail())
                .phoneNumber(customer.getPhoneNumber())
                .build();
    }
}
