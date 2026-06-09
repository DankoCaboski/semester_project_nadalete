package br.com.alltallent.e2e;

import br.com.alltallent.model.Area;
import br.com.alltallent.model.Perfil;
import br.com.alltallent.repository.AreaRepository;
import br.com.alltallent.repository.PerfilRepository;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static io.restassured.RestAssured.given;
import static io.restassured.module.jsv.JsonSchemaValidator.matchesJsonSchemaInClasspath;
import static org.hamcrest.Matchers.*;

class AuthE2EIT extends BaseE2ETest {

    @Autowired private AreaRepository areaRepository;
    @Autowired private PerfilRepository perfilRepository;

    private Integer areaId;
    private Integer perfilId;

    @BeforeEach
    void seedDatabase() {
        Area area = new Area();
        area.setNome("Engenharia");
        area.setDescricao("Desenvolvimento de software");
        areaId = areaRepository.save(area).getCodigo();

        Perfil perfil = new Perfil();
        perfil.setNome("Colaborador");
        perfil.setDescricao("Perfil padrão");
        perfilId = perfilRepository.save(perfil).getCodigo();
    }

    // ── POST /api/auth/register ────────────────────────────────────────────────

    @Test
    void register_returns201_whenValidPayload() {
        given()
                .contentType(ContentType.JSON)
                .body(registerPayload("joao@test.com"))
        .when()
                .post("/api/auth/register")
        .then()
                .statusCode(201)
                .body(containsString("sucesso"));
    }

    @Test
    void register_returns400_whenEmailAlreadyInUse() {
        String payload = registerPayload("dup@test.com");

        given().contentType(ContentType.JSON).body(payload)
                .post("/api/auth/register").then().statusCode(201);

        given()
                .contentType(ContentType.JSON)
                .body(payload)
        .when()
                .post("/api/auth/register")
        .then()
                .statusCode(400)
                .body(containsString("Email"));
    }

    @Test
    void register_returns400_whenRequiredFieldsMissing() {
        given()
                .contentType(ContentType.JSON)
                .body("{}")
        .when()
                .post("/api/auth/register")
        .then()
                .statusCode(400);
    }

    @Test
    void register_returns400_whenAreaDoesNotExist() {
        given()
                .contentType(ContentType.JSON)
                .body("""
                        {
                          "nomeCompleto": "Sem Area",
                          "email": "semarea@test.com",
                          "senha": "123456",
                          "cpf": "000.000.000-00",
                          "idCracha": "CRACHA-X",
                          "codigoArea": 99999,
                          "codigoPerfil": %d
                        }""".formatted(perfilId))
        .when()
                .post("/api/auth/register")
        .then()
                .statusCode(400);
    }

    // ── POST /api/auth/login ───────────────────────────────────────────────────

    @Test
    void login_returns200AndValidSchema_whenValidCredentials() {
        given().contentType(ContentType.JSON).body(registerPayload("login.ok@test.com"))
                .post("/api/auth/register").then().statusCode(201);

        given()
                .contentType(ContentType.JSON)
                .body("{\"email\":\"login.ok@test.com\",\"password\":\"senha123\"}")
        .when()
                .post("/api/auth/login")
        .then()
                .statusCode(200)
                .body(matchesJsonSchemaInClasspath("schemas/login-response.json"))
                .body("token", not(emptyString()))
                .body("nomeCompleto", equalTo("Login OK"))
                .body("userId", greaterThan(0));
    }

    @Test
    void login_returns401_whenPasswordIsWrong() {
        given().contentType(ContentType.JSON).body(registerPayload("wrongpass@test.com"))
                .post("/api/auth/register").then().statusCode(201);

        given()
                .contentType(ContentType.JSON)
                .body("{\"email\":\"wrongpass@test.com\",\"password\":\"ERRADA\"}")
        .when()
                .post("/api/auth/login")
        .then()
                .statusCode(401);
    }

    @Test
    void login_returns401_whenEmailDoesNotExist() {
        given()
                .contentType(ContentType.JSON)
                .body("{\"email\":\"nobody@test.com\",\"password\":\"any\"}")
        .when()
                .post("/api/auth/login")
        .then()
                .statusCode(401);
    }

    // ── GET /api/auth/me ──────────────────────────────────────────────────────

    @Test
    void me_returns200WithUserData_whenAuthenticated() {
        given().contentType(ContentType.JSON).body(registerPayload("me@test.com"))
                .post("/api/auth/register").then().statusCode(201);

        String token = obtainToken("me@test.com", "senha123");

        given()
                .header("Authorization", "Bearer " + token)
        .when()
                .get("/api/auth/me")
        .then()
                .statusCode(200)
                .body("email", equalTo("me@test.com"))
                .body("nomeCompleto", not(emptyString()));
    }

    @Test
    void me_returns401_whenNoToken() {
        given()
        .when()
                .get("/api/auth/me")
        .then()
                .statusCode(401);
    }

    // ── Helper ────────────────────────────────────────────────────────────────

    private String registerPayload(String email) {
        return """
                {
                  "nomeCompleto": "Login OK",
                  "email": "%s",
                  "senha": "senha123",
                  "cpf": "123.456.789-00",
                  "idCracha": "CRACHA-001",
                  "codigoArea": %d,
                  "codigoPerfil": %d
                }""".formatted(email, areaId, perfilId);
    }
}
