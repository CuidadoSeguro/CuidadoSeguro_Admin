package cl.duoc.msalapi;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@SpringBootApplication
@EntityScan("cl.duoc.msalapi.entity")
@EnableJpaRepositories("cl.duoc.msalapi.repository")
public class MsalApiApplication {

	public static void main(String[] args) {
		SpringApplication.run(MsalApiApplication.class, args);
	}
}
