package br.com.caiquejh.rquery.sample.service;

import br.com.caiquejh.rquery.sample.model.Person;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PersonRepository extends PagingAndSortingRepository<Person, Long>,
        JpaSpecificationExecutor<Person> {
}
