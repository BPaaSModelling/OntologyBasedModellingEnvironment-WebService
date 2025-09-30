package ch.fhnw.modeller.webservice.reasoning;

import ch.fhnw.modeller.webservice.dto.AIAskRequestDto;
import com.google.gson.Gson;

import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.logging.Logger;

@Path("/OntologyAndSWRL/ai")
public class AIChatService {

    private final Logger logger = Logger.getLogger(AIChatService.class.getName());
    private static final Gson gson = new Gson();

    @POST
    @Path("/ask-ollama")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response askOllama(AIAskRequestDto req) throws Exception {
        String system = "You are a SWRL assistant. Given a SWRL rule and (optional) TTL excerpt:" +
                "\n1) Validate syntax & variable usage (e.g., variables in consequent must appear in antecedent)." +
                "\n2) Explain issues briefly." +
                "\n3) Propose a corrected rule." +
                "\n4) Offer 1–2 alternative patterns when useful." +
                "\nKeep answers concise. Show SWRL in fenced code blocks.";

        String ttlPart = (req.getTtlContent() != null && !req.getTtlContent().isBlank())
                ? "\n\nTTL excerpt:\n```ttl\n" + truncate(req.getTtlContent(), 8000) + "\n```"
                : "";

        String prompt = system
                + "\n\nSWRL rule:\n```swrl\n" + safe(req.getSwrlRule()) + "\n```"
                + ttlPart
                + "\n\nUser question/comment:\n" + safe(req.getUserMessage());

        String body = "{\"model\":\"llama3.1:latest\",\"prompt\":%s,\"stream\":false}".formatted(jsonString(prompt));

        HttpClient http = HttpClient.newHttpClient();
        HttpRequest httpReq = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:11434/api/generate"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();

        HttpResponse<String> resp = http.send(httpReq, HttpResponse.BodyHandlers.ofString());

        // Ollama response contains a "response" field with the text
        String answer = new com.fasterxml.jackson.databind.json.JsonMapper()
                .readTree(resp.body()).path("response").asText("");

        String payload = gson.toJson(answer);

        logger.info("PAYLOAD: " + payload);

        return Response.ok(payload).build();
    }

    // --- helpers ---
    private static String truncate(String s, int max) {
        return s == null ? "" : (s.length() > max ? s.substring(0, max) : s);
    }

    private static String safe(String s) {
        return s == null ? "" : s;
    }

    private static String jsonString(String s) {
        return "\"" + s.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n") + "\"";
    }
}
