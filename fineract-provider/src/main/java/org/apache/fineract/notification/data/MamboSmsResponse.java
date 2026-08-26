package org.apache.fineract.notification.data;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.List;

@Data
@NoArgsConstructor
@Accessors(chain = true)
public class MamboSmsResponse implements Serializable {

    private static final long serialVersionUID = 1L;

    private String statusCode;
    private Boolean success;
    private List<String> messages;
    private DataObj data;

    @Data
    @NoArgsConstructor
    @Accessors(chain = true)
    public static class DataObj implements Serializable{

        private static final long serialVersionUID = 1L;

        private Integer recipients_count;
        private Integer message_count;
        private Integer sms_sent;
        private Integer sms_cost;
        private Integer new_balance;
        private Integer unsupported_contacts_count;
        private List<String> unsupported_contacts;
    }
}
