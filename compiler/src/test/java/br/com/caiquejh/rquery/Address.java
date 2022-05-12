package br.com.caiquejh.rquery;

import javax.persistence.Embeddable;
import java.util.Objects;

@Embeddable
public class Address {

    private String street;
    private String number;
    private Boolean isApartment;

    public Address() {
    }

    public Address(String street, String number, Boolean isApartment) {
        this.street = street;
        this.number = number;
        this.isApartment = isApartment;
    }

    public String getStreet() {
        return street;
    }

    public void setStreet(String street) {
        this.street = street;
    }

    public String getNumber() {
        return number;
    }

    public void setNumber(String number) {
        this.number = number;
    }

    public Boolean getApartment() {
        return isApartment;
    }

    public void setApartment(Boolean apartment) {
        isApartment = apartment;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Address address = (Address) o;
        return Objects.equals(street, address.street) && Objects.equals(number, address.number) && Objects.equals(isApartment, address.isApartment);
    }

    @Override
    public int hashCode() {
        return Objects.hash(street, number, isApartment);
    }

    @Override
    public String toString() {
        return "Address{" +
                "street='" + street + '\'' +
                ", number='" + number + '\'' +
                ", isApartment=" + isApartment +
                '}';
    }
}
