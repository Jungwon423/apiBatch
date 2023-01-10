package www.zigdeal.shop.apiBatch.batch.exchangeRate;

import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobExecutionException;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

public class Scheduler {
    @Component
    @RequiredArgsConstructor
    public class StoreStationScheduler {

        private final Logger logger = LoggerFactory.getLogger("file");

        private final Job job;
        private final JobLauncher jobLauncher;

        // 24시간 마다 실행
        @Scheduled(fixedDelay = 24 * 60 *60 * 1000L)
        public void executeJob () {
            try {
                logger.info("fetch gas station job start");
                jobLauncher.run(
                        job,
                        new JobParametersBuilder()
                                .addString("datetime", LocalDateTime.now().toString())
                                .toJobParameters()  // job parameter 설정
                );
                logger.info("successfully complete job\n\n");
            } catch (JobExecutionException ex) {
                System.out.println(ex.getMessage());
                ex.printStackTrace();
            }
        }
    }
}
