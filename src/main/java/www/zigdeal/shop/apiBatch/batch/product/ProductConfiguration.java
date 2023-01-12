package www.zigdeal.shop.apiBatch.batch.product;

import lombok.RequiredArgsConstructor;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.JobBuilderFactory;
import org.springframework.batch.core.configuration.annotation.StepBuilderFactory;
import org.springframework.batch.item.ItemReader;
import org.springframework.batch.item.data.MongoItemReader;
import org.springframework.batch.item.data.MongoItemWriter;
import org.springframework.batch.item.data.builder.MongoItemWriterBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import www.zigdeal.shop.apiBatch.batch.exchangeRate.domain.ExchangeRate;
import www.zigdeal.shop.apiBatch.batch.exchangeRate.readers.ExchangeRateReader;
import www.zigdeal.shop.apiBatch.batch.product.domain.Product;
import www.zigdeal.shop.apiBatch.batch.product.readers.AliExpressReader;

import java.util.HashMap;
import java.util.Map;

@RequiredArgsConstructor
@Configuration
public class ProductConfiguration {
    private final JobBuilderFactory jobBuilderFactory;
    private final StepBuilderFactory stepBuilderFactory;

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
                .reader(AliExpressItemReader())
                .writer(productMongoItemWriter())
                .build();
    }

    @Bean
    public ItemReader<Product> AliExpressItemReader() {
        return new AliExpressReader();
    }

//    @Bean
//    public MongoItemReader<Product> productMongoItemReader() {
//        MongoItemReader<Product> mongoItemReader = new MongoItemReader<>();
//        mongoItemReader.setTemplate(mongoTemplate);
//        mongoItemReader.setCollection("products");
//        mongoItemReader.setTargetType(Product.class);
//        mongoItemReader.setQuery("{}");
//        Map<String, Sort.Direction> sort = new HashMap<String, Sort.Direction>(1);
//        sort.put("_id", Sort.Direction.ASC);
//        mongoItemReader.setSort(sort);
//        return mongoItemReader;
//    }

    @Bean
    public MongoItemWriter<Product> productMongoItemWriter() {
        return new MongoItemWriterBuilder<Product>().template(mongoTemplate).collection("productBatchTest").build();
    }
}
