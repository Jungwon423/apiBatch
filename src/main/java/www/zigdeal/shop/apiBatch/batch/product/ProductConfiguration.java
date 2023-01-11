package www.zigdeal.shop.apiBatch.batch.product;

import lombok.RequiredArgsConstructor;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.JobBuilderFactory;
import org.springframework.batch.core.configuration.annotation.StepBuilderFactory;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.batch.item.data.MongoItemReader;
import org.springframework.batch.item.data.MongoItemWriter;
import org.springframework.batch.item.data.builder.MongoItemWriterBuilder;
import org.springframework.batch.item.support.CompositeItemProcessor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import www.zigdeal.shop.apiBatch.batch.product.domain.Product;
import www.zigdeal.shop.apiBatch.batch.product.service.PriceComparisonService;
import www.zigdeal.shop.apiBatch.batch.product.service.TranslateService;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RequiredArgsConstructor
@Configuration
public class ProductConfiguration {
    private final JobBuilderFactory jobBuilderFactory;
    private final StepBuilderFactory stepBuilderFactory;
    private final TranslateService translateService;
    private final PriceComparisonService priceComparisonService;

    @Autowired
    private MongoTemplate mongoTemplate;

    @Bean
    public Job AliExpressJob() {
        return jobBuilderFactory.get("AliExpressJob")
                .start(ProductStep())
                .build();
    }

    @Bean
    public Step ProductStep() {
        return stepBuilderFactory.get("CollectProductStep")
                .<Product, Product>chunk(10)
                .reader(productMongoItemReader())
                .processor(compositeItemProcessor())
                .writer(productMongoItemWriter())
                .build();
    }

    @Bean
    public MongoItemReader<Product> productMongoItemReader() {
        MongoItemReader<Product> mongoItemReader = new MongoItemReader<>();
        mongoItemReader.setTemplate(mongoTemplate);
        mongoItemReader.setCollection("products");
        mongoItemReader.setTargetType(Product.class);
        mongoItemReader.setQuery("{}");
        Map<String, Sort.Direction> sort = new HashMap<>(1);
        sort.put("_id", Sort.Direction.ASC);
        mongoItemReader.setSort(sort);
        return mongoItemReader;
    }

    @Bean
    public CompositeItemProcessor compositeItemProcessor() {
        List<ItemProcessor> delagates = new ArrayList<>();
        delagates.add(translateProcessor());
        delagates.add(priceComparisonProcessor());

        CompositeItemProcessor processor = new CompositeItemProcessor<>();

        processor.setDelegates(delagates);

        return processor;
    }

    @Bean
    public ItemProcessor<Product, Product> translateProcessor() {
        return translateService::translateProduct;
    }

    @Bean
    public ItemProcessor<Product, Product> priceComparisonProcessor() {
        return priceComparisonService::comparePrice;
    }

    @Bean
    public MongoItemWriter<Product> productMongoItemWriter() {
        return new MongoItemWriterBuilder<Product>().template(mongoTemplate).collection("productBatchTest").build();
    }
}
