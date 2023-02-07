package www.zigdeal.shop.apiBatch.batch;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.ArrayList;
import java.util.List;

class Comment {
    @Id
    private String id;
    private String writerId;
    private String content;
    private List<String> goodUser = new ArrayList<>();
    private List<String> badUser = new ArrayList<>();
    private String timestamp;
    private String productId;
}

@Data
@Document(collection = "product")
public class Product {
    @Id
    private String name;
    private List <String> subImageUrl = new ArrayList<>();
    private List <String> good = new ArrayList<>();
    private List <String> bad = new ArrayList<>();
    private List <String> wishUserList = new ArrayList<>();
    private List<Comment> comments = new ArrayList<>();
    private Double rating;
    private Double direct_tax = -1d;
    private Double direct_shippingFee = -1d;
    private Double price;
    private String currency;
    private Double discountRate;
    private String imageUrl;
    private String categoryName;
    private String categoryName2;
    private String marketName;
    private String link;
    private Double indirect_tax = -1d;
    private Double indirect_shippingFee = -1d;
    private int clickCount = 0;
    private String locale;
    private Double naverPrice;
}