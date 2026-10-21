package org.apache.fineract.portfolio.crb.api;


import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;

public final class CrbCheckApiResourceSwagger {

    private CrbCheckApiResourceSwagger() {}

    @Schema(description = "Create CRB validation check request")
    public static final class PostCrbCheckRequest {

        private PostCrbCheckRequest() {}

        @Schema(example = "1", description = "CRB validation type: client details, NIN, or phone")
        public Integer crbValidationType;
    }

    public static final class PostCrbCheckResponse{
        public PostCrbCheckResponse() {
        }
    }

    @Schema(description = "Get CRB validation check response")
    public static final class GetCrbChecksResponse {

        private GetCrbChecksResponse() {}

        static final class GetCrbValidationType {

            private GetCrbValidationType() {}

            @Schema(example = "1")
            public Long id;
            @Schema(example = "crbValidationType.details")
            public String code;
            @Schema(example = "Validation Details")
            public String description;
        }

        static final class GetCrbRequestType {

            private GetCrbRequestType() {}

            @Schema(example = "1")
            public Long id;
            @Schema(example = "crbRequestType.Initial")
            public String code;
            @Schema(example = "Initial Validation")
            public String description;
        }

        static final class GetCrbStatus {

            private GetCrbStatus() {}

            @Schema(example = "1")
            public Long id;
            @Schema(example = "crb.Pending")
            public String code;
            @Schema(example = "Pending")
            public String description;
        }

        static final class GetConsentStatus {

            private GetConsentStatus() {}

            @Schema(example = "1")
            public Long id;
            @Schema(example = "status.Pending")
            public String code;
            @Schema(example = "Pending")
            public String description;
        }

        @Schema(example = "15")
        public Long resourceId;

        @Schema(example = "27")
        public Long clientId;

        @Schema(example = "Tom")
        private String clientName;

        public GetCrbValidationType crbValidationType;

        public GetCrbRequestType requestType;

        public GetCrbStatus status;

        private GetConsentStatus consentStatus;

        @Schema(example = "23RETY09-78909-FGHJ")
        private String providerValidationId;

        @Schema(example = "ASDFGHJTYUI")
        private String providerReference;

        @Schema(example = "500.00")
        private BigDecimal cost;

        @Schema(example = "UGX")
        private String currency;

        @Schema(example = "CRB Failed")
        private String errorMessage;

        @Schema(example = "{}")
        private String responsePayload;

        @Schema(example = "2026-10-19T10:30:00")
        private LocalDateTime createdAt;

        @Schema(example = "2026-10-19T10:30:00")
        private LocalDateTime completedAt;

        @Schema(example = "NinStatus: Success")
        private Map<String, Object> responseDetails;

        @Schema(example = "CRB validation check created successfully")
        public String message;
    }
}
