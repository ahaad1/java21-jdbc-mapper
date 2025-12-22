package jdbcmapper.orm_na_kolenkax;

import jdbcmapper.db.Db;
import jdbcmapper.orm_na_kolenkax.aanotations.Column;
import jdbcmapper.orm_na_kolenkax.aanotations.Id;
import jdbcmapper.orm_na_kolenkax.aanotations.Table;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public final class OrmNaKolenkax {
    private final Db db;

    public OrmNaKolenkax(Db db) {
        this.db = db;
    }

    public void createTableIfNotExists(Class<?> entityClass) {
        String table = tableName(entityClass);

        List<Field> fields = persistentFields(entityClass);

        if(fields.isEmpty()) {
            throw  new IllegalArgumentException("No persistent fields for " + entityClass);
        }

        String cols = fields.stream().map(this::columnDdl).collect(Collectors.joining(", \n "));

        String POCHEMU_NE_RABOTAEEEEET = "create table if not exists " + table + "( " + cols + " )";
        System.out.println(POCHEMU_NE_RABOTAEEEEET);
        db.update(POCHEMU_NE_RABOTAEEEEET);
    }

    public <T> long insert(T entity) {
        Class<?> cls = entity.getClass();
        String table = tableName(cls);

        List<Field> fields = persistentFields(cls);

        List<Field> insertFields = fields.stream()
                .filter(f -> !(isId(f) && idAutoIncrement(f)))
                .toList();

        String columns = insertFields.stream().map(this::columnName).collect(Collectors.joining(", "));
        String placeholders = insertFields.stream().map(f -> "?").collect(Collectors.joining(", "));

        String sql = "insert into " + table + " (" + columns + " ) values ( " + placeholders + " ) ";

        final long[] generatedId = { -1L };
        db.inTransaction(con -> {
            try (PreparedStatement ps = con.prepareStatement(sql)) {
                for (int i = 0; i < insertFields.size(); i++) {
                    Object v = readField(entity, insertFields.get(i));
                    ps.setObject(i + 1, toDbValue(v));
                }
                ps.executeUpdate();
            }

            try (Statement st = con.createStatement();
                 ResultSet rs = st.executeQuery("SELECT last_insert_rowid()")) {
                rs.next();
                generatedId[0] = rs.getLong(1);
            }

            Field idField = findIdField(cls).orElse(null);
            if (idField != null && idAutoIncrement(idField)) {
                writeField(entity, idField, generatedId[0]);
            }
        });

        return generatedId[0];
    }

    public <T> List<T> findAll(Class<T> entityClass) {
        String table = tableName(entityClass);
        String sql = "select * from " + table;

        return db.query(sql, rs -> mapRow(entityClass, rs));
    }

    public <T> Optional<T> findById(Class<T> entityClass, long id) {
        String table = tableName(entityClass);
        Field idField = findIdField(entityClass)
                .orElseThrow(() -> new IllegalArgumentException("no @Id field in " + entityClass));

        String idCol = columnName(idField);
        String sql = "select * from " + table + " where " + idCol + " = ? limit 1";

        List<T> rows = db.query(sql, rs -> mapRow(entityClass, rs), id);
        return rows.isEmpty() ? Optional.empty() : Optional.of(rows.getFirst());
    }



    private <T> T mapRow(Class<T> cls, ResultSet rs) throws SQLException {
        T obj = newInstance(cls);

        for (Field f : persistentFields(cls)) {
            String col = columnName(f);

            int idx;
            try {
                idx = rs.findColumn(col);
            } catch (SQLException e) {
                continue;
            }

            Object dbVal = rs.getObject(idx);
            Object javaVal = fromDbValue(f.getType(), dbVal);
            writeField(obj, f, javaVal);
        }

        return obj;
    }

    private Optional<Field> findIdField(Class<?> cls) {
        return Arrays.stream(cls.getDeclaredFields())
                .filter(f -> f.getAnnotation(Id.class) != null)
                .findFirst();
    }


    private boolean isId(Field f) {
        return f.getAnnotation(Id.class) != null;
    }

    private boolean idAutoIncrement(Field f) {
        Id id = f.getAnnotation(Id.class);
        return id != null && id.autoIncrement();
    }


    private String tableName(Class<?> cls) {
        Table t = cls.getAnnotation(Table.class);
        if(t != null && !t.name().isBlank()) {
            return t.name();
        }

        return toSnake(cls.getSimpleName());
    }

    private static Object toDbValue(Object v) {
        return switch (v) {
            case null -> null;
            case LocalDate d -> d.toString(); // ISO
            case Boolean b -> b ? 1 : 0;
            default -> v;
        };
    }

    private static Object fromDbValue(Class<?> targetType, Object dbVal) {
        if (dbVal == null) return null;

        if (targetType == String.class) return dbVal.toString();

        if (targetType == int.class || targetType == Integer.class) {
            return (dbVal instanceof Number n) ? n.intValue() : Integer.parseInt(dbVal.toString());
        }

        if (targetType == long.class || targetType == Long.class) {
            return (dbVal instanceof Number n) ? n.longValue() : Long.parseLong(dbVal.toString());
        }

        if (targetType == boolean.class || targetType == Boolean.class) {
            int x = (dbVal instanceof Number n) ? n.intValue() : Integer.parseInt(dbVal.toString());
            return x != 0;
        }

        if (targetType == LocalDate.class) {
            return LocalDate.parse(dbVal.toString());
        }

        throw new IllegalArgumentException("unsupported mapping " + targetType.getName());
    }

    private static <T> T newInstance(Class<T> cls) {
        try {
            return cls.getDeclaredConstructor().newInstance();
        } catch (Exception e) {
            throw new RuntimeException("entity must have no args constructor " + cls.getName(), e);
        }
    }

    private static Object readField(Object obj, Field f) {
        try {
            f.setAccessible(true);
            return f.get(obj);
        } catch (Exception e) {
            throw new RuntimeException("read field failed " + f.getName(), e);
        }
    }

    private static void writeField(Object obj, Field f, Object value) {
        try {
            f.setAccessible(true);

            if (value == null && f.getType().isPrimitive()) {
                return;
            }

            f.set(obj, value);
        } catch (Exception e) {
            throw new RuntimeException("write field failed " + f.getName(), e);
        }
    }





    private String columnName(Field f) {
        Column c = f.getAnnotation(Column.class);
        if (c != null && !c.name().isBlank()) return c.name();
        return toSnake(f.getName());
    }

    private List<Field> persistentFields(Class<?> cls) {
        return Arrays.stream(cls.getDeclaredFields())
                .filter(f -> !Modifier.isStatic(f.getModifiers()))
                .filter(f -> !Modifier.isTransient(f.getModifiers()))
                .filter(f -> f.getAnnotation(Column.class) != null || f.getAnnotation(Id.class) != null)
                .toList();
    }

    private String columnDdl(Field f) {
        String col = columnName(f);
        String type = sqliteType(f.getType());

        Column c = f.getAnnotation(Column.class);
        boolean nullable = c == null || c.nullable();

        if(isId(f)){
            if(idAutoIncrement(f)) {
                return col + " integer primary key autoincrement";
            }
            return col + " " + type + " primary key";
        }
        String nn = nullable ? "" : " not null";
        return col + " " + type + " " + nn;
    }

    private static String sqliteType(Class<?> jType) {
        if (jType == String.class) return "TEXT";
        if (jType == int.class || jType == Integer.class) return "INTEGER";
        if (jType == long.class || jType == Long.class) return "INTEGER";
        if (jType == boolean.class || jType == Boolean.class) return "INTEGER";
        if (jType == LocalDate.class) return "TEXT"; // ISO
        throw new IllegalArgumentException("Unsupported field type: " + jType.getName());

    }


    private static String toSnake(String s) {
        return s.replaceAll("([a-z0-9])([A-Z])", "$1_$2").toLowerCase();
    }


}
