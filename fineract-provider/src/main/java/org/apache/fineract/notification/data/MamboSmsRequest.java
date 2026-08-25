package org.apache.fineract.notification.data;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;
import java.io.Serializable;
import java.util.List;

@Data
@NoArgsConstructor
@Accessors(chain = true)
public class MamboSmsRequest implements Serializable {

    private static final long serialVersionUID = 1L;


    private String message;
    private List<String> recipients;
    private String message_category;
    private String sender_id;

    public MamboSmsRequest(String message, List<String> recipients) {
        this.message = message;
        this.recipients = recipients;
    }
}
