package www.zigdeal.shop.apiBatch.batch;

import lombok.RequiredArgsConstructor;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.JobBuilderFactory;
import org.springframework.batch.core.configuration.annotation.StepBuilderFactory;
import org.springframework.batch.item.ItemReader;
import org.springframework.batch.item.data.MongoItemWriter;
import org.springframework.batch.item.data.builder.MongoItemWriterBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.core.MongoTemplate;
import www.zigdeal.shop.apiBatch.batch.readers.ExchangeRateReader;
import www.zigdeal.shop.apiBatch.domain.ExchangeRate;

@RequiredArgsConstructor
@Configuration
public class ExchangeRateConfiguration {

    private final JobBuilderFactory jobBuilderFactory;
    private final StepBuilderFactory stepBuilderFactory;

    @Bean
    public Job exchangeRateJob(Step exchangeRateStep) {
        return jobBuilderFactory.get("exchangeRateJob")
                .start(exchangeRateStep)
                .build();
    }


    @Bean
    public Step exchangeRateStep(MongoItemWriter<ExchangeRate> exchangeRateMongoItemWriter) {
        return stepBuilderFactory.get("exchangeRateStep")
                .<ExchangeRate, ExchangeRate>chunk(1)
                .reader(exchangeRateItemReader())
                .writer(exchangeRateMongoItemWriter)
                .build();
    }

    @Bean
    public ItemReader<ExchangeRate> exchangeRateItemReader() {
        return new ExchangeRateReader();
    }

    @Bean
    public MongoItemWriter<ExchangeRate> exchangeRateMongoItemWriter(MongoTemplate mongoTemplate) {
        return new MongoItemWriterBuilder<ExchangeRate>()
                .template(mongoTemplate)
                .collection("exchangeRate")
                .build();
    }


}
