package www.zigdeal.shop.apiBatch.batch;

import org.springframework.data.mongodb.repository.MongoRepository;
import www.zigdeal.shop.apiBatch.batch.exchangeRate.ExchangeRate;

public interface ExchangeRepository extends MongoRepository<ExchangeRate, String> {
}
