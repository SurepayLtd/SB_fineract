package org.apache.fineract.portfolio.crb.api;

import com.google.gson.JsonElement;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.HeaderParam;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.UriInfo;
import lombok.RequiredArgsConstructor;
import org.apache.fineract.commands.domain.CommandWrapper;
import org.apache.fineract.commands.service.CommandWrapperBuilder;
import org.apache.fineract.commands.service.PortfolioCommandSourceWritePlatformService;
import org.apache.fineract.infrastructure.core.api.ApiRequestParameterHelper;
import org.apache.fineract.infrastructure.core.api.JsonCommand;
import org.apache.fineract.infrastructure.core.data.CommandProcessingResult;
import org.apache.fineract.infrastructure.core.exception.UnrecognizedQueryParamException;
import org.apache.fineract.infrastructure.core.serialization.ApiRequestJsonSerializationSettings;
import org.apache.fineract.infrastructure.core.serialization.FromJsonHelper;
import org.apache.fineract.infrastructure.core.serialization.ToApiJsonSerializer;

import org.apache.fineract.infrastructure.core.service.CommandParameterUtil;
import org.apache.fineract.infrastructure.documentmanagement.api.FileUploadValidator;
import org.apache.fineract.infrastructure.security.service.PlatformSecurityContext;
import org.apache.fineract.portfolio.crb.data.CrbApiConstants;
import org.apache.fineract.portfolio.crb.data.CrbConsentData;
import org.apache.fineract.portfolio.crb.domain.ConsentChannel;
import org.apache.fineract.portfolio.crb.service.CrbConsentReadService;
import org.apache.fineract.portfolio.crb.service.CrbConsentWriteService;
import org.glassfish.jersey.media.multipart.FormDataBodyPart;
import org.glassfish.jersey.media.multipart.FormDataContentDisposition;
import org.glassfish.jersey.media.multipart.FormDataParam;
import org.springframework.stereotype.Component;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

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
    private final CrbConsentWriteService crbConsentWriteService;
    private final FileUploadValidator fileUploadValidator;
    private final FromJsonHelper fromJsonHelper;


    @GET
    @Path("template")
    @Consumes({ MediaType.APPLICATION_JSON })
    @Produces({ MediaType.APPLICATION_JSON })
    @Operation(summary = "Retrieve CRB Consent Template", description = "Returns the supported consent channels, such as OTP and PAPER.")
    @ApiResponses({@ApiResponse(responseCode = "200", description = "OK",
                    content = @Content(schema = @Schema(implementation = CrbConsentApiResourceSwagger.GetCrbConsentTemplateResponse.class))
            )
    })
    public String retrieveTemplate(@Context final UriInfo uriInfo, @QueryParam("command") @Parameter(description = "commandParam") final String commandParam) {
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
    @Path("{clientId}")
    @Consumes({ MediaType.APPLICATION_JSON })
    @Produces({ MediaType.APPLICATION_JSON })
    @Operation(summary = "Retrieve CRB Consent", description = "Retrieves a CRB consent record by its ID.")
    @ApiResponses({@ApiResponse(responseCode = "200", description = "OK", content = @Content(schema = @Schema(implementation = CrbConsentApiResourceSwagger.GetCrbConsentIdResponse.class)))})
    public String retrieveConsent(@PathParam("clientId") @Parameter(description = "clientId") final Long clientId, @Context final UriInfo uriInfo) {

        context.authenticatedUser().validateHasReadPermission(CrbApiConstants.CRB_RESOURCE_NAME);

        final CrbConsentData consentData = crbConsentReadService.retrieveConsent(clientId);

        final ApiRequestJsonSerializationSettings settings = apiRequestParameterHelper.process(uriInfo.getQueryParameters());

        return toApiJsonSerializer.serialize(settings, consentData, CrbApiConstants.CRB_CONSENT_RESPONSE_DATA_PARAMETERS);
    }

    @POST
    @Path("{clientId}")
    @Consumes({ MediaType.MULTIPART_FORM_DATA })
    @Produces({ MediaType.APPLICATION_JSON })
    @Operation(summary = "Create CRB Consent", description = "Creates a consent record for a client. OTP consent sends an OTP to the client's registered phone number. PAPER consent is recorded as verified.")
    @RequestBody(required = true, content = @Content(mediaType = MediaType.MULTIPART_FORM_DATA, schema = @Schema(implementation = CrbConsentApiResourceSwagger.PostCrbConsentRequest.class)))
    @ApiResponses({@ApiResponse(responseCode = "200", description = "OK", content = @Content(schema = @Schema(implementation = CrbConsentApiResourceSwagger.PostCrbConsentResponse.class)))
    })
    public String create(@PathParam("clientId") @Parameter(description = "Client ID", required = true) final Long clientId,
                         @FormDataParam("command") final String apiRequestBodyAsJson,
                         @FormDataParam("file") final InputStream file,
                         @FormDataParam("file") final FormDataBodyPart filePart,
                         @FormDataParam("file") final FormDataContentDisposition fileDetails,
                         @HeaderParam("Content-Length") @Parameter(description = "Content-Length") final Long contentLength
    ) throws IOException {
        this.context.authenticatedUser().validateHasCreatePermission(CrbApiConstants.CRB_RESOURCE_NAME);

        JsonElement parsedCommand = this.fromJsonHelper.parse(apiRequestBodyAsJson);


        JsonCommand command = JsonCommand.from(apiRequestBodyAsJson, parsedCommand, fromJsonHelper,
                null, null, null, null, clientId,
                null, null, null, null, null,
                null, null, null, null);

        Integer consentChannel = command.integerValueOfParameterNamed("consentChannel");

        if (ConsentChannel.PAPER.getValue().equals(consentChannel)) {
            this.fileUploadValidator.validate(contentLength, file, fileDetails, filePart);
        }

        byte[] fileBytes = file != null ? file.readAllBytes() : null;

        Long fileSize = fileBytes != null ? (long) fileBytes.length : null;

        InputStream fileInputStream = fileBytes != null ? new ByteArrayInputStream(fileBytes) : null;

        String fileName = fileDetails != null ? fileDetails.getFileName() : null;

        String contentType = filePart != null && filePart.getMediaType() != null ? filePart.getMediaType().toString() : null;

        CommandProcessingResult result = this.crbConsentWriteService.createConsent(
                clientId, command, fileInputStream, fileName, fileSize, contentType
        );

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
            @QueryParam("command") @Parameter(description = "validateOtp or revoke or reactivate or resendOtp") final String command,
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
        }else if (CommandParameterUtil.is(commandParam, "reactivate")) {
            commandRequest = builder.reactivateCrbConsent(consentId)
                    .withJson(apiRequestBodyAsJson)
                    .build();
        }else if (CommandParameterUtil.is(commandParam, "resendOtp")) {
            commandRequest = builder.resendOtpCrbConsent(consentId)
                    .withJson(apiRequestBodyAsJson)
                    .build();
        }else {
            throw new UnrecognizedQueryParamException("command", commandParam,
                    new Object[] { "validateOtp", "revoke", "reactivate", "resendOtp"}
            );
        }

        return commandRequest;
    }
}

