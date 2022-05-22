RQuery
======

[![Build Status](https://github.com/CaiqueJhones/rquery/workflows/CI/badge.svg)](https://github.com/CaiqueJhones/rquery/actions)
[![License](https://img.shields.io/badge/License-Apache_2.0-blue.svg)](https://opensource.org/licenses/Apache-2.0)

RQuery is a simple query language on top of Criteria Query.

## Usage

Assuming we have a json representation of a person with the following attributes:

```json
{
    "id": 100,
    "firstName": "Jon Snow",
    "lastName": "Stark",
    "email": "amacmeekan11@businesswire.com",
    "age": 79,
    "address": {
        "city": "North",
        "street": "Village Green",
        "number": "67",
        "postalCode": "3304"
    },
    "gender": "MALE"
}
```

To find the records by the firstName and the city of the address object, we can do,

```java
public class MyService {
    @PersistenceContext
    private EntityManager em;

    public List<Person> findPersons() {
        CriteriaBuilder cb = em.getCriteriaBuilder();
        CriteriaQuery<Person> query = builder.createQuery(Person.class);
        Root<Person> root = query.from(Person.class);

        String query = "firstName contains 'Jon' && address.city in ('New York, 'North')";
        Predicate predicate = RQuery.from(root, cb).parse(query);
        return em.createQuery(query.where(predicate)).getResultList();
    }
}
```

or using Spring Specification,

```java
public class MyService {
    @Autowired
    private PersonRepository repository;

    public List<Person> findPersons() {
        String query = "firstName contains 'Jon' && address.city in ('New York, 'North')";
        Specification<Person> filterSpecification = (root, query, criteriaBuilder) ->
                RQuery.from(root, criteriaBuilder).parse(filter);
        return repository.findAll(filterSpecification);
    }
}
```

## Reference

### Literals

| Literal  | Symbol                | Example               |
|----------|-----------------------|-----------------------|
| Number   | `(0..9)+ (.0..9)*`    | `123`, `1.099`        |
| String   | `''` or `""`          | `'Jon'`, `"Jon"`      |
| Date     | `yyyy-MM-dd`          | `2022-05-22`          |
| Time     | `HH:mm:ss`            | `12:00:00`            |
| DateTime | `yyyy-MM-dd HH:mm:ss` | `2022-05-22 12:00:00` |
| Enum     | `'UPPER_CASE_NAME'`   | `NAME`                |

### Operators

| Operators  | Symbols                               | Example                        |
|------------|---------------------------------------|--------------------------------|
| Relational | `=`, `<`, `<=`, `>`, `>=`, `!=`, `<>` | `name = 'Jon'`                 |
| Between    | `between ? and ?`                     | `age between 18 and 60`        |
| Booleans   | `is true`, `is false`                 | `hasName is true`              |
| Nullables  | `is null`, `is not null`              | `name is not null`             |
| Strings    | `starts`, `contains`                  | `name contains 'Jon'`          |
| List       | `in(?,...)`, `not in(?,...)`          | `gender in ('MALE', 'FEMALE')` |
| Logical    | `&&`, <code>&#124;&#124;</code>       | `age > 10 && age < 100`        |

### Entities relationship

Relationships can be accessed using a dot, including multivalued.
For example, assuming we have a json representation of a person with the following attributes,

```json
{
    "firstName": "Jon Snow",
    "address": {
        "city": "North",
        "street": "Village Green",
        "number": "67",
        "postalCode": "3304"
    },
    "children": [
      {
        "firstName": "Nobody"
      }
    ]
}
```

We can access the city field of the address as follows: `address.city = 'North'` and `children.firstName is not null`.

### Supported Types

| Type                         |
|------------------------------|
| java primitives and wrappers |
| `java.util.UUID`             |
| `java.util.Date`             |
| `java.time.LocalDate`        |
| `java.time.LocalTime`        |
| `java.time.LocalDateTime`    |
| `java.time.ZonedDateTime`    |
| `java.time.OffsetDateTime`   |
| `java.time.Instant`          |
| `java.math.BigInteger`       |
| `java.math.BigDecimal`       |