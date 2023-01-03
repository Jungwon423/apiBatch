package www.zigdeal.shop.apiBatch.batch.readers;

import lombok.RequiredArgsConstructor;
import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;
import org.springframework.batch.item.ItemReader;
import www.zigdeal.shop.apiBatch.domain.ExchangeRate;
import org.json.simple.*;
import org.json.simple.parser.*;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;

@RequiredArgsConstructor
public class ExchangeRateReader implements ItemReader<ExchangeRate> {

    @Override
    public ExchangeRate read() {
        ExchangeRate exchangeRate = new ExchangeRate();
        crawling(exchangeRate);
//        APIReader(exchangeRate);
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
//            System.out.println(exchangeRate.toString());

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public Double elementToDouble(Elements ele){
        String text = ele.text();
        String regexText = text.replaceAll(",", "");
        return Double.parseDouble(regexText);
    }

    public Double ObjectToDouble(Object object){
        String StringRate = (String) object;
        String regexText = StringRate.replaceAll(",", "");
        return Double.parseDouble(regexText);
    }

    public void APIReader(ExchangeRate exchangeRate) {
        JSONParser parser = new JSONParser();
        String AuthKey = "e4lofYKs4QdnWUW6eAoSHTAzJkNncSGf";
        String SearchDate = "20230103";
        String dataType = "AP01";
        String apiURL = "https://www.koreaexim.go.kr/site/program/financial/exchangeJSON?authkey=" + AuthKey + "&searchdate=" + SearchDate + "&data=" + dataType;
        //한국수출입은행 환율
        try {
            URL oracle = new URL(apiURL);
            URLConnection yc = oracle.openConnection();
            BufferedReader in = new BufferedReader(new InputStreamReader(yc.getInputStream()));

            String inputLine;
            while ((inputLine = in.readLine()) != null) {
                JSONArray a = (JSONArray) parser.parse(inputLine);

                for (Object o : a) {
                    JSONObject JSONRate = (JSONObject) o;
                    //System.out.println((String) JSONRate.get("cur_nm"));
                    if (("미국 달러").equals(JSONRate.get("cur_nm"))){
                        Double todayExchangeRate = ObjectToDouble(JSONRate.get("deal_bas_r"));
                        exchangeRate.setName("오늘의 환율");
                        exchangeRate.setExchangeRate(todayExchangeRate);
//                    환율 api 데이터 예시에 대한 것은 핫딜 노션 환율 API 관련 참조
//                    System.out.println("bkpr>>" + (String) JSONRate.get("bkpr"));
                        System.out.println("deal_bas_r>>" + JSONRate.get("deal_bas_r"));  //매매 기준율 이거 쓰면 될듯 함
                    }
                }
            }
            in.close();
        } catch (MalformedURLException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (ParseException e) {
            e.printStackTrace();
        }
    }
}
