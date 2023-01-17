package www.zigdeal.shop.apiBatch.batch.AliExpress;

import org.openqa.selenium.*;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.item.ItemReader;
import www.zigdeal.shop.apiBatch.batch.Product;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class AliExpressReader implements ItemReader<Product> {

    private List<String> links;
    private int count = 0;
    private int idx = 0;
    private final Logger logger = LoggerFactory.getLogger("AliLogger");
    private WebDriver driver;
    private boolean created = false; //생성자

    //Properties 설정
    public static String WEB_DRIVER_ID = "webdriver.chrome.driver";
    public static String WEB_DRIVER_PATH = "C:/chromedriver.exe";
//    public static String WEB_DRIVER_PATH = "/home/ubuntu/Downloads/chromedriver";
    public static String TARGET_URL = "https://ko.aliexpress.com/campaign/wow/gcp/ae/channel/ae/accelerate/tupr?spm=a2g0o.home.countrygrid.1.472b4430H8ED7a&wh_weex=true&_immersiveMode=true&wx_navbar_hidden=true&wx_navbar_transparent=true&ignoreNavigationBar=true&wx_statusbar_hidden=true&wh_pid=ae%2Fchannel%2Fae%2Fkr_plaza%2FKRfastshipping&productIds=%252C%252C%252C%252C%252C%252C%252C%252C%252C%252C";


    public void create() { // 생성자로 links 초기화
        this.links = getProductLinks();
        logger.info("AliExpress 생성자 초기화 성공! links 길이 : " + links.size());
    }

    @Override
    public Product read() {
        if (!created){
            created=true;
            create();
        }
        if (idx < links.size()) {
            if (++count%20==0) logger.info("현재는 " + count +"번째 작동 중입니다! ");
            return getProductDetails(links.get(idx++));
        }
        else {
            driver.close();
            driver.quit();
            return null;
        }
    }

    public List<String> getProductLinks() {
        System.setProperty(WEB_DRIVER_ID, WEB_DRIVER_PATH);
        ChromeOptions options = new ChromeOptions();
        options.setPageLoadStrategy(PageLoadStrategy.EAGER);
        options.setCapability("ignoreProtectedModeSettings", true);
        options.addArguments("--disable-popup-blocking");       //팝업안띄움
        options.addArguments("headless");                       //브라우저 안띄움
        options.addArguments("__lang:euc-kr");
        this.driver = new ChromeDriver(options);
        String baseURL = "https://ko.aliexpress.com/item/";
        int len = baseURL.length();

        List<String> links = new ArrayList<>();

        try {
            driver.get(TARGET_URL);
            var crawlingTime = new Date().getTime();

          //  logger.info("while문 루프 도는 중");
            while (new Date().getTime() < crawlingTime + 30000) { // 60000 = 60000 millisecond = 60 sec = 1 min
                ((JavascriptExecutor)driver).executeScript("window.scrollTo(0, document.body.scrollHeight)");
            }
            Thread.sleep(10000);
            List<WebElement> elements = driver.findElements(By.tagName("a"));
       //     logger.info("현재 읽어온 a 태그 수 : "+ elements.size());
            for (WebElement element : elements) {
                String link = element.getAttribute("href");
                if (link.length() < len || !(link.substring(0, len).equals(baseURL))) continue; // href가 없으면 continue
                links.add(link);
            }
         //   logger.info("link 크롤링 종료");
        }
        catch (Exception e){
            logger.error("에러 발생! : " + e);
        }
       // logger.info("총 읽어온 product link 개수 : "+ links.size());
        return links;
    }

    public Product getProductDetails(String link) {

        Product product = new Product();
        try {
            product.setLink(link);
            product.setMarketName("AliExpress");
            product.setCurrency("KRW");
            product.setLocale("kr");
            // TODO product.setTax() & product.setShippingFee()
            driver.get(link);
            Thread.sleep(100);

            // name 크롤링
            WebElement productName = driver.findElement(By.className("product-title-text"));
            product.setName(productName.getText());

            int discountRate = -1 ;
            String price = "NOPE";
          //  logger.info("링크 : " + link);
            try{ // 판매가
                WebElement element = driver.findElement(By.className("uniform-banner-box-price"));
            //    logger.info("가격 Case 1: " + ToPrice(element.getText()));
                price = ToPrice(element.getText());
            }
            catch(Exception e){
                try {
                    WebElement element = driver.findElement(By.className("product-price-current"));
                    element = element.findElement(By.tagName("span"));
                //    logger.info("가격 Case 2: " + ToPrice(element.getText()));
                    price = ToPrice(element.getText());
                }
                catch(Exception f){}
            }
            try { // discountRate 크롤링
                WebElement element = driver.findElement(By.className("uniform-banner-box-discounts"));
                List<WebElement> spans = element.findElements(By.tagName("span"));
                discountRate = Integer.parseInt(spans.get(1).getText().replaceAll("[^0-9]", ""));
             //   logger.info("할인율 : " + discountRate);
            }
            catch(Exception e){
                try {
                    WebElement element = driver.findElement(By.className("product-price-original"));
                    List<WebElement> elements = element.findElements(By.tagName("span"));
                    discountRate = Integer.parseInt(elements.get(1).getText().replaceAll("[^0-9]", ""));
                 //   logger.info("할인율 : " + discountRate);
                }
                catch(Exception f){}
            }

            try{ // imageUrl 크롤링
                WebElement element = driver.findElement(By.className("video-container"));
                element = element.findElement(By.tagName("img"));
          //      logger.info("Case 1 : 사진 링크 : " + element.getAttribute("src"));
                product.setImageUrl(element.getAttribute("src"));
            }
            catch(Exception e){
                try {
                    WebElement element = driver.findElement(By.className("image-view-magnifier-wrap"));
                    element = element.findElement(By.tagName("img"));
           //         logger.info("Case 2 : 사진 링크 : " + element.getAttribute("src"));
                    product.setImageUrl(element.getAttribute("src"));
                }
                catch(Exception f){}
            }

            // categoryName 크롤링
            WebElement element = driver.findElement(By.className("buy-now-wrap"));
            ((JavascriptExecutor)driver).executeScript("arguments[0].scrollIntoView();", element);
            Thread.sleep(100);
            WebElement category = driver.findElement(By.className("parent-title"));
        //    logger.info("카테고리 이름 : " + category.getText());
            product.setCategoryName(category.getText());

            try { // productDescription 크롤링
                WebElement description = driver.findElement(By.className("product-description"));
//                logger.info("productDescription : " +  description.getAttribute("innerHTML"));
            }
            catch(Exception e){
                WebElement description = driver.findElement(By.className("product-overview"));
//                logger.info("productDescription : "+ description.getAttribute("innerHTML"));
            }

            if (price.equals("NOPE")) price = "-1";
            product.setPrice(Double.valueOf(price));
            product.setDiscountRate((double) discountRate);

        }
        catch(Exception e){
            e.printStackTrace();
        }
        return product;
    }

    public String ToPrice(String price){
        String[] Arr = price.split(" ");
        if (Arr.length>2){
            return "NOPE";
        }
        return Arr[1].replaceAll("[^0-9]", "");
    }

}
