package dk.itu.minitwit;


import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.sql.SQLException;


@SpringBootApplication(scanBasePackages={"dk.itu.minitwit.database","dk.itu.minitwit.controller"})
public class MiniTwitApplication {


    public static void main(final String[] args) throws SQLException {
        SpringApplication.run(MiniTwitApplication.class, args);
    }

}
