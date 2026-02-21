package custom.dialect;

import org.hibernate.dialect.MySQLInnoDBDialect;

public class FixedMySQLInnoDBDialect extends MySQLInnoDBDialect {

    @Override
    public String getTableTypeString() {
        // Forcer Hibernate à générer ENGINE=InnoDB au lieu de type=InnoDB
        return " ENGINE=InnoDB";
    }
}

