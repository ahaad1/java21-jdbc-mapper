package jdbcmapper;

import jdbcmapper.db.Db;
import jdbcmapper.orm_na_kolenkax.OrmNaKolenkax;
import jdbcmapper.user.USer;
import jdbcmapper.user.UserRepository;
import jdbcmapper.user.UserSchema;
import jdbcmapper.user.UserSeeder;

import java.nio.file.Path;
import java.time.LocalDate;
import java.util.List;

public class Main {
    public static void main(String[] args) {
        Path dbPath = Path.of("app.db");
        String url = "jdbc:sqlite:" + dbPath.toAbsolutePath().toString();

        Db db = new Db(url);
        OrmNaKolenkax orm = new OrmNaKolenkax(db);

        orm.createTableIfNotExists(USer.class);

        if(orm.findAll(USer.class).isEmpty()) {
            orm.insert(new USer("leonid", LocalDate.of(2004, 11, 19)));
            orm.insert(new USer("ahad", LocalDate.of(2005, 9, 17)));
        }


        List<USer> users = orm.findAll(USer.class);
        users.forEach(System.out::println);

        orm.findById(USer.class, 1).ifPresent(System.out::println);

//
//        UserSchema.create(db);
//
//        UserRepository repo = new UserRepository(db);
//        UserSeeder.seedIfEmty(repo);
//
//        repo.findAll().forEach(System.out::println);
    }
}