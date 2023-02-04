package www.zigdeal.shop.apiBatch.batch.AliExpress;

import com.taobao.api.ApiException;
import com.taobao.api.DefaultTaobaoClient;
import com.taobao.api.TaobaoClient;
import com.taobao.api.request.AliexpressAffiliateHotproductQueryRequest;
import com.taobao.api.response.AliexpressAffiliateHotproductQueryResponse;
import org.openqa.selenium.By;
import org.openqa.selenium.PageLoadStrategy;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.item.ItemReader;
import www.zigdeal.shop.apiBatch.batch.Product;

import java.util.ArrayList;
import java.util.List;

public class AliExpressReader implements ItemReader<Product> {

    private int idx = 0;
    private WebDriver driver;
    public static String WEB_DRIVER_ID = "webdriver.chrome.driver";
    public static String WEB_DRIVER_PATH = "C:/chromedriver.exe";
    //    public static String WEB_DRIVER_PATH = "/home/ubuntu/Downloads/chromedriver";
    private final Logger logger = LoggerFactory.getLogger("AliLogger");
    private List<AliexpressAffiliateHotproductQueryResponse.Product> products = new ArrayList<>();

    public void create() throws ApiException { // 생성자로 links 초기화
        System.setProperty(WEB_DRIVER_ID, WEB_DRIVER_PATH);
        ChromeOptions options = new ChromeOptions();
        options.setPageLoadStrategy(PageLoadStrategy.EAGER);
        options.setCapability("ignoreProtectedModeSettings", true);
        options.addArguments("--disable-popup-blocking");       //팝업안띄움
        options.addArguments("headless");                       //브라우저 안띄움
        options.addArguments("__lang:euc-kr");
        driver = new ChromeDriver(options);
        getProducts();
        logger.info("AliExpress 생성자 초기화 성공! product의 개수 : " + products.size());
    }

    @Override
    public Product read() throws ApiException {
        if (idx==0){
            create();
        }
        if (idx < products.size()) {
            if ((idx+1) % 20==0) logger.info("현재는 " + (idx+1) +"번째 작동 중입니다! ");
            return getProductDetails(idx++);
        }
        else {
            driver.close();
            driver.quit();
            return null;
        }
    }

    public void getProducts() throws ApiException {
        TaobaoClient client = new DefaultTaobaoClient("http://api.taobao.com/router/rest", "34272625", "9eb7f31401f66e9f636acbcc5e02e1d5");
        for (long PageNumber = 1 ; PageNumber<=20; PageNumber++) {
            AliexpressAffiliateHotproductQueryRequest req = new AliexpressAffiliateHotproductQueryRequest();
            req.setMinSalePrice(50000L);
            req.setPageNo(PageNumber);
            req.setSort("LAST_VOLUME_DESC");
            req.setCategoryIds("3,6,7,13,15");  // 2 : Food, 3 : Apparel & Accessories, 6 : Home Appliances
                                                // 7 : Computer & Office, 13 : Home Improvement, 15 : Home & Garden
            req.setTargetCurrency("KRW");
            req.setTargetLanguage("KO");
            AliexpressAffiliateHotproductQueryResponse response = client.execute(req);
            products.addAll(response.getRespResult().getResult().getProducts());
        }
    }

    public Product getProductDetails(int idx) {
        Product product = new Product();
        AliexpressAffiliateHotproductQueryResponse.Product now = products.get(idx);
        product.setName(now.getProductTitle());
        product.setImageUrl(now.getProductMainImageUrl());
        product.setLocale("kr");
        product.setPrice(Double.parseDouble(now.getTargetAppSalePrice()));
        product.setImages(now.getProductSmallImageUrls());
        product.setCurrency("KRW");
        String rating = now.getEvaluateRate();
        if(rating!=null) rating = rating.substring(0,rating.length()-1);
        else rating="-1";
        product.setRating(Double.parseDouble(rating));
        product.setMarketName("AliExpress");
        product.setLink(now.getPromotionLink());
        driver.get(product.getLink());
        try {
            String price = driver.findElement(By.className("product-price-value")).getText();
            String [] a = price.split(" ");
            String result = a[1].replaceAll("[^0-9]", "");
            product.setPrice(Double.parseDouble(result));
        }
        catch(Exception e){
            try {
                String price = driver.findElement(By.className("uniform-banner-box-price")).getText();
                String [] a = price.split(" ");
                String result = a[1].replaceAll("[^0-9]", "");
                product.setPrice(Double.parseDouble(result));
            }
            catch(Exception f){
            }
        }
        System.out.println(product);
        System.out.println("----------------------------------------------------");
        return product;
    }



}
