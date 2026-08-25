package org.apache.fineract.notification.data;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

import java.io.Serializable;
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
    public class DataObj implements Serializable{
        private String name;
        private String contact;
        private String email;
        private String balance;
    }
}
