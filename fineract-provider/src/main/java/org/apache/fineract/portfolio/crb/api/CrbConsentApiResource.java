package org.apache.fineract.portfolio.crb.api;


import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.UriInfo;
import lombok.RequiredArgsConstructor;
import org.apache.fineract.commands.domain.CommandWrapper;
import org.apache.fineract.commands.service.CommandWrapperBuilder;
import org.apache.fineract.commands.service.PortfolioCommandSourceWritePlatformService;
import org.apache.fineract.infrastructure.core.api.ApiRequestParameterHelper;
import org.apache.fineract.infrastructure.core.data.CommandProcessingResult;
import org.apache.fineract.infrastructure.core.exception.UnrecognizedQueryParamException;
import org.apache.fineract.infrastructure.core.serialization.ApiRequestJsonSerializationSettings;
import org.apache.fineract.infrastructure.core.serialization.ToApiJsonSerializer;

import org.apache.fineract.infrastructure.core.service.CommandParameterUtil;
import org.apache.fineract.infrastructure.security.service.PlatformSecurityContext;
import org.apache.fineract.portfolio.client.api.ClientApiConstants;
import org.apache.fineract.portfolio.crb.data.CrbApiConstants;
import org.apache.fineract.portfolio.crb.data.CrbConsentData;
import org.apache.fineract.portfolio.crb.service.CrbConsentReadService;
import org.springframework.stereotype.Component;

@Path("/v1/crbconsents")
@Component
@Tag(name = "CRB Consent", description = "Manage client consent for CRB identity verification.")
@RequiredArgsConstructor
public class CrbConsentApiResource {

    private final PlatformSecurityContext context;
    private final CrbConsentReadService crbConsentReadService;
    private final ToApiJsonSerializer<CrbConsentData> toApiJsonSerializer;
    private final ApiRequestParameterHelper apiRequestParameterHelper;
    private final PortfolioCommandSourceWritePlatformService commandsSourceWritePlatformService;

    @GET
    @Path("template")
    @Consumes({ MediaType.APPLICATION_JSON })
    @Produces({ MediaType.APPLICATION_JSON })
    @Operation(summary = "Retrieve CRB Consent Template", description = "Returns the supported consent channels, such as OTP and PAPER.")
    @ApiResponses({@ApiResponse(responseCode = "200", description = "OK",
                    content = @Content(schema = @Schema(implementation = CrbConsentApiResourceSwagger.GetCrbConsentTemplateResponse.class))
            )
    })
    public String retrieveTemplate(@Context final UriInfo uriInfo, @Parameter(description = "commandParam") final String commandParam) {
        context.authenticatedUser().validateHasReadPermission(CrbApiConstants.CRB_RESOURCE_NAME);

        CrbConsentData consentData = null;

        if (CommandParameterUtil.is(commandParam, "revoke")) {
            consentData = crbConsentReadService.retrieveAllNarrations(CrbApiConstants.CRB_REVOKE_REASON);
        }else {
            consentData =crbConsentReadService.retrieveChannelTemplate();
        }

        final ApiRequestJsonSerializationSettings settings = apiRequestParameterHelper.process(uriInfo.getQueryParameters());

        return toApiJsonSerializer.serialize(settings, consentData, CrbApiConstants.CRB_CONSENT_TEMPLATE_RESPONSE_DATA_PARAMETERS);
    }

    @GET
    @Path("{consentId}")
    @Consumes({ MediaType.APPLICATION_JSON })
    @Produces({ MediaType.APPLICATION_JSON })
    @Operation(summary = "Retrieve CRB Consent", description = "Retrieves a CRB consent record by its ID.")
    @ApiResponses({@ApiResponse(responseCode = "200", description = "OK", content = @Content(schema = @Schema(implementation = CrbConsentApiResourceSwagger.GetCrbConsentIdResponse.class)))})
    public String retrieveConsent(@PathParam("consentId") @Parameter(description = "consentId") final Long consentId, @Context final UriInfo uriInfo) {

        context.authenticatedUser().validateHasReadPermission(CrbApiConstants.CRB_RESOURCE_NAME);

        final CrbConsentData consentData = crbConsentReadService.retrieveConsent(consentId);

        final ApiRequestJsonSerializationSettings settings = apiRequestParameterHelper.process(uriInfo.getQueryParameters());

        return toApiJsonSerializer.serialize(settings, consentData, CrbApiConstants.CRB_CONSENT_RESPONSE_DATA_PARAMETERS);
    }

    @POST
    @Consumes({ MediaType.APPLICATION_JSON })
    @Produces({ MediaType.APPLICATION_JSON })
    @Operation(summary = "Create CRB Consent", description = "Creates a consent record for a client. OTP consent sends an OTP to the client's registered phone number. PAPER consent is recorded as verified.")
    @RequestBody(required = true, content = @Content(schema = @Schema(implementation = CrbConsentApiResourceSwagger.PostCrbConsentRequest.class)))
    @ApiResponses({@ApiResponse(responseCode = "200", description = "OK", content = @Content(schema = @Schema(implementation = CrbConsentApiResourceSwagger.PostCrbConsentResponse.class)))
    })
    public String create(@Parameter(hidden = true) final String apiRequestBodyAsJson) {

        final CommandWrapper commandRequest = new CommandWrapperBuilder()
                .createClientCRBConsent()
                .withJson(apiRequestBodyAsJson)
                .build();

        final CommandProcessingResult result = commandsSourceWritePlatformService.logCommandSource(commandRequest);

        return toApiJsonSerializer.serialize(result);
    }

    @PUT
    @Path("{consentId}")
    @Consumes({ MediaType.APPLICATION_JSON })
    @Produces({ MediaType.APPLICATION_JSON })
    @Operation(summary = "Update CRB Consent", description = "Use command=validateOtp to validate the consent OTP or command=revoke to revoke verified consent.")
    @ApiResponses({@ApiResponse(responseCode = "200", description = "OK", content = @Content(schema = @Schema(implementation = CrbConsentApiResourceSwagger.PostCrbConsentValidateOtpResponse.class)))
    })
    public String update(
            @PathParam("consentId") @Parameter(description = "consentId") final Long consentId,
            @QueryParam("command") @Parameter(description = "validateOtp or revoke") final String command,
            @Parameter(hidden = true) final String apiRequestBodyAsJson) {

        final CommandWrapperBuilder builder = new CommandWrapperBuilder();

        final CommandWrapper commandRequest =evaluateCommand(consentId, command, builder, apiRequestBodyAsJson);

        final CommandProcessingResult result = commandsSourceWritePlatformService.logCommandSource(commandRequest);

        return toApiJsonSerializer.serialize(result);
    }

    private CommandWrapper evaluateCommand(final Long consentId, final String commandParam, final CommandWrapperBuilder builder, final String apiRequestBodyAsJson) {
        CommandWrapper commandRequest = null;
        if (CommandParameterUtil.is(commandParam, "validateOtp")) {
            commandRequest = builder.validateCrbOtp(consentId)
                    .withJson(apiRequestBodyAsJson)
                    .build();

        } else if (CommandParameterUtil.is(commandParam, "revoke")) {
            commandRequest = builder.revokeCrbConsent(consentId)
                    .withJson(apiRequestBodyAsJson)
                    .build();
        }else {
            throw new UnrecognizedQueryParamException("command", commandParam,
                    new Object[] { "validateOtp", "revoke"}
            );
        }

        return commandRequest;
    }
}

