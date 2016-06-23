package ru.hse.securechat.server;

class DatabaseCredentials {
    static String connectionString = "jdbc:mysql://localhost/chat"+
            "?useUnicode=true&useJDBCCompliantTimezoneShift=true" +
            "&useLegacyDatetimeCode=false&serverTimezone=UTC&useSSL=false";
    static String JDBC_DRIVER = "com.mysql.cj.jdbc.Driver";
    static String USER_NAME = "chat";
    static String PASSWORD = "YwtMLkrzSPy3";
}
