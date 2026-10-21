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
import org.apache.fineract.portfolio.crb.data.CrbApiConstants;
import org.apache.fineract.portfolio.crb.data.CrbCheckData;
import org.apache.fineract.portfolio.crb.service.CrbCheckReadServicePlatform;
import org.springframework.stereotype.Component;

import java.util.List;

@Path("/v1/crbchecks")
@Component
@Tag(name = "CRB Checks", description = "Manage CRB client verification checks.")
@RequiredArgsConstructor
public class CrbCheckApiResource {

    private final PlatformSecurityContext context;
    private final CrbCheckReadServicePlatform crbCheckReadService;
    private final ToApiJsonSerializer<List<CrbCheckData>> toApiJsonSerializer;
    private final ApiRequestParameterHelper apiRequestParameterHelper;
    private final PortfolioCommandSourceWritePlatformService commandsSourceWritePlatformService;

    @GET
    @Path("{clientId}")
    @Consumes({ MediaType.APPLICATION_JSON })
    @Produces({ MediaType.APPLICATION_JSON })
    @Operation(summary = "Retrieve CRB Checks", description = "Retrieves the saved CRB validation checks for a client, including their statuses and provider results.")
    @ApiResponses({@ApiResponse(responseCode = "200", description = "OK", content = @Content(schema = @Schema(implementation = CrbCheckApiResourceSwagger.GetCrbChecksResponse.class)))})
    public String retrieveChecks(@PathParam("clientId") @Parameter(description = "Client ID", required = true) final Long clientId, @Context final UriInfo uriInfo) {

        context.authenticatedUser().validateHasReadPermission(CrbApiConstants.CRB_CHECK_RESOURCE_NAME);

        if (clientId == null) {
            throw new IllegalArgumentException("clientId is required");
        }

        final List<CrbCheckData> checks = crbCheckReadService.retrieveChecks(clientId);

        final ApiRequestJsonSerializationSettings settings = apiRequestParameterHelper.process(uriInfo.getQueryParameters());

        return toApiJsonSerializer.serialize(settings, checks);
    }

    @POST
    @Consumes({ MediaType.APPLICATION_JSON })
    @Produces({ MediaType.APPLICATION_JSON })
    @Operation(summary = "Create CRB Check", description = "Creates and executes an initial CRB validation check for a client with verified, non-revoked consent.")
    @RequestBody(required = true, content = @Content(schema = @Schema(implementation = CrbCheckApiResourceSwagger.PostCrbCheckRequest.class)))
    @ApiResponses({@ApiResponse(responseCode = "200", description = "CRB check processed successfully", content = @Content(schema = @Schema(implementation = CrbCheckApiResourceSwagger.PostCrbCheckResponse.class))),
            @ApiResponse(responseCode = "400", description = "Invalid request or missing verified consent"),
            @ApiResponse(responseCode = "404", description = "Client not found")
    })
    public String create(@QueryParam("clientId") @Parameter(description = "Client ID", required = true) final Long clientId,
                         @Parameter(hidden = true) final String apiRequestBodyAsJson) {


        if (clientId == null) {
            throw new IllegalArgumentException("clientId is required");
        }

        final CommandWrapper commandRequest = new CommandWrapperBuilder()
                .createCrbCheck(clientId)
                .withJson(apiRequestBodyAsJson)
                .build();

        final CommandProcessingResult result = commandsSourceWritePlatformService.logCommandSource(commandRequest);

        return toApiJsonSerializer.serialize(result);
    }

    @POST
    @Path("{clientId}")
    @Consumes({ MediaType.APPLICATION_JSON })
    @Produces({ MediaType.APPLICATION_JSON })
    @Operation(summary = "Recheck CRB Validation", description = "Re-executes an eligible CRB validation check using the existing check and provider reference where supported.")
    @ApiResponses({@ApiResponse(responseCode = "200", description = "CRB recheck processed successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid command or check cannot be rechecked"),
            @ApiResponse(responseCode = "404", description = "CRB check not found")
    })
    public String recheck(@PathParam("clientId") @Parameter(description = "Client ID", required = true) final Long clientId,
            @QueryParam("command") @Parameter(description = "recheck") final String command,
            @Parameter(hidden = true) final String apiRequestBodyAsJson) {


        if (!CommandParameterUtil.is(command, "recheck")) {
            throw new UnrecognizedQueryParamException("command", command,
                    new Object[] { "recheck" }
            );
        }

        final CommandWrapper commandRequest = new CommandWrapperBuilder()
                .recheckClientCRBCheck(clientId)
                .withJson(apiRequestBodyAsJson)
                .build();

        final CommandProcessingResult result = commandsSourceWritePlatformService.logCommandSource(commandRequest);

        return toApiJsonSerializer.serialize(result);
    }
}
