package dk.itu.minitwit;


import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.sql.SQLException;


@SpringBootApplication(scanBasePackages={"dk.itu.minitwit.database","dk.itu.minitwit.controller"}, exclude = {SecurityAutoConfiguration.class} )
public class MiniTwitApplication {


    public static void main(final String[] args) throws SQLException {
        SpringApplication.run(MiniTwitApplication.class, args);
    }

    @Bean
    public BCryptPasswordEncoder bCryptPasswordEncoder() {
        return new BCryptPasswordEncoder();
    }

}
