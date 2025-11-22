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
    @Produces(MediaType.TEXT_PLAIN)
    public Response askOllama(AIAskRequestDto req) throws Exception {
        String system =
                "You are an expert SWRL reasoning assistant. You help users create, validate and improve "
                        + "Semantic Web Rule Language (SWRL) rules based on ontology content provided in TTL format.\n\n"

                        + "General behaviour:\n"
                        + "- If the user asks to create or suggest one or more SWRL rules, do that leveraging the provided TTL.\n"
                        + "- You may internally validate, correct and optimise rules, but your FINAL ANSWER must contain SWRL rules with their explanations.\n"
                        + "- NEVER include or repeat the TTL content in your answer.\n\n"

                        + "Examples of SWRL rules (style and structure):\n"
                        + "1) Inferring a Mother from a Woman with a child:\n"
                        + "\n"
                        + "Woman(?m) ^ hasChild(?m, ?c) -> Mother(?m)\n"
                        + "\n\n"
                        + "2) Inferring ancestors from parent relationships:\n"
                        + "hasParent(?x, ?y) -> hasAncestor(?x, ?y);\n"
                        + "hasParent(?x, ?y) ^ hasAncestor(?y, ?z) -> hasAncestor(?x, ?z)\n"
                        + "\n\n"

                        + "OUTPUT FORMAT (very important):\n"
                        + "• Your final answer must consist ONLY of one or more SWRL rules inside a single fenced code block:\n"
                        + "Example of the output structure:\n" +
                        "\n1. Explanation of Rule1\n"
                        + "SWRL Rule1\n"
                        + "\n2. Explanation of Rule2\n"
                        + "SWRL Rule2\n"
                        + "• include any surrounding explanation, bullets, headings, or comments.\n"
                        + "• Do NOT include the TTL in the output.\n";

        String ttlPart = (req.getTtlContent() != null && !req.getTtlContent().isBlank())
                ? "\n\nTTL excerpt:\n```ttl\n" + truncate(req.getTtlContent(), 8000) + "\n```"
                : "";

        String prompt = system
                + "\n\nSWRL rule:\n```swrl\n" + safe(req.getSwrlRule()) + "\n```"
                + ttlPart
                + "\n\nUser question/comment:\n" + safe(req.getUserMessage());

        String body = "{\"model\":\"gemma3:latest\",\"prompt\":%s,\"stream\":false}".formatted(jsonString(prompt));

        HttpClient http = HttpClient.newHttpClient();
        HttpRequest httpReq = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:11434/api/generate"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();

        HttpResponse<String> resp = http.send(httpReq, HttpResponse.BodyHandlers.ofString());

        // Ollama response contains a "response" field with the textw
        String answer = new com.fasterxml.jackson.databind.json.JsonMapper()
                .readTree(resp.body()).path("response").asText("");


        String payload = gson.toJson(answer);

        logger.info("PAYLOAD: " + payload);

        return Response.ok(answer)
                .type(MediaType.TEXT_PLAIN)
                .build();
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
