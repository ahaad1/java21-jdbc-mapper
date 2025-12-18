package jdbcmapper.user;

import jdbcmapper.db.Db;
import jdbcmapper.db.RowMapper;

import java.time.LocalDate;
import java.util.List;

public final class UserRepository {
    private final Db db;

    public UserRepository(final Db db) {
        this.db = db;
    }

    public static final RowMapper<USer> USER_MAPPEN = rs -> {
        long id = rs.getLong("id");
        String name = rs.getString("name");
        String birthday = rs.getString("birthdate");
        LocalDate birthDate = LocalDate.parse(birthday);

        return new USer(id, name, birthDate);
    };

    public long count() {
        return db.queryForLong("select count(*) from users");
    }

    public void insertBatch(List<USer> users) {
        db.inTransaction(con -> {
            String sql = "insert into users (name, birthdate) values (?, ?)";
            try (var ps = con.prepareStatement(sql)) {
                for (var u : users) {
                    ps.setString(1, u.name());
                    ps.setString(2, u.birthdate().toString());
                    ps.addBatch();
                }
                ps.executeBatch();
            }
        });
    }

    public List<USer> findAll() {
        return db.query("select * from users", USER_MAPPEN);
    }
}
