package www.zigdeal.shop.apiBatch.batch.eBay;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.listener.JobExecutionListenerSupport;
import org.springframework.stereotype.Component;

@Component
public class eBayJobListener extends JobExecutionListenerSupport {

    private static final Logger log = LoggerFactory.getLogger("eBayLogger");

    @Override
    public void beforeJob(JobExecution jobExecution) {
        if(jobExecution.getStatus() == BatchStatus.STARTED) {
            log.info("eBayJob start! ");
        }
    }

    @Override
    public void afterJob(JobExecution jobExecution) {
        if(jobExecution.getStatus() == BatchStatus.COMPLETED) {
            log.info("eBayJob successed! ");
        } else if (jobExecution.getStatus() == BatchStatus.FAILED) {
            log.info("eBayJob failed! ");
        }
    }
}
