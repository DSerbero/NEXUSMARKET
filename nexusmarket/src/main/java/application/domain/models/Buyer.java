package application.domain.models;
import application.domain.valueobjects.BuyerStatus;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.util.ArrayList;
import java.util.List;

@Setter
@Getter
@NoArgsConstructor
public class Buyer extends User {
    private String primaryAddress;
    private List<String> additionalAddresses = new ArrayList<>();
    private BuyerStatus buyerStatus;

}
