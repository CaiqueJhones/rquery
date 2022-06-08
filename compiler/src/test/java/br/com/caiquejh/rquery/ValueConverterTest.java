package br.com.caiquejh.rquery;

import br.com.caiquejh.rquery.model.Gender;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.*;
import java.util.Date;
import java.util.UUID;

import static br.com.caiquejh.rquery.ValueConverter.convert;
import static org.junit.jupiter.api.Assertions.assertEquals;

class ValueConverterTest {

    @Test
    void shouldCovertNumbers() {
        String valueInt = "1011";
        String valueDecimal = "1.011";
        BigInteger expectedBigInteger = new BigInteger(valueInt);
        BigDecimal expectedBigDecimal = new BigDecimal(valueDecimal);

        assertEquals(expectedBigInteger, convert(Example.class, "integer", valueInt));
        assertEquals(expectedBigDecimal, convert(Example.class, "decimal", valueDecimal));
    }

    @Test
    void shouldConvertUUIDType() {
        UUID expectedUUID = UUID.randomUUID();
        assertEquals(expectedUUID, convert(Example.class, "uuid", expectedUUID.toString()));
    }

    @Test
    void shouldConvertDateTimeTypes() {
        String valueDateTime = "2022-05-21 15:00:00";
        String valueDate = "2022-05-21";
        String valueTime = "15:00:00";

        ZoneOffset offset = OffsetDateTime.now().getOffset();

        LocalDate expectedLocalDate = LocalDate.of(2022, 5, 21);
        LocalTime expectedLocalTime = LocalTime.of(15, 0);
        LocalDateTime expectedLocalDateTime = LocalDateTime.of(expectedLocalDate, expectedLocalTime);
        ZonedDateTime expectedZonedDateTime = expectedLocalDateTime.atZone(ZoneId.systemDefault());
        Instant expectedInstant = expectedZonedDateTime.toInstant();
        OffsetDateTime expectedOffsetDateTime = expectedLocalDateTime.atOffset(offset);
        Date expectedDate = Date.from(expectedInstant);

        assertEquals(expectedInstant, convert(Example.class, "instant", valueDateTime));
        assertEquals(expectedLocalDateTime, convert(Example.class, "localDateTime", valueDateTime));
        assertEquals(expectedZonedDateTime, convert(Example.class, "zonedDateTime", valueDateTime));
        assertEquals(expectedOffsetDateTime, convert(Example.class, "offsetDateTime", valueDateTime));
        assertEquals(expectedLocalDate, convert(Example.class, "localDate", valueDate));
        assertEquals(expectedLocalTime, convert(Example.class, "localTime", valueTime));
        assertEquals(expectedDate, convert(Example.class, "date", valueDateTime));
    }

    @Test
    void shouldConvertEnum() {
        String value = "MALE";
        Gender expectedGender = Gender.MALE;
        assertEquals(expectedGender, convert(Example.class, "gender", value));
    }

    private static class Example {
        Instant instant;
        LocalDateTime localDateTime;
        ZonedDateTime zonedDateTime;
        OffsetDateTime offsetDateTime;
        LocalDate localDate;
        LocalTime localTime;
        Date date;
        UUID uuid;
        BigInteger integer;
        BigDecimal decimal;
        Gender gender;
    }
}