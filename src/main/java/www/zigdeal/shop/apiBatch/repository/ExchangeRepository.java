package www.zigdeal.shop.apiBatch.repository;

import org.springframework.data.mongodb.repository.MongoRepository;
import www.zigdeal.shop.apiBatch.domain.ExchangeRate;

import java.util.List;

public interface ExchangeRepository extends MongoRepository<ExchangeRate, String> {
    List<ExchangeRate> findByName(String name);
}
