package www.zigdeal.shop.apiBatch.service;

import lombok.RequiredArgsConstructor;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.springframework.stereotype.Service;
import www.zigdeal.shop.apiBatch.batch.ExchangeRepository;
import www.zigdeal.shop.apiBatch.batch.Product;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.*;

@Service
@RequiredArgsConstructor
public class PriceComparisonService {

    private final ExchangeRepository exchangeRepository;

    public Product comparePrice(Product product) throws ParseException {
        String productName = product.getName();

        product = CalculateKRWPrice(product);

        // Price filtering
        Double productPrice = product.getPrice();
        Double productTax = product.getDirect_tax();
        Double productShip = product.getDirect_shippingFee();
        String responseBody = SearchAPI(productName);
        List<?> objectList = responseStrToList(responseBody);

        Double naverPrice = 0.0;
        String category ="";
        String category2= "";
        if (objectList.isEmpty()){
            naverPrice = 0.0;
        }
        for (Object li : objectList) {
            naverPrice = ObjectToDouble(li);
            category = getCategory(li);
            category2 = getCategory2(li);
        }



        if (productPrice + productTax + productShip> naverPrice) return null;
        product.setCategoryName(category);
        product.setCategoryName2(category2);
        product.setNaverPrice(naverPrice);

        return product;
    }

    public Product CalculateKRWPrice (Product product) {
        Double productPrice = product.getPrice();
        String currency = product.getCurrency();
        Double productShip = product.getDirect_shippingFee();
        Double productTax = product.getDirect_tax();
        if (currency.equals("KRW")) return product;
        else {
            Double USD = 0.0;
            if (exchangeRepository.findById("USD").isPresent()) USD = exchangeRepository.findById("USD").get().getExchangeRate();
            product.setCurrency("KRW");
            product.setPrice(productPrice*USD);
            if (product.getMarketName().equals("Amazon")) {
                product.setDirect_shippingFee(productShip * USD);
                product.setDirect_tax(productTax * USD);
            }
            return product;
        }
    }

    public String SearchAPI(String searchText){
        String clientId = "KhJZhJGtfYYqxV5srTNB"; //애플리케이션 클라이언트 아이디
        String clientSecret = "VKYxFd5kWN"; //애플리케이션 클라이언트 시크릿

        String text;
        text = URLEncoder.encode(searchText, StandardCharsets.UTF_8);  // 이 부분을 변경해야함

        String apiURL = "https://openapi.naver.com/v1/search/shop?query=" + text + "&display=5&sort=asc";    // JSON 결과 (shop 결과)

        Map<String, String> requestHeaders = new HashMap<>();
        requestHeaders.put("X-Naver-Client-Id", clientId);
        requestHeaders.put("X-Naver-Client-Secret", clientSecret);
        return get(apiURL, requestHeaders);
    }

    private String get(String apiUrl, Map<String, String> requestHeaders) {
        HttpURLConnection con = connect(apiUrl);
        try {
            con.setRequestMethod("GET");
            for (Map.Entry<String, String> header : requestHeaders.entrySet()) {
                con.setRequestProperty(header.getKey(), header.getValue());
            }
            int responseCode = con.getResponseCode();
            if (responseCode == HttpURLConnection.HTTP_OK) { // 정상 호출
                return readBody(con.getInputStream());
            } else { // 오류 발생
                return readBody(con.getErrorStream());
            }
        } catch (IOException e) {
            throw new RuntimeException("API 요청과 응답 실패", e);
        } finally {
            con.disconnect();
        }
    }

    private HttpURLConnection connect(String apiUrl) {
        try {
            URL url = new URL(apiUrl);
            return (HttpURLConnection) url.openConnection();
        } catch (MalformedURLException e) {
            throw new RuntimeException("API URL이 잘못되었습니다. : " + apiUrl, e);
        } catch (IOException e) {
            throw new RuntimeException("연결이 실패했습니다. : " + apiUrl, e);
        }
    }

    private String readBody(InputStream body) {
        InputStreamReader streamReader = new InputStreamReader(body);
        try (BufferedReader lineReader = new BufferedReader(streamReader)) {
            StringBuilder responseBody = new StringBuilder();
            String line;
            while ((line = lineReader.readLine()) != null) {
                responseBody.append(line);
            }
            return responseBody.toString();
        } catch (IOException e) {
            throw new RuntimeException("API 응답을 읽는 데 실패했습니다.", e);
        }
    }

    private List<?> responseStrToList(String responseBody) throws ParseException {
        List<?> objectList = new ArrayList<>();
        JSONParser parser = new JSONParser();
        JSONObject JsonBodyFirstLevel = (JSONObject) parser.parse(responseBody);
        Object obj1 = JsonBodyFirstLevel.get("items");
        if (obj1.getClass().isArray()) {
            objectList = Arrays.asList((Object[]) obj1);
        } else if (obj1 instanceof Collection) {
            objectList = new ArrayList<>((Collection<?>) obj1);
        }
        return objectList;
    }

    private Double ObjectToDouble(Object rawObj){
        JSONObject JsonLi = (JSONObject) rawObj;
        Object objLi = JsonLi.get("lprice");
        return Double.parseDouble((String) objLi);
    }

    private String getCategory(Object rawObj){
        JSONObject JsonLi = (JSONObject) rawObj;
        Object objLi = JsonLi.get("category1");
        return (String)objLi;
    }

    private String getCategory2(Object rawObj){
        JSONObject JsonLi = (JSONObject) rawObj;
        Object objLi = JsonLi.get("category2");
        return (String)objLi;
    }
}