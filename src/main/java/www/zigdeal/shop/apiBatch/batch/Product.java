package www.zigdeal.shop.apiBatch.batch;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.List;

@Data
@Document(collection = "product")
public class Product {

    @Id
    private String name;
    private List <String> images;
    private double rating;
    private Double direct_tax;
    private Double direct_shippingFee;
    private Double price;
    private String currency;
    private Double discountRate;
    private String imageUrl;
    private String categoryName;
    private String marketName;
    private String link;
    private Double indirect_tax;
    private Double indirect_shippingFee;
    private int clickCount;
    private String locale;
    private Double naverPrice;
}