package com.placeandplay.organizationservice.api;

import com.placeandplay.organizationservice.model.OrganizationLoginRequest;
import com.placeandplay.organizationservice.model.OrganizationRegistrationRequest;
import com.placeandplay.organizationservice.service.OrganizationService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Api(tags = "Organization APIs", description = "APIs for organization management")
@RestController
@SecurityRequirement(name = "bearerAuth")
@RequestMapping("/api/auth/organization")
@RequiredArgsConstructor
public class OrganizationController {

    private static final Logger log = LoggerFactory.getLogger(OrganizationController.class);
    private final OrganizationService organizationService;

    @PostMapping("/registration")
    @ApiOperation(value = "Register new organization", notes = "Register a new organization with basic information")
    @ApiResponses(value = {
        @ApiResponse(code = 200, message = "Successfully registered organization"),
        @ApiResponse(code = 400, message = "Invalid input data"),
        @ApiResponse(code = 409, message = "Organization already exists")
    })
    public ResponseEntity<?> registerOrganization(
            @RequestBody OrganizationRegistrationRequest request
    ) throws Exception {
        log.info("Received organization registration request for email: {}", request.getEmail());
        return organizationService.register(request);
    }

    @PostMapping("/login")
    @ApiOperation(value = "Login organization", notes = "Authenticate organization using email and password")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Successfully logged in"),
            @ApiResponse(code = 401, message = "Invalid email or password")
    })
    public ResponseEntity<?> login(
            @RequestBody OrganizationLoginRequest loginRequest
    ) throws Exception {
        log.info("Received login request for email: {}", loginRequest.getEmail());
        return organizationService.login(loginRequest);
    }

    @GetMapping("/orgInfo")
    @ApiOperation(value = "Get organization info", notes = "Retrieve organization information using access and refresh tokens")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Successfully retrieved organization info"),
            @ApiResponse(code = 401, message = "Invalid access token"),
            @ApiResponse(code = 404, message = "Organization not found")
    })
    public ResponseEntity<?> orgInfo(
            @RequestHeader("accessToken") String accessToken,
            @RequestHeader("refreshToken") String refreshToken
    ) throws Exception {
        log.info("Received request for organization info with access token");
        return organizationService.orgInfo(accessToken, refreshToken);
    }
}