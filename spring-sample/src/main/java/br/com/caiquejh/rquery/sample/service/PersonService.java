package br.com.caiquejh.rquery.sample.service;

import br.com.caiquejh.rquery.RQuery;
import br.com.caiquejh.rquery.sample.model.Person;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

@RequiredArgsConstructor
@Service
public class PersonService {

    private final PersonRepository repository;

    public Page<Person> listAllPageable(String filter, Pageable pageable) {
        if (StringUtils.isBlank(filter)) {
            return repository.findAll(pageable);
        }

        Specification<Person> filterSpecification = (root, query, criteriaBuilder) ->
                RQuery.from(root, criteriaBuilder).parse(filter);
        return repository.findAll(filterSpecification, pageable);
    }
}
