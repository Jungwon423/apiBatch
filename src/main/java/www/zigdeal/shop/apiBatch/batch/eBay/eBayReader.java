package www.zigdeal.shop.apiBatch.batch.eBay;

import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.item.ItemReader;
import www.zigdeal.shop.apiBatch.batch.Product;

import java.util.*;

public class eBayReader implements ItemReader<Product> {
    private int count=0;
    private List<String> categories; //카테고리 이름
    private List<String> categoryLinks; // 카테고리 페이지로 가는 링크
    private final List<String> links = new ArrayList<>(); // 특정 카테고리의 링크들
    private final List<Double> priceList =new ArrayList<>(); // links와 대응되는 인덱스를 가지며 links의 제품의 가격
    private final Logger logger = LoggerFactory.getLogger("eBayLogger");
    private WebDriver driver;

    //Properties 설정
    public static String WEB_DRIVER_ID = "webdriver.chrome.driver";
    public static String WEB_DRIVER_PATH = "C:/chromedriver.exe";
//    public static String WEB_DRIVER_PATH = "/home/ubuntu/Downloads/chromedriver";
    public static String TARGET_URL = "https://www.ebay.com/globaldeals";
    public int category_idx = 0;
    public int link_idx = 0;
    public boolean created = false;


    public void create() { // 생성자로 links 초기화

        System.setProperty(WEB_DRIVER_ID, WEB_DRIVER_PATH);
        ChromeOptions options = new ChromeOptions();
        options.setCapability("ignoreProtectedModeSettings", true);
        options.addArguments("--disable-popup-blocking");       //팝업안띄움
        options.addArguments("headless");                       //브라우저 안띄움
        options.addArguments("__lang:euc-kr");
        this.driver = new ChromeDriver(options);

        this.categories = getCategories();
        this.categoryLinks = getCategoryLinks();

        logger.info("eBay 생성자 초기화 성공! 카테고리 종류 수 : " + categoryLinks.size());
    }

    @Override
    public Product read() {
        if (!created){
            created=true;
            create();
        }
        if (category_idx==categoryLinks.size()) {
            driver.close();
            driver.quit();
            return null;
        }
        if (++count%20==0) logger.info("현재는 " + count +"번째 작동 중입니다! ");
        if (link_idx==0){
            getLinks();
        }
        Product ret = getProductDetails();
//        logger.info("현재 category idx : " + String.valueOf(category_idx));
//        logger.info("현재 link idx : " + String.valueOf(link_idx));
        updateIdx();
        return ret;
    }

    public void updateIdx(){
        link_idx++;
        if (link_idx==links.size()){
            link_idx=0;
            category_idx++;
        }
    }

    public void getLinks(){ // 해당 카테고리의 링크들과 가격들 수집
        try {
            driver.get(categoryLinks.get(category_idx)); //카테고리 링크로 이동
            this.links.clear();
            this.priceList.clear();
            ((JavascriptExecutor) driver).executeScript("window.scrollTo(0, document.body.scrollHeight)");
            Thread.sleep(3000);

            List<WebElement> elements;
            elements = driver.findElements(By.cssSelector("div[class='dne-itemtile dne-itemtile-large']")); //제품 링크들 모으는 과정
            for (WebElement element: elements){
                try {
                    String a = element.findElement(By.className("first")).getText();
                    String discount = "";
                    boolean num = false;
                    for (int j = 0; j < a.length(); j++) {
                        if (!num) {
                            if (a.charAt(j) == '$') num = true;
                            continue;
                        }
                        if (a.charAt(j)!=',') discount += a.charAt(j);
                    }
                    this.priceList.add(Double.parseDouble(discount));
                    this.links.add(element.findElement(By.tagName("a")).getAttribute("href"));
                }
                catch(Exception e){}
            }
        }
        catch(Exception e){}
        logger.info(category_idx + "번 카테고리로 변경되었습니다!!");
        logger.info("price list의 길이 : " + String.valueOf(priceList.size()));
        logger.info("links의 길이 : " + String.valueOf(links.size()));
    }


    public List<String> getCategories() {
        driver.get(TARGET_URL);
        List<String> categories = new ArrayList<>();
        List<WebElement> elements = driver.findElements(By.className("dne-pattern-title"));
        for (WebElement element : elements)
            categories.add(element.findElement(By.tagName("span")).getText());
        return categories;
    }

    public List<String> getCategoryLinks(){
        driver.get(TARGET_URL);
        List<String> categoryLinks = new ArrayList<>();
        List <WebElement> elements = driver.findElements(By.cssSelector("div[class='dne-itemtile dne-show-more dne-skip-focus dne-show-more-with-link']"));
        for (WebElement element : elements)
            categoryLinks.add(element.findElement(By.tagName("a")).getAttribute("href"));
        return categoryLinks;
    }

    public Product getProductDetails() {

        Product product = new Product();
        String link = links.get(link_idx);
        driver.get(link);
        double discountRate = 0;
        double price = priceList.get(link_idx);
        String name = "";
        String imgUrl = "";
        WebElement element;
        try { // name
            element = driver.findElement(By.className("x-item-title__mainTitle"));
            name = element.findElement(By.tagName("span")).getText();
        }
        catch(Exception e){
            price=-1;
        }

        try { // imgUrl
            element = driver.findElement(By.cssSelector("div[class='ux-image-carousel-item active image']"));
            imgUrl = element.findElement(By.tagName("img")).getAttribute("src");
        }
        catch(Exception e){
            price=-1;
        }

        try {// discountRate
            element = driver.findElement(By.cssSelector("span[class='ux-textspans ux-textspans--EMPHASIS']"));
            discountRate=Double.parseDouble(toDiscountRate(element.getText()));
        }
        catch(Exception e){
            price=-1;
        }

        product.setName(name);
        product.setPrice(price);
        product.setCurrency("USD");
        product.setDiscountRate(discountRate);
        product.setImageUrl(imgUrl);
        product.setCategoryName(categories.get(category_idx));
        product.setMarketName("eBay");
        product.setLink(link);
        product.setLocale("kr");
//        logger.info("---------- 개별 제품 크롤링 결과입니다 ----------");
//        logger.info("이름 : " + name);
//        logger.info("가격 : " + priceList.get(link_idx).toString());
//        logger.info("할인율 : " + String.valueOf(discountRate));
//        logger.info("이미지 : " + imgUrl);
//        logger.info("link : " + link);
//        logger.info("카테고리 : " + categories.get(category_idx));
        return product;
    }

    public String toDiscountRate(String s){
        String discountRate= "";
        boolean get= false;
        for (int i=0; i<s.length(); i++){
            if (s.charAt(i)=='%') return discountRate;
            if (!get){
                if (s.charAt(i)=='(') get=true;
                continue;
            }
            discountRate+=s.charAt(i);
        }
        return "NOPE";
    }

}
