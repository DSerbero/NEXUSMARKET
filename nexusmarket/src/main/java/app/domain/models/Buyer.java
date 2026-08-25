package app.domain.models;
import app.domain.valueObjects.BuyerStatus;
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
    private List<String> additionalAddress = new ArrayList<>();
    private BuyerStatus buyerStatus;

}
