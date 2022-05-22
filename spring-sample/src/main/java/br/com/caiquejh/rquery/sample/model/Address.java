package br.com.caiquejh.rquery.sample.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.persistence.Embeddable;
import javax.validation.constraints.NotBlank;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Embeddable
public class Address {

    @NotBlank
    private String city;
    @NotBlank
    private String street;
    @NotBlank
    private String number;
    private String postalCode;
}
