package br.com.caiquejh.rquery.sample.controller;

import br.com.caiquejh.rquery.sample.model.Person;
import br.com.caiquejh.rquery.sample.service.PersonService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RequiredArgsConstructor
@RestController
@RequestMapping("/persons")
public class PersonController {

    private final PersonService service;

    @GetMapping
    public Page<Person> getAllPageable(
            @RequestParam(value = "filter", required = false) String filter,
            Pageable pageable) {
        return service.listAllPageable(filter, pageable);
    }
}
