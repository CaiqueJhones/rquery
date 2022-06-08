package br.com.caiquejh.rquery;

import br.com.caiquejh.rquery.exception.RQueryException;
import org.apache.commons.lang3.ClassUtils;

import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

import static java.util.Objects.requireNonNull;

class ValueConverter {

    private static final Map<Class<?>, Function<String, ?>> CONVERTERS = new ConcurrentHashMap<>();
    private static final Map<ClassField, Class<?>> CLASS_FIELD_TYPE = new ConcurrentHashMap<>();

    private static final ThreadLocal<DateFormat> DATE_FORMAT =
            ThreadLocal.withInitial(() -> {
                TimeZone tz = TimeZone.getTimeZone(ZoneId.systemDefault());
                DateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                df.setTimeZone(tz);
                return df;
            });
    private static final DateTimeFormatter DATE_TIME_FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    static {
        register(String.class, Function.identity());
        register(UUID.class, UUID::fromString);

        register(LocalDate.class, LocalDate::parse);
        register(LocalTime.class, LocalTime::parse);
        register(LocalDateTime.class, source -> LocalDateTime.parse(source, DATE_TIME_FORMATTER));
        register(ZonedDateTime.class, source -> LocalDateTime.parse(source, DATE_TIME_FORMATTER).atZone(ZoneId.systemDefault()));
        register(OffsetDateTime.class, source -> LocalDateTime.parse(source, DATE_TIME_FORMATTER).atOffset(OffsetDateTime.now().getOffset()));
        register(Instant.class, source -> LocalDateTime.parse(source, DATE_TIME_FORMATTER).atZone(ZoneId.systemDefault()).toInstant());

        register(Date.class, source -> {
            try {
                return DATE_FORMAT.get().parse(source);
            } catch (ParseException e) {
                throw new RQueryException(e.getMessage(), e);
            }
        });

        register(int.class, Integer::parseInt);
        register(Integer.class, Integer::valueOf);
        register(short.class, Short::parseShort);
        register(Short.class, Short::valueOf);
        register(long.class, Long::parseLong);
        register(Long.class, Long::valueOf);

        register(float.class, Float::parseFloat);
        register(Float.class, Float::valueOf);
        register(double.class, Double::parseDouble);
        register(Double.class, Double::valueOf);

        register(BigInteger.class, BigInteger::new);
        register(BigDecimal.class, BigDecimal::new);
    }
    
    public static <T> void register(Class<T> classOfT, Function<String, T> converter) {
        CONVERTERS.put(classOfT, converter);
    }

    public static Object convert(Class<?> fromType, String fieldName, String value) {
        ClassField classField = new ClassField(fromType, fieldName);
        if (CLASS_FIELD_TYPE.containsKey(classField)) {
            return convert(CLASS_FIELD_TYPE.get(classField), value);
        }
        putAllTypeFromClass(fromType);
        Class<?> typeOfField = requireNonNull(CLASS_FIELD_TYPE.get(classField));
        return convert(typeOfField, value);
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static Object convert(Class<?> fieldType, String value) {
        try {
            if (fieldType.isEnum()) {
                return Enum.valueOf((Class) fieldType, value);
            }
            return CONVERTERS.get(fieldType).apply(value);
        } catch (RQueryException rqe) {
            throw rqe;
        } catch (Exception ex) {
            String message = "Cannot convert to " + fieldType.getSimpleName() + " - ";
            throw new RQueryException(message + ex.getMessage(), ex);
        }
    }

    private static void putAllTypeFromClass(Class<?> fromClass) {
        for (Class<?> clazz : ClassUtils.hierarchy(fromClass)) {
            for (Field field : clazz.getDeclaredFields()) {
                CLASS_FIELD_TYPE.put(new ClassField(fromClass, field.getName()), field.getType());
            }
        }
    }

    private static class ClassField {
        final Class<?> type;
        final String fieldName;

        public ClassField(Class<?> type, String fieldName) {
            this.type = type;
            this.fieldName = fieldName;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            ClassField that = (ClassField) o;
            return Objects.equals(type, that.type) && Objects.equals(fieldName, that.fieldName);
        }

        @Override
        public int hashCode() {
            return Objects.hash(type, fieldName);
        }

        @Override
        public String toString() {
            return "ClassField{" +
                    "type=" + type +
                    ", fieldName='" + fieldName + '\'' +
                    '}';
        }
    }

}
