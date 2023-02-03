package www.zigdeal.shop.apiBatch.batch.AliExpress;

import com.taobao.api.ApiException;
import com.taobao.api.DefaultTaobaoClient;
import com.taobao.api.TaobaoClient;
import com.taobao.api.request.AliexpressAffiliateHotproductQueryRequest;
import com.taobao.api.response.AliexpressAffiliateHotproductQueryResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.item.ItemReader;
import www.zigdeal.shop.apiBatch.batch.Product;

import java.util.ArrayList;
import java.util.List;

public class AliExpressReader implements ItemReader<Product> {

    private int idx = 0;
    private final Logger logger = LoggerFactory.getLogger("AliLogger");
    private List<AliexpressAffiliateHotproductQueryResponse.Product> products = new ArrayList<>();

    public void create() throws ApiException { // 생성자로 links 초기화
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
            return null;
        }
    }

    public void getProducts() throws ApiException {
        TaobaoClient client = new DefaultTaobaoClient("http://api.taobao.com/router/rest", "34272625", "9eb7f31401f66e9f636acbcc5e02e1d5");
        AliexpressAffiliateHotproductQueryRequest req = new AliexpressAffiliateHotproductQueryRequest();
        req.setKeywords("all");
        req.setMinSalePrice(50000L);
        req.setPageSize(10L);
        req.setTargetCurrency("KRW");
        req.setTargetLanguage("KO");
        AliexpressAffiliateHotproductQueryResponse response = client.execute(req);
        products = response.getRespResult().getResult().getProducts();
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
        System.out.println(product);
        return product;
    }


}
