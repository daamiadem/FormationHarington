package com.veterinary.controller;

import com.veterinary.dto.PetDto;
import com.veterinary.manager.PetManager;
import com.veterinary.model.Pet;
import io.micrometer.core.annotation.Timed;
import io.swagger.annotations.ApiOperation;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

@Slf4j
@RestController
@RequestMapping("/api")
@Validated
@AllArgsConstructor
@Timed
public class PetController {

    private final PetManager petManager;


    @ApiOperation("get all pets")
    @Timed
    @GetMapping(produces = APPLICATION_JSON_VALUE, path = "/v1/pets")
    ResponseEntity<List<PetDto>> getAllPets() {
        List<PetDto> pets = petManager.findAll();
        return ResponseEntity.ok(pets);
    }
    @ApiOperation("get name pets weight sup 3")
    @Timed
    @GetMapping(produces = APPLICATION_JSON_VALUE, path = "/v1/pets/name")
    public List<String> getPetsName() {

        return petManager.PetSup3();
    }
    @ApiOperation("get pet by id")
    @Timed
    @GetMapping(produces = APPLICATION_JSON_VALUE, path = "/v1/pets/{id}")
    ResponseEntity<PetDto> getByPets(@PathVariable("id")long id) {
        PetDto pet = petManager.findById(id);
        return ResponseEntity.ok(pet);
    }

    @ApiOperation("Add pet")
    @Timed
    @PostMapping(produces = APPLICATION_JSON_VALUE, path = "/v1/pets/add")

    public PetDto AddPet(@RequestBody PetDto pet){

       return petManager.AddPet(pet);
    }

    @ApiOperation("Add or update pet")
    @Timed
    @PostMapping(produces = APPLICATION_JSON_VALUE, path = "/v1/pets/addOrUpdate")

    public String AddOrUpdatePet(@RequestBody PetDto pet){

        return petManager.CreateOrUpate(pet);
    }

    @ApiOperation("Delete pet")
    @Timed
    @DeleteMapping(produces = APPLICATION_JSON_VALUE, path = "/v1/pets/delete/{id}")
    ResponseEntity DeletePet (@PathVariable("id")long id ){
        petManager.deletePet(id);
        return ResponseEntity.ok(id);

    }

    @ApiOperation("Delete pet")
    @Timed
    @DeleteMapping(produces = APPLICATION_JSON_VALUE, path = "/v1/pets/delete")
    ResponseEntity DeletePet (PetDto petdto ){
        petManager.deletePet(petdto);
        return ResponseEntity.ok(petdto);

    }

    @ApiOperation("update pet")
    @Timed
    @PutMapping(produces = APPLICATION_JSON_VALUE, path = "/v1/pets/update/{id}")
    ResponseEntity UpdatePet (@RequestBody PetDto pet ,@PathVariable("id") long id){
        PetDto petupdated = petManager.updatePet(pet, id);
        return ResponseEntity.ok(petupdated);
    }




}
