package www.zigdeal.shop.apiBatch.batch.product.domain;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Data
@Document(collection = "product")
public class Product {

    @Id
    private String id;
    private String name;
    private Double price;
    private String currency;
    private Double discountRate;
    private String imageUrl;
    private String categoryName;
    private String marketName;
    private String link;
    private Double tax;
    private Double shippingFee;
    private int clickCount;
    private String locale;
}