package www.zigdeal.shop.apiBatch.batch.Amazon;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.listener.JobExecutionListenerSupport;
import org.springframework.stereotype.Component;

@Component
public class AmazonJobListener extends JobExecutionListenerSupport {
    private static final Logger log = LoggerFactory.getLogger("AmazonLogger");

    @Override
    public void beforeJob(JobExecution jobExecution) {
        if(jobExecution.getStatus() == BatchStatus.STARTED) {
            log.info("AmazonJob start! ");
        }
    }

    @Override
    public void afterJob(JobExecution jobExecution) {
        if(jobExecution.getStatus() == BatchStatus.COMPLETED) {
            log.info("AmazonJob successed! ");
        } else if (jobExecution.getStatus() == BatchStatus.FAILED) {
            log.info("AmazonJob failed! ");
        }
    }
}
