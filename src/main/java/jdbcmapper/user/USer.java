package jdbcmapper.user;

import jdbcmapper.orm_na_kolenkax.aanotations.Column;
import jdbcmapper.orm_na_kolenkax.aanotations.Id;
import jdbcmapper.orm_na_kolenkax.aanotations.Table;

import java.time.LocalDate;

@Table(name = "users")
public class USer {
    @Id()
    @Column()
    private Long id;

    @Column()
    private String name;

    @Column()
    private LocalDate birthday;

    public USer() {}

    public USer(String name, LocalDate birthday) {
        this.name = name;
        this.birthday = birthday;
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public LocalDate getBirthday() {
        return birthday;
    }


    @Override
    public String toString() {
        return "User{id=" + id + ", name='" + name + "', birthdate=" + birthday + "}";
    }
}