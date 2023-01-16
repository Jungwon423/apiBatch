package www.zigdeal.shop.apiBatch.batch.Amazon;

import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.JobBuilderFactory;
import org.springframework.batch.core.configuration.annotation.StepBuilderFactory;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.batch.item.ItemReader;
import org.springframework.batch.item.data.MongoItemWriter;
import org.springframework.batch.item.data.builder.MongoItemWriterBuilder;
import org.springframework.batch.item.support.CompositeItemProcessor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.core.MongoTemplate;
import www.zigdeal.shop.apiBatch.batch.Product;
import www.zigdeal.shop.apiBatch.service.PriceComparisonService;
import www.zigdeal.shop.apiBatch.service.TranslateService;

import java.util.ArrayList;
import java.util.List;

@Configuration
@RequiredArgsConstructor
public class AmazonConfiguration {
    public static final Logger logger = LoggerFactory.getLogger("AmazonLogger");
    private final JobBuilderFactory jobBuilderFactory;
    private final StepBuilderFactory stepBuilderFactory;
    private final TranslateService translateService;
    private final PriceComparisonService priceComparisonService;

    @Autowired
    private MongoTemplate mongoTemplate;

    @Bean
    public Job AmazonJob() {
        return jobBuilderFactory.get("AmazonJob")
                .listener(AmazonJobListener())
                .start(AmazonStep())
                .build();
    }

    @Bean
    public Step AmazonStep(){
        logger.info("Amazon Step 시작");
        return stepBuilderFactory.get("AmazonStep")
                .<Product, Product>chunk(10)
                .reader(AmazonItemReader())
                .processor(compositeItemProcessor())
                .writer(productMongoItemWriter())
                .listener(AmazonChunkListener())
                .build();
    }
    @Bean
    public AmazonJobListener AmazonJobListener(){
        return new AmazonJobListener();
    }
    @Bean
    public AmazonChunkListener AmazonChunkListener(){
        return new AmazonChunkListener();
    }
    @Bean
    public ItemReader<Product> AmazonItemReader() {
        return new AmazonReader();
    }

    public CompositeItemProcessor compositeItemProcessor() {
        List<ItemProcessor> delagates = new ArrayList<>();
        delagates.add(validateProcessor());
        delagates.add(translateProcessor());
        delagates.add(priceComparisonProcessor());

        CompositeItemProcessor processor = new CompositeItemProcessor<>();

        processor.setDelegates(delagates);

        return processor;
    }

    public ItemProcessor<Product, Product> validateProcessor() {
        return product -> {
            if (product.getPrice() < 0) return null;
            else return product;
        };
    }
    public ItemProcessor<Product, Product> translateProcessor() {
        return translateService::translateProduct;
    }

    public ItemProcessor<Product, Product> priceComparisonProcessor() {
        return priceComparisonService::comparePrice;
    }

    public MongoItemWriter<Product> productMongoItemWriter() {
        return new MongoItemWriterBuilder<Product>().template(mongoTemplate).collection("productBatchTest").build();
    }
}
