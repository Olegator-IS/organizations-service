package com.placeandplay.organizationservice.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.placeandplay.organizationservice.config.JwtAuthenticationFilter;
import com.placeandplay.organizationservice.config.TokenSecurity;
import com.placeandplay.organizationservice.model.ResponseAnswers.Response;
import com.placeandplay.organizationservice.model.ResponseAnswers.ResponseToken;
import com.placeandplay.organizationservice.model.*;
import com.placeandplay.organizationservice.repository.OrganizationAccountRepository;
import com.placeandplay.organizationservice.repository.OrganizationRepository;
import com.placeandplay.organizationservice.model.places.Place;
import com.placeandplay.organizationservice.repository.PlaceRepository;
import io.jsonwebtoken.Claims;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.crypto.SecretKey;

@Service
@RequiredArgsConstructor
public class OrganizationService {

    private static final Logger log = LoggerFactory.getLogger(OrganizationService.class);
    private final SecretKey secretKey;
    private final OrganizationRepository organizationRepository;
    private final PlaceRepository placeRepository;
    private final OrganizationAccountRepository organizationAccountRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    @Transactional
    public ResponseEntity<?> register(OrganizationRegistrationRequest request) throws Exception {
        if (organizationAccountRepository.existsByEmail(request.getEmail())) {
            log.warn("Registration attempt with existing email: {}", request.getEmail());
            return ResponseEntity.badRequest()
                    .body(new Response(400, "Email already exists", "EMAIL_ALREADY_EXISTS"));
        }

        if (organizationAccountRepository.existsByPhone(request.getPhone())) {
            log.warn("Registration attempt with existing phone: {}", request.getPhone());
            return ResponseEntity.badRequest()
                    .body(new Response(400, "Phone already exists", "PHONE_ALREADY_EXISTS"));
        }


        // Создаем новую структуру для org_info
        ObjectMapper mapper = new ObjectMapper();
        ObjectNode orgInfoNode = mapper.createObjectNode();

        // Добавляем name и sportName
        orgInfoNode.set("name", request.getOrgInfo().get("name"));
        orgInfoNode.set("sportName", request.getOrgInfo().get("sportName"));

        // Создаем структуру для атрибутов
        ObjectNode attributesNode = mapper.createObjectNode();
        JsonNode originalAttributes = request.getOrgInfo().get("attributes");

        // Проходим по всем атрибутам
        originalAttributes.fields().forEachRemaining(entry -> {
            String key = entry.getKey();
            JsonNode attributeMetadata = entry.getValue();

            // Создаем новую структуру для каждого атрибута
            ObjectNode attributeNode = mapper.createObjectNode();

            // Копируем метаданные
            attributeNode.set("name", attributeMetadata.get("name"));
            attributeNode.set("input", attributeMetadata.get("input"));

            // Для multiselect добавляем options
            if (attributeMetadata.has("options")) {
                attributeNode.set("options", attributeMetadata.get("options"));
            }

            // Добавляем значение атрибута
            if (attributeMetadata.has("value")) {
                attributeNode.set("value", attributeMetadata.get("value"));
            }

            // Добавляем атрибут в общую структуру
            attributesNode.set(key, attributeNode);
        });

        orgInfoNode.set("attributes", attributesNode);

        // Создание организации
        Organizations organization = new Organizations();
//        organization.setOrgId(placeId);
        organization.setOrgType(request.getOrgType());
        organization.setCurrentLocation(request.getCurrentLocation());
        organization.setAttributes(orgInfoNode);  // Устанавливаем новую структуру
        organization.setStatus("PENDING");
        organization.setVerificationStatus("UNVERIFIED");
        organization.setPriceCategory("STANDARD");
        organization.setRating(0.0);
        organization.setAddress(request.getAddress());

//        if (request.getAttributes() != null &&
//                request.getAttributes().has("facilities") &&
//                request.getAttributes().get("facilities").has("capacity")) {
//            organization.setMaxCapacity(request.getAttributes().get("facilities").get("capacity").asInt());
//        }

        organizationRepository.save(organization);
        log.info("Created new organization with ID: {}", organization.getOrgId());

        Long orgId = organization.getOrgId();
        if (orgId == null) {
            log.error("Failed to create organization");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new Response(500, "Failed to create place entry", "PLACE_CREATION_FAILED"));
        }



        // Сначала создаём место
        Place place = new Place();
        place.setOrgId(orgId);
        place.setCurrentLocationCityId(1);
        place.setCurrentLocationCountryId(1);
        place.setName(request.getOrgName());
        place.setTypeId(request.getSportTypeId()); // Предполагаем, что type_id 1 соответствует TENNIS
        place.setAddress(request.getAddress());
        place.setPhone(request.getPhone());
        place.setDescription(request.getDescription());
        place.setLatitude(request.getLatitude()); // Добавьте, когда будет доступно
        place.setLongitude(request.getLongitude()); // Добавьте, когда будет доступно
        place = placeRepository.save(place);

        // Проверка успешного создания места


        // Создание учетной записи организации
        OrganizationAccounts account = new OrganizationAccounts();
        account.setOrgId(orgId);
        account.setEmail(request.getEmail());
        account.setPassword(passwordEncoder.encode(request.getPassword()));
        account.setPhone(request.getPhone());
        account.setStatus("PENDING");
        account.setEmailVerified(false);
        account.setPhoneVerified(false);
        organizationAccountRepository.save(account);



        log.info("Created organization account for email: {}", request.getEmail());

        // Генерация токенов (предполагается, что у вас есть метод для этого)
        String accessToken = jwtAuthenticationFilter.generateTokenForOrg(request.getEmail(),"ORGANIZATION");
        String refreshToken = jwtAuthenticationFilter.generateRefreshTokenForOrg(request.getEmail(),"ORGANIZATION");

        return ResponseEntity.ok(new ResponseToken(200,
                TokenSecurity.encryptToken(accessToken, secretKey),
                TokenSecurity.encryptToken(refreshToken, secretKey)));
    }

    public ResponseEntity<?> login(OrganizationLoginRequest loginRequest) throws Exception {
        OrganizationAccounts account = organizationAccountRepository.findByEmail(loginRequest.getEmail());
        if (account == null || !passwordEncoder.matches(loginRequest.getPassword(), account.getPassword())) {
            log.warn("Invalid login attempt for email: {}", loginRequest.getEmail());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new Response(401, "Invalid email or password", "INVALID_CREDENTIALS"));
        }

        // Генерация токенов
        String accessToken = JwtAuthenticationFilter.generateTokenForOrg(loginRequest.getEmail(),"ORGANIZATION");
        String refreshToken = JwtAuthenticationFilter.generateRefreshTokenForOrg(loginRequest.getEmail(),"ORGANIZATION");

        log.info("User logged in with email: {}", loginRequest.getEmail());
        return ResponseEntity.ok(new ResponseToken(200,
                TokenSecurity.encryptToken(accessToken, secretKey),
                TokenSecurity.encryptToken(refreshToken, secretKey)));
    }

    public ResponseEntity<?> orgInfo(String accessToken, String refreshToken) throws Exception {
        String decryptedToken = TokenSecurity.decryptToken(accessToken, secretKey);
        Claims claims = jwtAuthenticationFilter.extractClaims(decryptedToken);


        if (claims.getSubject() != null) {
            OrganizationAccounts account = organizationAccountRepository.findByEmail(claims.getSubject());
            if (account == null) {
                log.warn("No account found for email: {}", claims.getSubject());
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(new Response(404, "Organization not found", "ORG_NOT_FOUND"));
            }

            Long orgId = account.getOrgId();
            Organizations organization = organizationRepository.findById(orgId)
                    .orElse(null);
            OrganizationAccounts organizationAccounts = organizationAccountRepository.findByEmail(account.getEmail());

            if (organization == null) {
                log.warn("No organization found for orgId: {}", orgId);
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(new Response(404, "Organization not found", "ORG_NOT_FOUND"));
            }
            if(organizationAccounts == null) {
                log.warn("No organization accounts found for orgId: {}", orgId);
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(new Response(404, "Organization account not found", "ORG_NOT_FOUND"));
            }


            log.info("Retrieved organization info for orgId: {}", orgId);
            return ResponseEntity.ok(new OrganizationInfoResponse(organization, account));

        } else {
            log.warn("Пустой subject в токене. Токен: {}", accessToken);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new Response(400, "TOKEN_VALIDATION_ERROR", "Произошла ошибка при получении данных из токена."));
        }

    }
}