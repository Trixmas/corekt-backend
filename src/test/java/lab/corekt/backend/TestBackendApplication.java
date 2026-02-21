package lab.corekt.backend;

import org.springframework.boot.SpringApplication;

public class TestBackendApplication {
    static void main(String[] args) {
        SpringApplication.from(BackendApplication::main).with(TestcontainersConfiguration.class).run(args);
    }
}
