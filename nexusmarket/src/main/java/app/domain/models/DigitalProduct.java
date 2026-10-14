package app.domain.models;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class DigitalProduct extends Product {
    private String digitalAsset;
}
