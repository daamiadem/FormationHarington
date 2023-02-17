package com.veterinary.converter;

import org.springframework.core.convert.converter.Converter;
import com.veterinary.dto.PetDto;
import com.veterinary.model.Pet;
import lombok.Data;

import java.util.Objects;

@Data(staticConstructor = "newInstance")
public class PetDtoConverter implements Converter<PetDto, Pet> {

    @Override
    public Pet convert(PetDto petDto) {
        if (Objects.isNull(petDto)) {
            return null;
        }
        return Pet.builder()
                .id(petDto.getId())
                .name(petDto.getName())
                .weight(petDto.getWeight())
                .category(petDto.getCategory())
                .entryDate(petDto.getEntryDate())
                .build();
    }
}
