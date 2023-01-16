
package www.zigdeal.shop.apiBatch.batch.eBay;

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
import www.zigdeal.shop.apiBatch.batch.Amazon.AmazonJobListener;
import www.zigdeal.shop.apiBatch.batch.Product;
import www.zigdeal.shop.apiBatch.service.PriceComparisonService;
import www.zigdeal.shop.apiBatch.service.TranslateService;

import java.util.ArrayList;
import java.util.List;

@RequiredArgsConstructor
@Configuration
public class eBayConfiguration {
    public static final Logger logger = LoggerFactory.getLogger("eBayLogger");
    private final JobBuilderFactory jobBuilderFactory;
    private final StepBuilderFactory stepBuilderFactory;
    private final TranslateService translateService;
    private final PriceComparisonService priceComparisonService;

    @Autowired
    private MongoTemplate mongoTemplate;

    @Bean
    public Job eBayJob() {
        return jobBuilderFactory.get("eBayJob")
                .listener(new eBayJobListener())
                .start(eBayStep())
                .build();
    }

    @Bean
    public Step eBayStep(){
        return stepBuilderFactory.get("eBayStep")
                .<Product, Product>chunk(10)
                .reader(eBayItemReader())
                .processor(compositeItemProcessor())
                .writer(productMongoItemWriter())
                .build();
    }

    @Bean
    public ItemReader<Product> eBayItemReader() {
        return new eBayReader();
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

