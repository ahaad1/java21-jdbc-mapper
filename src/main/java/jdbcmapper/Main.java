package jdbcmapper;

import jdbcmapper.db.Db;
import jdbcmapper.user.UserRepository;
import jdbcmapper.user.UserSchema;
import jdbcmapper.user.UserSeeder;

import java.nio.file.Path;

public class Main {
    public static void main(String[] args) {
        Path dbPath = Path.of("app.db");
        String url = "jdbc:sqlite:" + dbPath.toAbsolutePath().toString();

        Db db = new Db(url);
        UserSchema.create(db);

        UserRepository repo = new UserRepository(db);
        UserSeeder.seedIfEmty(repo);

        repo.findAll().forEach(System.out::println);
    }
}