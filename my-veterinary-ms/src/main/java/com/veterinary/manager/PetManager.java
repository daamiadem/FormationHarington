package com.veterinary.manager;

import com.veterinary.converter.PetConverter;
import com.veterinary.converter.PetDtoConverter;
import com.veterinary.dto.PetDto;
import com.veterinary.model.Pet;
import com.veterinary.repository.PetRepository;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.CrossOrigin;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Component
@AllArgsConstructor
@CrossOrigin
public class PetManager {

    private final PetRepository petRepository;

    public List<PetDto> findAll() {
        return petRepository.findAll().stream()
                .map(pet -> PetConverter.newInstance().convert(pet))
                .collect(Collectors.toList());
    }

    public List <String> PetSup3 (){
        return  petRepository.findAll().stream()
                .filter(p ->  p.getWeight()>3)
                .map(p -> p.getName())
                .collect(Collectors.toList());

    }



    public PetDto findById(Long id) {
        return PetConverter.newInstance().convert(petRepository.findById(id).orElse(null));

    }

    public PetDto AddPet(PetDto petdto) {
        Pet pet = PetDtoConverter.newInstance().convert(petdto);
        Pet save = petRepository.save(pet);
        return PetConverter.newInstance().convert(save);
    }

    public void deletePet(Long id) {
        petRepository.deleteById(id);
    }

    public void deletePet(PetDto petDto) {
        Pet Pet = PetDtoConverter.newInstance().convert(petDto);
        petRepository.delete(Pet);

    }

    public PetDto updatePet(PetDto petnew, Long id) {
        Pet pet = PetDtoConverter.newInstance().convert(petnew);
        Pet oldPet = petRepository.findById(id).orElseThrow();
        oldPet.setCategory(pet.getCategory());
        oldPet.setCustomer(pet.getCustomer());
        oldPet.setEntryDate(pet.getEntryDate());
        oldPet.setName(pet.getName());
        oldPet.setWeight(pet.getWeight());
        oldPet.setTreatments(pet.getTreatments());

        petRepository.save(oldPet);
        return PetConverter.newInstance().convert(oldPet);
    }


    public String CreateOrUpate (PetDto NewPet){

        if(findById(NewPet.getId())==null ){
            NewPet.setId(null);
            AddPet(NewPet);
            return "Create new pet" ;
        }
        updatePet(NewPet, NewPet.getId());
        return  "update pet" ;

    }

}
