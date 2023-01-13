package www.zigdeal.shop.apiBatch.batch.exchangeRate;

import  lombok.RequiredArgsConstructor;
import org.springframework.batch.item.ItemReader;
import org.json.simple.*;
import org.json.simple.parser.*;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.*;

@RequiredArgsConstructor
public class ExchangeRateReader implements ItemReader<ExchangeRate> {

    private int cnt = 0;

    @Override
    public ExchangeRate read() throws ParseException {
        cnt++;
        ExchangeRate exchangeRate = new ExchangeRate();
        if (cnt <= 1) {
            String responseBody = APIReader();
            List<Object> objectList = responseStrToList(responseBody);

            for (Object object : objectList) {
                JSONObject jsonObject = (JSONObject) object;
                if (!jsonObject.get("cur_unit").equals("USD")) {
                } else {
                    exchangeRate.setExchangeRate(Double.valueOf(((String) jsonObject.get("deal_bas_r")).replaceAll(",", "")));
                    exchangeRate.setName("USD");
                }
            }

            System.out.println("Itemreader running");
            System.out.println(exchangeRate);
            System.out.println(cnt);
        }
        return cnt <= 1 ? exchangeRate : null;
    }

    private List<Object> responseStrToList(String responseBody) throws ParseException {
        List<Object> objectList = new ArrayList<>();
        JSONParser parser = new JSONParser();
        JSONArray jsonArray = (JSONArray) parser.parse(responseBody);

        if (jsonArray != null) {
            objectList.addAll(jsonArray);
        }
        return objectList;
    }

    public String APIReader() {
        JSONParser parser = new JSONParser();
//        String AuthKey = "e4lofYKs4QdnWUW6eAoSHTAzJkNncSGf";
        String AuthKey = "b0MB3AsOJ5wqkU3q1RiDzZjDfJiEVZbn";
        String SearchDate = "20230103";
        String dataType = "AP01";
        String apiURL = "https://www.koreaexim.go.kr/site/program/financial/exchangeJSON?authkey=" + AuthKey + "&searchdate=" + SearchDate + "&data=" + dataType;
        Map<String, String> requestHeaders = new HashMap<>();
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
}
