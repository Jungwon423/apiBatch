package www.zigdeal.shop.apiBatch.batch.product.readers;

import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.item.ItemReader;
import www.zigdeal.shop.apiBatch.batch.product.domain.Product;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class AliExpressReader implements ItemReader<Product> {

    private final List<String> links;
    private int idx = 0;
    private final Logger logger = LoggerFactory.getLogger("크롤링 로그");
    private WebDriver driver;

    //Properties 설정
    public static String WEB_DRIVER_ID = "webdriver.chrome.driver";
    public static String WEB_DRIVER_PATH = "C:/chromedriver.exe";
    public static String TARGET_URL = "https://ko.aliexpress.com/campaign/wow/gcp/ae/channel/ae/accelerate/tupr?spm=a2g0o.home.countrygrid.1.472b4430H8ED7a&wh_weex=true&_immersiveMode=true&wx_navbar_hidden=true&wx_navbar_transparent=true&ignoreNavigationBar=true&wx_statusbar_hidden=true&wh_pid=ae%2Fchannel%2Fae%2Fkr_plaza%2FKRfastshipping&productIds=%252C%252C%252C%252C%252C%252C%252C%252C%252C%252C";


    public AliExpressReader() { // 생성자로 links 초기화
        this.links = getProductLinks();
    }

    @Override
    public Product read() {
        logger.info(links.toString());
        if (idx < links.size()) {
            return getProductDetails(links.get(idx++));
        }
        else return null;
    }

    public List<String> getProductLinks() {
        System.setProperty(WEB_DRIVER_ID, WEB_DRIVER_PATH);
        ChromeOptions options = new ChromeOptions();
        options.setCapability("ignoreProtectedModeSettings", true);
        options.addArguments("--disable-popup-blocking");       //팝업안띄움
		options.addArguments("headless");                       //브라우저 안띄움
        options.addArguments("__lang:euc-kr");
        driver = new ChromeDriver(options);
        String baseURL = "https://ko.aliexpress.com/item/";
        int len = baseURL.length();

        List<String> links = new ArrayList<>();

        try {
            driver.get(TARGET_URL);
//            var crawlingTime = new Date().getTime();
//            while (new Date().getTime() < crawlingTime + 60000) { // 30000 = 30000 millisecond = 30 sec
//                logger.info("while문 루프 도는 중");
//                ((JavascriptExecutor)driver).executeScript("window.scrollTo(0, document.body.scrollHeight)");
//            }
            Thread.sleep(4000);
            List<WebElement> elements = driver.findElements(By.tagName("a"));
            logger.info("현재 읽어온 a 태그 수 : "+ elements.size());
            for (WebElement element : elements) {
                String link = element.getAttribute("href");
                if (link.length() < len || !(link.substring(0, len).equals(baseURL))) continue; // href가 없으면 continue
                links.add(link);
            }
            logger.info("현재 읽어온 product link 개수 : "+ links.size());
            logger.info("크롤링 종료");
            Thread.sleep(100000);
        }
        catch (Exception e){
            logger.error("에러 발생! : " + e);
        }
        logger.info("총 읽어온 product link 개수 : "+ links.size());
        return links;
    }

    public Product getProductDetails(String link) {

        System.setProperty(WEB_DRIVER_ID, WEB_DRIVER_PATH);

        //Driver SetUp
        ChromeOptions options = new ChromeOptions();
        options.setCapability("ignoreProtectedModeSettings", true);
        options.addArguments("--disable-popup-blocking");       //팝업안띄움
        //options.addArguments("headless");                       //브라우저 안띄움
        options.addArguments("__lang:euc-kr");
        driver = new ChromeDriver(options);
        String baseURL = "https://ko.aliexpress.com/item/";

        Product product = new Product();
        try {
            product.setLink(link);
            product.setMarketName("AliExpress");
            product.setCurrency("KRW");
            product.setLocale("kr");
            // TODO product.setTax() & product.setShippingFee()
            driver.get(link);
            Thread.sleep(1000);

            // name 크롤링
            WebElement productName = driver.findElement(By.className("product-title-text"));
            product.setName(productName.getText());

            int discountRate;
            String price;
            try { // price, discountRate 크롤링
                WebElement element = driver.findElement(By.className("uniform-banner-box-discounts"));
                List<WebElement> spans = element.findElements(By.tagName("span"));
                price = ToPrice(spans.get(0).getText());
                logger.info(price);
                discountRate = Integer.parseInt(spans.get(1).getText().replaceAll("[^0-9]", ""));
                logger.info(String.valueOf(discountRate));
            }
            catch(Exception e){
                WebElement element = driver.findElement(By.className("product-price-original"));
                List <WebElement> elements = element.findElements(By.tagName("span"));
                price = ToPrice(elements.get(0).getText());
                logger.info(price);
                discountRate = Integer.parseInt(elements.get(1).getText().replaceAll("[^0-9]", ""));
                logger.info(String.valueOf(discountRate));
            }

            try{ // imageUrl 크롤링
                WebElement element = driver.findElement(By.className("video-container"));
                element = element.findElement(By.tagName("img"));
                logger.info(element.getAttribute("src"));
                product.setImageUrl(element.getAttribute("src"));
            }
            catch(Exception e){
                WebElement element = driver.findElement(By.className("image-view-magnifier-wrap"));
                element = element.findElement(By.tagName("img"));
                logger.info(element.getAttribute("src"));
                product.setImageUrl(element.getAttribute("src"));
            }

            // categoryName 크롤링
            WebElement element = driver.findElement(By.className("buy-now-wrap"));
            ((JavascriptExecutor)driver).executeScript("arguments[0].scrollIntoView();", element);
            Thread.sleep(100);
            WebElement category = driver.findElement(By.className("parent-title"));
            logger.info(category.getText());
            product.setCategoryName(category.getText());

            try { // productDescription 크롤링
                WebElement description = driver.findElement(By.className("product-description"));
                logger.info("productDescription : " +  description.getAttribute("innerHTML"));
            }
            catch(Exception e){
                WebElement description = driver.findElement(By.className("product-overview"));
                logger.info("productDescription : "+ description.getAttribute("innerHTML"));
            }

            if (price.equals("NOPE")) return null;
            product.setPrice(Double.valueOf(price));
            product.setDiscountRate((double) discountRate);
            logger.info("-------------------- 크롤링 결과 --------------------");
            logger.info(product.toString());
            logger.info("-------------------- 크롤링 결과 --------------------");

        }
        catch(Exception e){
            e.printStackTrace();
        }
        driver.close();
        driver.quit();
        return product;
    }

    public String ToPrice(String price){
        String[] Arr = price.split(" ");
        if (Arr.length>2){
            return "NOPE";
        }
        return price.replaceAll("[^0-9]", "");
    }

}