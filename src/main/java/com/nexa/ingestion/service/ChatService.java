package com.nexa.ingestion.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

/**
 * Chat service with mock streaming responses.
 * Simulates ChatGPT-style streaming with chunk-by-chunk delivery.
 */
@Service
public class ChatService {

    private static final Logger log = LoggerFactory.getLogger(ChatService.class);
    private static final Random random = new Random();

    // Mock responses for different query types
    private static final Map<String, String> MOCK_RESPONSES = new HashMap<>();

    static {
        MOCK_RESPONSES.put("80c", 
            "Section 80C is a part of the Indian Income Tax Act that allows individuals to reduce their taxable income by investing in certain instruments. " +
            "You can claim deductions up to ₹1.5 lakh per financial year. " +
            "Eligible investments include Public Provident Fund (PPF), Employee Provident Fund (EPF), Equity Linked Savings Scheme (ELSS), " +
            "National Savings Certificate (NSC), Tax-Saving Fixed Deposits, and life insurance premiums. " +
            "This deduction helps reduce your overall tax liability while encouraging savings and investments.");
        
        MOCK_RESPONSES.put("onboarding", 
            "The onboarding process typically includes several key steps. " +
            "First, you'll receive your welcome email with access credentials and initial documentation. " +
            "Then, you'll complete your profile setup, including personal information and emergency contacts. " +
            "Next, you'll attend orientation sessions covering company policies, culture, and your role. " +
            "You'll also meet with your manager and team members, set up your workspace, and receive necessary equipment. " +
            "Finally, you'll begin your first assignments with guidance from your mentor.");
        
        MOCK_RESPONSES.put("leave", 
            "To request time off, log into the HR portal and navigate to the Leave Management section. " +
            "Select the type of leave (casual, sick, or earned), choose your dates, and provide a brief reason. " +
            "Submit the request, and your manager will be notified for approval. " +
            "You can track the status of your request in the portal. " +
            "Remember to submit requests at least 48 hours in advance for planned leave.");
    }

    /**
     * Generates a streaming mock response for the given query.
     * Simulates ChatGPT-style typing with chunks delivered every 100-200ms.
     *
     * @param query user's query
     * @return Flux of text chunks to stream
     */
    public Flux<String> streamResponse(String query) {
        if (query == null || query.isBlank()) {
            return Flux.just("Please provide a valid query.");
        }

        log.info("Generating mock streaming response for query: {}", query);

        // Select appropriate mock response based on query keywords
        String fullResponse = selectMockResponse(query.toLowerCase());

        // Split response into words for natural chunking
        String[] words = fullResponse.split(" ");
        
        return Flux.fromArray(words)
                .index()
                .concatMap(tuple -> {
                    long index = tuple.getT1();
                    String word = tuple.getT2();
                    
                    // Add space after word (except last word)
                    String chunk = word + (index < words.length - 1 ? " " : "");
                    
                    // Simulate variable typing speed (100-200ms per chunk)
                    int delayMs = 100 + random.nextInt(100);
                    
                    return Flux.just(chunk)
                            .delayElements(Duration.ofMillis(delayMs));
                })
                .doOnComplete(() -> log.debug("Finished streaming response"))
                .doOnError(e -> log.error("Error streaming response", e));
    }

    /**
     * Selects an appropriate mock response based on query keywords.
     */
    private String selectMockResponse(String queryLower) {
        // Check for specific keywords
        if (queryLower.contains("80c") || queryLower.contains("section 80c") || queryLower.contains("tax deduction")) {
            return MOCK_RESPONSES.get("80c");
        }
        if (queryLower.contains("onboarding") || queryLower.contains("onboard") || queryLower.contains("new employee")) {
            return MOCK_RESPONSES.get("onboarding");
        }
        if (queryLower.contains("leave") || queryLower.contains("time off") || queryLower.contains("vacation")) {
            return MOCK_RESPONSES.get("leave");
        }
        
        // Default generic response
        return generateGenericResponse(queryLower);
    }

    /**
     * Generates a generic mock response for unknown queries.
     */
    private String generateGenericResponse(String query) {
        return String.format(
            "Thank you for your question about '%s'. " +
            "This is a mock response demonstrating streaming capabilities. " +
            "In a production environment, this would be generated by an LLM with access to your knowledge base. " +
            "The streaming response allows for a ChatGPT-like experience with real-time text delivery. " +
            "Each chunk is sent as it becomes available, creating a natural typing effect.",
            query
        );
    }
}
