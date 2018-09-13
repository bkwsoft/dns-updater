package net.wachsmuths.dns;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.ApplicationPidFileWriter;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class DnsUpdaterApplication {

	public static void main(String[] args) {
        SpringApplication springApplication = new SpringApplication(DnsUpdaterApplication.class);
        springApplication.addListeners(new ApplicationPidFileWriter());     // register PID write to spring boot. It will write PID to file
        springApplication.run(args);	}
}
