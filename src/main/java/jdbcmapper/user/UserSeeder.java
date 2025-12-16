package jdbcmapper.user;

import java.time.LocalDate;
import java.util.List;

public final class UserSeeder {
    private UserSeeder() {}

    public static void seedIfEmty(UserRepository repo) {
        if(repo.count() > 0) {
            return;
        }

        repo.insertBatch(List.of(
                new USer(0, "leonid", LocalDate.of(2004, 11, 19)),
                new USer(0, "ahad", LocalDate.of(2005, 9, 17))
        ));
    }
}
