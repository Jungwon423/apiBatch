package www.zigdeal.shop.apiBatch.batch.Amazon;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.ChunkListener;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.scope.context.StepContext;

public class AmazonChunkListener implements ChunkListener {
    private static final Logger logger = LoggerFactory.getLogger("AmazonLogger");

    @Override
    public void beforeChunk(ChunkContext context) {
        StepContext stepContext = context.getStepContext();
        StepExecution stepExecution = stepContext.getStepExecution();

        logger.info("#####  BeforeChunk : " + stepExecution.getReadCount());
    }

    @Override
    public void afterChunk(ChunkContext context) {
        StepContext stepContext = context.getStepContext();
        StepExecution stepExecution = stepContext.getStepExecution();

        logger.info("#####  Chunk 이후에 Commit된 갯수 : " + stepExecution.getCommitCount());
    }

    @Override
    public void afterChunkError(ChunkContext context) {
        StepContext stepContext = context.getStepContext();
        StepExecution stepExecution = stepContext.getStepExecution();

        logger.error("##### Chunk 이후 에러 발생 : " + stepExecution.getRollbackCount());
    }
}
