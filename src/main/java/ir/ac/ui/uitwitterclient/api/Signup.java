package ir.ac.ui.uitwitterclient.api;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class Signup {

    private String phone;
    private String name;
}
