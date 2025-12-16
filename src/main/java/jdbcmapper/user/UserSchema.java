package jdbcmapper.user;

import jdbcmapper.db.Db;

public final class UserSchema {
    private UserSchema() {}

    public static void create(Db db) {
        String ddl = """
                create table if not exists users (
                id integer primary key autoincrement,
                name text not null,
                birthdate text not null,
                )
                """;
        db.update(ddl);
    }
}
