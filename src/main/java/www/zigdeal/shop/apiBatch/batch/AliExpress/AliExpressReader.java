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
    private boolean modal = true; //모달 있음?
    private int count = 0;
    private int idx = 0;
    private final Logger logger = LoggerFactory.getLogger("AliLogger");
    private WebDriver driver;
    private boolean created = false; //생성자

    //Properties 설정
    public static String WEB_DRIVER_ID = "webdriver.chrome.driver";
    public static String WEB_DRIVER_PATH = "C:/chromedriver.exe";
//    public static String WEB_DRIVER_PATH = "/home/ubuntu/Downloads/chromedriver";
    public static String [] TARGET_URLS = {
            "https://ko.aliexpress.com/premium/category/509.html?spm=a2g0o.productlist.103.1.3d2d123f1HNXkB&category_redirect=1&dida=y",
        "https://ko.aliexpress.com/premium/category/7.html?spm=a2g0o.productlist.104.1.e38822c4iopClk&category_redirect=1&dida=y",
        "https://ko.aliexpress.com/premium/category/21.html?spm=a2g0o.productlist.104.2.398e22c4G7g0Xr&category_redirect=1&dida=y",
        "https://ko.aliexpress.com/premium/category/30.html?spm=a2g0o.productlist.104.3.44d222c4aDdo2o&category_redirect=1&dida=y",
        "https://ko.aliexpress.com/premium/category/44.html?spm=a2g0o.productlist.105.1.160522c4Z6BkLS&dida=y",
};


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
        for (String TARGET_URL : TARGET_URLS) {
            try {
                int cnt=0;
                driver.get(TARGET_URL);
                driver.findElement(By.xpath("//*[@id=\"root\"]/div/div/div[2]/div/div[2]/div[2]/div[1]/div/div/div[2]")).click();
                try{Thread.sleep(2000);}
                catch (Exception e){}
                driver.findElement(By.xpath("//*[@id=\"root\"]/div/div/div[2]/div/div[2]/div[1]/div/div[1]/div/div/input[1]")).sendKeys("30000");
                try{Thread.sleep(100);}
                catch (Exception e){}
                driver.findElement(By.xpath("//*[@id=\"root\"]/div/div/div[2]/div/div[2]/div[1]/div/div[1]/div/div[1]/span[3]")).click();
                try{Thread.sleep(1000);}
                catch (Exception e){}
                JavascriptExecutor js = (JavascriptExecutor) driver;
                for (int i=0; i<20 ;i++) {
                    js.executeScript("window.scrollBy(0,300)", "");
                }
                try{Thread.sleep(3000);}
                catch(Exception e){}

                List<WebElement> elements = driver.findElements(By.tagName("a"));
                for (WebElement element : elements) {
                    String link = element.getAttribute("href");
                    if (link == null) continue;
                    if (link.length() < len || !(link.substring(0, len).equals(baseURL)))
                        continue; // href가 없으면 continue
                    cnt++;
                    links.add(link);
                }
                logger.info("추가된 수 : " + cnt);
                //   logger.info("link 크롤링 종료");
            } catch (Exception e) {
                logger.error("에러 발생! : " + e);
            }
        }
       // logger.info("총 읽어온 product link 개수 : "+ links.size());
        return links;
    }

    public Product getProductDetails(String link) {
        Product product = new Product();
        try {
            product.setMarketName("AliExpress");
            product.setCurrency("KRW");
            product.setLocale("kr");
            // TODO product.setTax() & product.setShippingFee()
            driver.get(link);
            Thread.sleep(100);


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
            // name 크롤링
            try {
                WebElement productName = driver.findElement(By.className("product-title-text"));
                product.setName(productName.getText());
            }
            catch(Exception e){
                price="NOPE";
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

            JavascriptExecutor js = (JavascriptExecutor) driver;
            js.executeScript(
                    "var b = document.createElement('script');" +
                            "b.setAttribute('type', 'text/javascript');" +
                            " b.setAttribute('charset', 'UTF-8');" +
                            "b.setAttribute('src', 'https://ln-rules.rewardstyle.com/bookmarklet.js?r=' + Math.random() * 99999999);" +
                            "document.body.appendChild(b);"
            );
            if (idx==1) {
                try {
                    Thread.sleep(3000);
                } catch (Exception e) {
                }
                driver.switchTo().frame("linkninja-frame-bookmarklet");
                driver.findElement(By.xpath("//*[@id=\"login\"]/form/div[1]/div/div/div[1]/div/input")).sendKeys("egrang@daum.net");
                try{Thread.sleep(400);}
                catch (Exception e){}
                driver.findElement(By.xpath("//*[@id=\"login\"]/form/div[2]/div/div/div[1]/div/input")).sendKeys("Zenius123!");
                try{Thread.sleep(400);}
                catch (Exception e){}
                driver.findElement(By.xpath("//*[@id=\"login\"]/form/div[3]/button")).click();
            }
            try{Thread.sleep(5000);}
            catch(Exception e){}
            if (idx!=1)
                driver.switchTo().frame("linkninja-frame-bookmarklet");
            if (modal) {
                try {
                    driver.findElement(By.className("rw--popover__footer")).findElement(By.tagName("button")).click();
                    modal = false;
                }
                catch(Exception e){}
            }
            try{
                Thread.sleep(5000);
            }
            catch(Exception e){}
            try {
                driver.findElement(By.xpath("//*[@id=\"copy-button\"]")).click();
            }
            catch(Exception e){
                price="NOPE";
            }
            if(price!="NOPE") {
                try {
                    WebElement texfield = driver.findElement(By.xpath("//*[@id=\"product_caption\"]/div/div/div[1]/div/textarea"));
                    texfield.click();
                    texfield.sendKeys(Keys.CONTROL + "v");
                    String Ltklink = texfield.getAttribute("value");
                    product.setLink(Ltklink);
                } catch (Exception e) {
                    driver.get("https://www.naver.com/");
                    WebElement textfield = driver.findElement(By.xpath("//*[@id=\"query\"]"));
                    textfield.sendKeys(Keys.CONTROL + "v");
                    String Ltklink = textfield.getAttribute("value");
                    product.setLink(Ltklink);
                }
            }
            if (price.equals("NOPE")) price = "-1";
            product.setPrice(Double.valueOf(price));
            product.setDiscountRate((double) discountRate);

        }
        catch(Exception e){
            e.printStackTrace();
        }
        System.out.println(product);
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
