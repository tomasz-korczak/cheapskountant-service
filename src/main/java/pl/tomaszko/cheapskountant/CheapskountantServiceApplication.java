package pl.tomaszko.cheapskountant;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class CheapskountantServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(CheapskountantServiceApplication.class, args);
    }
}
