package unidue.ub.rssfeeder;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.netflix.eureka.EnableEurekaClient;
import org.springframework.cloud.openfeign.EnableFeignClients;

@SpringBootApplication
@EnableEurekaClient
@EnableFeignClients
public class RssfeederApplication {

	public static void main(String[] args) {
		SpringApplication.run(RssfeederApplication.class, args);
	}
}
