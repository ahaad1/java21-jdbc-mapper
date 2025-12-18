package jdbcmapper.user;

import jdbcmapper.db.Db;

public final class UserSchema {
    private UserSchema() {}

    public static void create(Db db) {
        String ddl = """
    CREATE TABLE IF NOT EXISTS users (
      id        INTEGER PRIMARY KEY AUTOINCREMENT,
      name      TEXT NOT NULL,
      birthdate TEXT NOT NULL
    )
    """;
        db.update(ddl);

    }
}
