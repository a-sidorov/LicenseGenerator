import com.google.gson.annotations.Expose;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.sql.Date;
import java.util.UUID;


//Это лежит в бд
@Getter
@AllArgsConstructor
public class License {
    @Expose
    private final UUID id;//primary key

    private final String privateKey;

    @Expose
    private final String licenseKey;

    @Expose
    private final Date createDate;

    @Expose
    private final Date endDate;

    private final Long userID;

    private final String type;
}
