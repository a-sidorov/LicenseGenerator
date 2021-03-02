import com.google.gson.annotations.Expose;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.sql.Date;
import java.util.UUID;


@Getter
@AllArgsConstructor
public class PublicLicense {

    @Expose
    private final UUID id;

    @Expose
    private final String licenseKey;

    @Expose
    private final Date createDate;

    @Expose
    private final Date endDate;
}
