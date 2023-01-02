package www.zigdeal.shop.apiBatch.batch.writers;

import org.springframework.batch.item.ItemWriter;
import www.zigdeal.shop.apiBatch.domain.ExchangeRate;

import java.util.List;

public class ExchangeRateWriter implements ItemWriter<ExchangeRate> {

    @Override
    public void write(List<? extends ExchangeRate> items) throws Exception {

    }
}
