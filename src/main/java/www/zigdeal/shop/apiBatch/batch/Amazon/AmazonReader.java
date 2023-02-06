package www.zigdeal.shop.apiBatch.batch.Amazon;

import org.openqa.selenium.*;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.item.ItemReader;
import www.zigdeal.shop.apiBatch.batch.Product;

import java.util.ArrayList;
import java.util.List;

public class AmazonReader implements ItemReader<Product> {
    private List<String> links;
    private int idx = 0;
    private final Logger logger = LoggerFactory.getLogger("AmazonLogger");
    private WebDriver driver;
    private  boolean created = false;

    //Properties 설정
    public static String WEB_DRIVER_ID = "webdriver.chrome.driver";
    public static String WEB_DRIVER_PATH = "C:/chromedriver.exe";
//    public static String WEB_DRIVER_PATH = "/home/ubuntu/Downloads/chromedriver";
    public static String TARGET_URL = "https://www.amazon.com/-/ko/gp/goldbox?ref_=nav_cs_gb&language=ko_KR&currency=USD";
    public static int CrollingNumber = 30;
    String pageUrlprefix = "https://www.amazon.com/gp/goldbox?ref_=nav_cs_gb&deals-widget=%257B%2522version%2522%253A1%252C%2522viewIndex%2522%253A";
    String pageUrlsuffix = "%252C%2522presetId%2522%253A%2522AE6BA37878475F9AE4C584B7AD5E12BE%2522%252C%2522sorting%2522%253A%2522BY_SCORE%2522%257D#";

    public void create() { // 생성자로 links 초기화
        this.links = CrawlSecondLevel();
        logger.info("생성자 초기화 성공! links 길이" + links.size());
    }

    @Override
    public Product read() {
        if (!created){
            created=true;
            create();
        }
        if (idx < links.size()) {
            return Crawl(links.get(idx++));
        } else {
            driver.close();
            driver.quit();
            return null;
        }
    }

    public List<String> CrawlFirstLevel() {
        System.setProperty(WEB_DRIVER_ID, WEB_DRIVER_PATH);
        ChromeOptions options = new ChromeOptions();
        options.setPageLoadStrategy(PageLoadStrategy.EAGER);
        options.setCapability("ignoreProtectedModeSettings", true);
        options.addArguments("--disable-popup-blocking");       //팝업안띄움
       // options.addArguments("headless");                       //브라우저 안띄움
        List<String> firstLinks = new ArrayList<>();
        driver = new ChromeDriver(options);
        driver.get(TARGET_URL);

        try {
            Thread.sleep(2000);
            ((JavascriptExecutor) driver).executeScript("window.scrollTo(0, document.body.scrollHeight)");

            List<WebElement> elements = driver.findElements(By.className("DealGridItem-module__withoutActionButton_2OI8DAanWNRCagYDL2iIqN"));
            for (WebElement elem : elements) { //elements로 받아서 temps로 addall 하면 오류가 떠서 String 리스트로 받았음.
                firstLinks.add(elem.findElement(By.className("a-link-normal")).getAttribute("href"));
            }
            int pageNum = CrollingNumber / 60;
            if (pageNum >= 1) {
                for (int x = 1; x <= pageNum; x++) {     //x 페이지로 이동  (60개씩 받음) 3페이지면 180개
                    String newLink = pageUrlprefix + x * 60 + pageUrlsuffix;
                    Thread.sleep(500);
                    driver.get(newLink);
                    Thread.sleep(2000);
                    ((JavascriptExecutor) driver).executeScript("window.scrollTo(0, document.body.scrollHeight)");  //스크롤
                    List<WebElement> temps = driver.findElements(By.className("DealGridItem-module__withoutActionButton_2OI8DAanWNRCagYDL2iIqN"));
                    for (WebElement temp : temps) {      //elements로 받아서 temps로 addall 하면 오류가 떠서 String 리스트로 받았음.
                        firstLinks.add(temp.findElement(By.className("a-link-normal")).getAttribute("href"));
                    }
                    System.out.println(firstLinks.size());
                }
            }
        } catch (Exception e) {
            logger.error("firstLevel에서 에러남!");
        }
        logger.info("firstLevelLink 갯수: " + firstLinks.size());
        return firstLinks;
    }

    public List<String> CrawlSecondLevel() {
        logger.info("최종 link 가져오는 중....");
        List<String> firstLinks = CrawlFirstLevel();

        String cmp = "https://www.amazon.com/deal"; // 가능성

        List<String> links = new ArrayList<>();
        for (int i = 0; i < CrollingNumber; i++) {      //링크 받고 싶은 만큼 선택 (걸러져서 조금 적게 나옴)
            String testLink = firstLinks.get(i);
            try {
                driver.get(testLink);
                driver.findElement(By.id("productTitle"));   //바로 제품페이지로 가서 productTitle을 찾을 때
                links.add(testLink);
            } catch (Exception e) {
                if (!testLink.startsWith(cmp)) continue;
                if (testLink.contains("/promotion/")) {    //프로모션 페이지로 갈 때
                    continue;
                }
                try {
                    WebElement Unavailable = driver.findElement(By.className("a-spacing-base"));    //상품 다 팔려서 없다고 뜰 때
                    if (Unavailable.getText().contains("unavailable")) continue;
                    if (Unavailable.getText().contains("이 딜은 현재 이용할 수 없지만")) continue;
                }
                catch(Exception ignored){}

                try {    //링크 여러개 뜰 때 제품 하나 가져오기
                    List<WebElement> ele = driver.findElements(By.className("a-list-item"));
                    for (WebElement element : ele){
                        String now = element.findElement(By.className("a-link-normal")).getAttribute("href");
                        links.add(now);
                    }
                } catch (Exception e2) {    //나머지 경우는 그냥 제외했음 어지럽다
                    logger.error("SecondLevel 예외 제외함");
                }
            }
        }
        logger.info("최종링크 갯수 : " + links.size());
        return links;
    }

    public Product Crawl(String link) {
        Product product = new Product();
        product.setLink(link);
        //logger.info(link);
        product.setMarketName("Amazon");
        product.setLocale("kr");
        product.setCurrency("USD");
        System.out.println(link);
        try {// name
            driver.get(link);
            Thread.sleep(1500);
            WebElement productTitle = driver.findElement(By.id("productTitle"));
            product.setName(productTitle.getText()); //제품이름
            //logger.info("제품이름 : " + productTitle.getText());
        } catch (Exception e) {
            product.setPrice(-1d);
        }

        try {// Price
            WebElement price = driver.findElement(By.className("a-price"));
            String priceText = price.getText();
            if (!priceText.contains(".")) {
                String[] text = priceText.split("\n");
                priceText = text[0] + "." + text[1];
            }
            priceText = priceText.replaceAll("[^0-9.]", "");
            product.setPrice(Double.parseDouble(priceText));  //가격
            //logger.info("가격 : " + priceText);
        } catch (Exception e) {
            product.setPrice(-1d);
        }

        try {
            WebElement imgLink = driver.findElement(By.id("landingImage"));
            product.setImageUrl(imgLink.getAttribute("src")); //이미지 링크
            //logger.info("이미지링크 : " + imgLink.getAttribute("src"));
        } catch (Exception e) {
            product.setPrice(-1d);
            //logger.error("이미지 에러");
        }

        List<String> images= new ArrayList<>();// images
        List<WebElement> elements = driver.findElements(By.cssSelector("li[class=\"a-spacing-small item imageThumbnail a-declarative\"]"));
        for (WebElement element : elements) {
            element = element.findElement(By.tagName("img"));
            images.add(element.getAttribute("src"));
        }
        if(images.size()==0){
            product.setPrice(-1d);
        }

        try {// Rating
            WebElement element= driver.findElement(By.id("acrPopover"));
            String [] a = element.getAttribute("title").split(" ");
            product.setRating(Double.parseDouble(a[a.length-1]));
        }
        catch(Exception e){
            product.setRating(-1d);
        }

        try {
            Thread.sleep(1000);
            WebElement element= driver.findElement(By.id("amazonGlobal_feature_div"));
            element = element.findElement(By.cssSelector("a[class=\"a-popover-trigger a-declarative\"]"));
            element.click();
            element = driver.findElement(By.cssSelector("div[class=\"a-popover a-popover-no-header a-declarative a-arrow-bottom\"]"));
            elements = element.findElements(By.tagName("tr"));
            System.out.println("hi" + elements.size());
            String ship = elements.get(1).findElement(By.cssSelector("td[class=\"a-span2 a-text-right\"]")).findElement(By.tagName("span")).getText();
            StringBuilder direct_ship = new StringBuilder();
            boolean on = false;
            for (int i=0; i<ship.length(); i++){
                if (!on){
                    if(ship.charAt(i)=='$') on=true;
                    continue;
                }
                direct_ship.append(ship.charAt(i));
            }
            System.out.println("ship = " + direct_ship);
            String tax =  elements.get(2).findElement(By.cssSelector("td[class=\"a-span2 a-text-right\"]")).findElement(By.tagName("span")).getText();
            on = false;
            StringBuilder direct_tax = new StringBuilder();
            for (int i=0; i<tax.length(); i++){
                if (!on){
                    if(tax.charAt(i)=='$') on=true;
                    continue;
                }
                direct_tax.append(tax.charAt(i));
            }
            System.out.println("tax = "  + direct_tax);
            product.setDirect_shippingFee(Double.parseDouble(direct_ship.toString()));
            product.setDirect_tax(Double.parseDouble(direct_tax.toString()));
        }
        catch(Exception e){
            product.setPrice(-1d);
        }
        product.setSubImageUrl(images);
        //logger.info("---------- 개별 제품 크롤링 결과입니다 ----------");
        //logger.info(product.toString());
        //logger.info("---------- 개별 제품 크롤링 결과입니다 ----------");
        return product;
    }
}
