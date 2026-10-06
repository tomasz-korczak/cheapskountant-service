package pl.tomaszko.cheapskountant.config;

import org.hibernate.dialect.MariaDBDialect;

public class AppMariaDbDialect extends MariaDBDialect {

    @Override
    public int getDefaultTimestampPrecision() {
        return 3;
    }
}
