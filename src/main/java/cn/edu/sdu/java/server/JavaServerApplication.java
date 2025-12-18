package cn.edu.sdu.java.server;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.transaction.annotation.EnableTransactionManagement;

@SpringBootApplication
@EnableTransactionManagement
public class JavaServerApplication {

	public static void main(String[] args) {
		SpringApplication.run(JavaServerApplication.class, args);
	}

}
