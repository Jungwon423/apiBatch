package www.zigdeal.shop.apiBatch.batch.readers;

import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;
import org.springframework.batch.item.ItemReader;
import www.zigdeal.shop.apiBatch.domain.ExchangeRate;

import java.io.IOException;

@RequiredArgsConstructor
public class ExchangeRateReader implements ItemReader<ExchangeRate> {

    @Override
    public ExchangeRate read() {
        ExchangeRate exchangeRate = new ExchangeRate();
        crawling(exchangeRate);
        return exchangeRate;
    }

    public void crawling(ExchangeRate exchangeRate){
        final String crawlingUrl = "https://search.naver.com/search.naver?sm=tab_hty.top&where=nexearch&query=%EB%8B%AC%EB%9F%AC+%ED%99%98%EC%9C%A8&oquery=%ED%99%98%EC%9C%A8&tqi=hI8yWsp0YihssdfcMcZssssssgN-226712";
        Connection conn = Jsoup.connect(crawlingUrl);

        try {
            Document doc = conn.get();
            Elements elements = doc.getElementsByClass("spt_con up");
//            Elements elements = doc.getElementsByClass("spt_con dw");
            Elements ele = elements.get(0).getElementsByTag("strong");

            Double todayExchangeRate = elementToDouble(ele);
            exchangeRate.setName("오늘의 환율");
            exchangeRate.setExchangeRate(todayExchangeRate);

            System.out.println(exchangeRate.toString());

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public Double elementToDouble(Elements ele){
        String text = ele.text();
        String regexText = text.replaceAll(",", "");
        return Double.parseDouble(regexText);
    }
}
