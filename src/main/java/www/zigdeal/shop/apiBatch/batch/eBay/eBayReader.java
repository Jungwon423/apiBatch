package www.zigdeal.shop.apiBatch.batch.eBay;

import org.openqa.selenium.*;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.support.ui.Select;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.item.ItemReader;
import www.zigdeal.shop.apiBatch.batch.Product;

import java.util.*;

public class eBayReader implements ItemReader<Product> {
    private int count=0;
    private boolean modal = true;
    private List<String> categories; //카테고리 이름
    private List<String> categoryLinks; // 카테고리 페이지로 가는 링크
    private final List<String> links = new ArrayList<>(); // 특정 카테고리의 링크들
    private final List<Double> priceList =new ArrayList<>(); // links와 대응되는 인덱스를 가지며 links의 제품의 가격
    private final Logger logger = LoggerFactory.getLogger("eBayLogger");
    private WebDriver driver;
    private JavascriptExecutor js;

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
        options.setPageLoadStrategy(PageLoadStrategy.EAGER);
        options.setCapability("ignoreProtectedModeSettings", true);
        options.addArguments("--disable-popup-blocking");       //팝업안띄움
//        options.addArguments("headless");                       //브라우저 안띄움
        options.addArguments("__lang:euc-kr");
        this.driver = new ChromeDriver(options);
        js = (JavascriptExecutor) driver;
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
        double star = -1;
        List <String> images = new ArrayList<>();
        List <WebElement> elements = driver.findElements(By.cssSelector("button[class=\"ux-image-filmstrip-carousel-item image-treatment image\"]"));
        System.out.println(elements.size());
        try {
            Thread.sleep(1000);
            for (WebElement element2 : elements) {
                images.add(element2.findElement(By.tagName("img")).getAttribute("src"));
            }
        }
        catch(Exception e){
            System.out.println("이미지들 모 ㅅ가져옴");
        }
        try { //별점
            element = driver.findElement(By.id("review-ratings-cntr"));
            String a= element.getAttribute("aria-label");
            int cnt=0;
            int pre=-1;
            String str_star ="";
            for (int i=0 ;i<a.length(); i++){
                if (a.charAt(i)=='개'){
                    cnt++;
                    if(cnt==2){
                        for (int j=pre+1; j<i; j++){
                            str_star+=a.charAt(j);
                        }
                        break;
                    }
                }
                else if(a.charAt(i)==' '){
                    pre=i;
                }
            }
            star=Double.valueOf(str_star);
        }
        catch(Exception e){
        }


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

        js.executeScript(
                "var b = document.createElement('script');" +
                        "b.setAttribute('type', 'text/javascript');" +
                        " b.setAttribute('charset', 'UTF-8');" +
                        "b.setAttribute('src', 'https://ln-rules.rewardstyle.com/bookmarklet.js?r=' + Math.random() * 99999999);" +
                        "document.body.appendChild(b);"
        );
        if (category_idx==0 && link_idx==0) {
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
        if(category_idx!=0 || link_idx!=0)
            driver.switchTo().frame("linkninja-frame-bookmarklet");
        if (modal) {
            try{
                driver.findElement(By.className("rw--popover__footer")).findElement(By.tagName("button")).click();
                modal = false;
                try{Thread.sleep(400);}
                catch (Exception e){}
            }
            catch(Exception e){}
        }
        try{Thread.sleep(400);}
        catch (Exception e){}
        try {
            driver.findElement(By.xpath("//*[@id=\"copy-button\"]")).click();
        }
        catch(Exception e){
            price=-1;
        }
        if(price!=-1) {
            try {
                WebElement texfield = driver.findElement(By.xpath("//*[@id=\"product_caption\"]/div/div/div[1]/div/textarea"));
                texfield.click();
                texfield.sendKeys(Keys.CONTROL + "v");
                link = texfield.getAttribute("value");
            } catch (Exception e) {
                driver.get("https://www.naver.com/");
                WebElement textfield = driver.findElement(By.xpath("//*[@id=\"query\"]"));
                textfield.sendKeys(Keys.CONTROL + "v");
                link = textfield.getAttribute("value");
            }
        }
        product.setSubImageUrl(images);
        product.setRating(star);
        product.setName(name);
        product.setPrice(price);
        product.setCurrency("USD");
        product.setDiscountRate(discountRate);
        product.setImageUrl(imgUrl);
        product.setCategoryName(categories.get(category_idx));
        product.setMarketName("eBay");
        product.setLink(link);
        product.setLocale("kr");
        System.out.println(link);
        System.out.println(star);
        System.out.println(images);
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
