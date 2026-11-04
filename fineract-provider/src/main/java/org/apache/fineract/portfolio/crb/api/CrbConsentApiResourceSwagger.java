package org.apache.fineract.portfolio.crb.api;


import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;
import java.util.Collection;


public final class CrbConsentApiResourceSwagger {

    private CrbConsentApiResourceSwagger() {}

    @Schema(description = "GetCrbConsentIdResponse")
    public static final class GetCrbConsentIdResponse {

        private GetCrbConsentIdResponse() {}

        static final class GetChannel {

            private GetChannel() {}

            @Schema(example = "1")
            public Long id;
            @Schema(example = "channel.Otp")
            public String code;
            @Schema(example = "Otp Channel")
            public String description;
        }

        static final class GetStatus {

            private GetStatus() {}

            @Schema(example = "1")
            public Long id;
            @Schema(example = "status.Pending")
            public String code;
            @Schema(example = "Pending")
            public String description;
        }

        @Schema(example = "1")
        public Long id;

        @Schema(example = "27")
        public Long clientId;

        @Schema(example = "Victor Nuwagaba")
        public String clientName;

        public GetChannel consentChannel;

        public GetStatus consentStatus;

        @Schema(example = "+256700123456")
        public String phoneNumber;

        @Schema(example = "CRB_CONSENT_V1")
        public String consentVersion;

        @Schema(example = "2026-10-19T10:30:00")
        public LocalDateTime consentedAt;

        @Schema(example = "2026-10-19T11:30:00")
        public LocalDateTime revokedAt;

        @Schema(example = "1")
        public Long consentedBy;

        @Schema(example = "2")
        public Long verifiedBy;

        @Schema(example = "3")
        public Long revokedBy;
    }

    @Schema(description = "GetCrbConsentTemplateResponse")
    public static final class GetCrbConsentTemplateResponse {

        private GetCrbConsentTemplateResponse() {}

        static final class GetChannel {

            private GetChannel() {}

            @Schema(example = "1")
            public Long id;
            @Schema(example = "channel.Otp")
            public String code;
            @Schema(example = "Otp Channel")
            public String description;
        }

        public Collection<GetChannel> consentChannels;
    }

    @Schema(description = "PostCrbConsentRequest")
    public static final class PostCrbConsentRequest {

        private PostCrbConsentRequest() {}

        @Schema(example = "27")
        public Long clientId;

        @Schema(example = "1", description = "Consent channel ID: OTP or PAPER")
        public Integer consentChannel;

        @Schema(example = "CRB_CONSENT_V1")
        public String consentVersion;

        @Schema(example = "I consent to the verification of my identity through CRB.")
        public String consentText;

        @Schema(example = "+256700123456")
        public String phoneNumber;

        @Schema(example = "dd MMMM yyyy")
        public String dateFormat;

        @Schema(example = "en")
        public String locale;
    }

    @Schema(description = "PostCrbConsentResponse")
    public static final class PostCrbConsentResponse {

        private PostCrbConsentResponse() {}

        @Schema(example = "10")
        public Long resourceId;

        @Schema(example = "27")
        public Long clientId;
    }

    @Schema(description = "PostCrbConsentValidateOtpRequest")
    public static final class PostCrbConsentValidateOtpRequest {

        private PostCrbConsentValidateOtpRequest() {}

        @Schema(example = "12345")
        public Integer otpCode;

        @Schema(example = "dd MMMM yyyy")
        public String dateFormat;

        @Schema(example = "en")
        public String locale;
    }

    @Schema(description = "PostCrbConsentValidateOtpResponse")
    public static final class PostCrbConsentValidateOtpResponse {

        private PostCrbConsentValidateOtpResponse() {}

        @Schema(example = "10")
        public Long resourceId;

        @Schema(example = "27")
        public Long clientId;

        @Schema(example = "1")
        public Long officeId;

    }

    @Schema(description = "PostCrbConsentRevokeRequest")
    public static final class PostCrbConsentRevokeRequest {

        private PostCrbConsentRevokeRequest() {}

        @Schema(example = "1")
        public Long rejectionReasonId;
    }

    @Schema(description = "PostCrbConsentRevokeResponse")
    public static final class PostCrbConsentRevokeResponse {

        private PostCrbConsentRevokeResponse() {}

        @Schema(example = "10")
        public Long resourceId;

        @Schema(example = "27")
        public Long clientId;

        @Schema(example = "1")
        public Long officeId;
    }
}
