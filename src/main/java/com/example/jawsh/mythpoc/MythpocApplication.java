package com.example.jawsh.mythpoc;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;
import org.springframework.retry.RetryContext;
import org.springframework.retry.RetryStatistics;
import org.springframework.retry.annotation.*;
import org.springframework.retry.stats.DefaultStatisticsRepository;
import org.springframework.retry.stats.StatisticsListener;
import org.springframework.retry.stats.StatisticsRepository;
import org.springframework.retry.support.RetrySynchronizationManager;
import org.springframework.stereotype.Component;

@SpringBootApplication
@EnableRetry
public class MythpocApplication {


	public static void main(String[] args) {
		SpringApplication.run(MythpocApplication.class, args);
	}

	@Bean
	public ApplicationRunner runner(Foo foo) {
		return args -> {
			try {
				foo.exec();
			}
			catch (Exception e) {
				try {
					foo.exec();
				}
				catch (Exception ee) {
					Thread.sleep(11000);
					try {
						foo.exec();
					}
					catch (Exception eee) {

					}
				}
			}
		};
	}

	@Component
	public static class Foo {

		private static final Logger LOGGER = LoggerFactory.getLogger(Foo.class);

		private final Bar bar;

		private RetryContext status;

		@Autowired
		private StatisticsRepository repository;

		public Foo(Bar bar) {
			this.bar = bar;
		}

		@CircuitBreaker(label="test", maxAttempts = 1, openTimeout = 10000, resetTimeoutExpression="#{${retry.timeout}}")
		public void exec() throws Exception {
			status = RetrySynchronizationManager.getContext();

			LOGGER.info("CircuitOpen: " + this.status.getAttribute("circuit.open"));
			LOGGER.info("Foo.circuit");
			this.bar.retryWhenException();


		}

		@Recover
		public void recover(Throwable t) throws Throwable {
			status = RetrySynchronizationManager.getContext();

			LOGGER.info("CircuitOpen: " + this.status.getAttribute("circuit.open"));
			LOGGER.info("Foo.recover");
			throw t;
		}

	}

	@Component
	public static class Bar {

		private static final Logger LOGGER = LoggerFactory.getLogger(Bar.class);

		@Retryable(value = { Exception.class }, maxAttemptsExpression = "${retry.maxAttempts}", backoff = @Backoff(2000))
		public void retryWhenException() throws Exception {
			LOGGER.info("Retrying");
			throw new Exception();
		}

		@Recover
		public void recover(Throwable t) throws Throwable {
			LOGGER.info("Bar.recover");
			throw t;
		}

	}

	@Configuration
	protected static class FunConfig {
		@Bean
		public StatisticsRepository repository() {
			return new DefaultStatisticsRepository();
		}

		@Bean
		public StatisticsListener listener(StatisticsRepository repository) {
			return new StatisticsListener(repository);
		}
	}
}
