# Sentiment Service

Microsserviço em **Java 21 + Spring Boot 3** para análise de sentimento de feedbacks.  
Integra com **Hugging Face Inference API** (ou **mock** para rodar offline).  
Retorna **insights acionáveis**: `sentiment`, `score`, `summary`, `reason`, `provider`, **`improvementArea`** e **`improvement`**.

---

## 🧰 Pré-requisitos

- Java 21
- Maven 3.9+
- (Opcional) Token da Hugging Face (`hf_...`) se usar provider real

---

## 🚀 Como rodar

### Via Maven (dev)
```bash
mvn spring-boot:run
```

### Via JAR
```bash
mvn -q -DskipTests package
java -jar target/sentiment-service-0.1.0.jar
```

---

## 🔌 Configuração dos providers

### Mock (padrão)
Sem chaves, útil para desenvolvimento local.
```properties
app.ai.provider=mock
```

### Hugging Face (real)
**Linux/macOS**
```bash
export APP_AI_PROVIDER=huggingface
export HUGGINGFACE_TOKEN=hf_xxx
export HUGGINGFACE_MODEL=cardiffnlp/twitter-xlm-roberta-base-sentiment
export HUGGINGFACE_TIMEOUT_SECONDS=60
mvn spring-boot:run
```

**Windows PowerShell**
```powershell
$Env:APP_AI_PROVIDER="huggingface"
$Env:HUGGINGFACE_TOKEN="hf_xxx"
$Env:HUGGINGFACE_MODEL="cardiffnlp/twitter-xlm-roberta-base-sentiment"
$Env:HUGGINGFACE_TIMEOUT_SECONDS="60"
mvn spring-boot:run
```

**application.yml (recomendado)**
```yaml
app:
  ai:
    provider: ${APP_AI_PROVIDER:mock}

huggingface:
  token: ${HUGGINGFACE_TOKEN:}
  model: ${HUGGINGFACE_MODEL:cardiffnlp/twitter-xlm-roberta-base-sentiment}
  timeout-seconds: ${HUGGINGFACE_TIMEOUT_SECONDS:60}

server:
  port: 8080
```

---

## 🌐 Endpoint

```
POST /api/v1/sentiment
Content-Type: application/json
```

### Request
```json
{
  "text": "O atendimento foi excelente, mas a espera foi grande e o sistema apresentou lentidão.",
  "source": "Suporte"
}
```

### Response (exemplo)
```json
{
  "sentiment": "MIXED",
  "score": 0.78,
  "summary": "Elogio ao atendimento com sinais negativos de espera e performance.",
  "reason": "tempo de espera",
  "provider": "HuggingFace: cardiffnlp/twitter-xlm-roberta-base-sentiment",
  "improvementArea": "tempo de espera",
  "improvement": "Reduzir tempo de resposta: revisar SLAs, dimensionar equipe nos picos e informar tempo estimado de espera."
}
```

---

## 🧪 Teste rápido (curl)

```bash
curl -s -X POST "http://localhost:8080/api/v1/sentiment"   -H "Content-Type: application/json"   -d '{"text":"Atendimento cordial, mas a espera foi grande e o sistema lento.","source":"Suporte"}' | jq
```

---

## 📖 Swagger (UI)

Adicione ao `pom.xml`:
```xml
<dependency>
  <groupId>org.springdoc</groupId>
  <artifactId>springdoc-openapi-starter-webmvc-ui</artifactId>
  <version>2.5.0</version>
</dependency>
```

Acesse:
- UI: `http://localhost:8080/swagger-ui.html`
- Docs: `http://localhost:8080/v3/api-docs`

---

## 🔐 CORS (front em `http://localhost:3000`)

**Bean global simples (sem Spring Security):**
```java
// src/main/java/com/pedro/sentiment/config/CorsConfig.java
@Configuration
public class CorsConfig implements WebMvcConfigurer {
  @Override public void addCorsMappings(CorsRegistry r) {
    r.addMapping("/**")
     .allowedOrigins("http://localhost:3000")
     .allowedMethods("GET","POST","PUT","DELETE","OPTIONS")
     .allowedHeaders("*")
     .allowCredentials(false)
     .maxAge(3600);
  }
}
```

Teste do preflight:
```bash
curl -i -X OPTIONS "http://localhost:8080/api/v1/sentiment"   -H "Origin: http://localhost:3000"   -H "Access-Control-Request-Method: POST"   -H "Access-Control-Request-Headers: Content-Type"
```

---

## 🧩 Endpoints auxiliares

- `GET /_env` → diagnóstico rápido (provider, modelo; token mascarado)

---

## 🐳 Docker

### Build & run
```bash
mvn -q -DskipTests package
docker build -t pedro/sentiment-service:0.1.0 .
docker run --rm -p 8080:8080   -e APP_AI_PROVIDER=huggingface   -e HUGGINGFACE_TOKEN=hf_xxx   -e HUGGINGFACE_MODEL=cardiffnlp/twitter-xlm-roberta-base-sentiment   -e HUGGINGFACE_TIMEOUT_SECONDS=60   pedro/sentiment-service:0.1.0
```

### Docker Compose (exemplo)
```yaml
services:
  sentiment:
    image: pedro/sentiment-service:0.1.0
    ports:
      - "8080:8080"
    environment:
      APP_AI_PROVIDER: huggingface
      HUGGINGFACE_TOKEN: ${HUGGINGFACE_TOKEN}
      HUGGINGFACE_MODEL: cardiffnlp/twitter-xlm-roberta-base-sentiment
      HUGGINGFACE_TIMEOUT_SECONDS: "60"
```

---

## 🛡️ Boas práticas

- **Não** commitar o token (`.env` no `.gitignore`).
- Usar variáveis de ambiente/secret manager em produção.
- Logs sem vazar segredo (apenas presença/tamanho do token).

---

## 🩺 Troubleshooting

- **404 em `/api/v1/sentiment`** → verifique o controller:  
  Classe `@RequestMapping("/api/v1")` **e** método `@PostMapping("/sentiment")`.
- **CORS no front** → habilite `allowedOrigins` para `http://localhost:3000` e confirme o preflight (comando acima).
- **422 Invalid JSON (Hugging Face)** → payload batch deve ser `{"inputs":["frase1","frase2"]}` (já implementado).
- **Neutro em texto longo/misto** → segmentação por sentenças e agregação; ajuste limiares (`POS_STRONG/NEG_STRONG`) se preciso.

---

## 🗺️ Roadmap

- [x] Segmentação por sentenças + rótulo **MIXED**
- [x] Extração de **reason** (ex.: “tempo de espera”)
- [x] **ImprovementArea** + **Improvement** (ação recomendada)
- [x] Swagger UI
- [ ] Métricas (Micrometer) + dashboards (p95/p99, error/fallback rate)
- [ ] Auth JWT e rate limiting
- [ ] Testes de integração (WebTestClient)
- [ ] Manifests para Kubernetes
