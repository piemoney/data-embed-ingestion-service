# Chat API Documentation

Streaming chat API with mock responses, ready for LLM integration.

## Endpoints

### POST /api/chat

Streams chat responses using Server-Sent Events (SSE), similar to ChatGPT.

**Request:**
```json
{
  "query": "What is Section 80C in India?"
}
```

**Response:** Server-Sent Events stream with text chunks

**Example curl:**
```bash
curl -N -X POST http://localhost:8080/api/chat \
  -H "Content-Type: application/json" \
  -d '{"query": "What is Section 80C?"}'
```

**Response Format (SSE):**
```
data: Section 
data: 80C 
data: is 
data: a 
data: part 
...
event: done
data: [DONE]
```

### POST /api/chat/text

Alternative endpoint returning plain text stream (without SSE wrapper).

**Example curl:**
```bash
curl -N -X POST http://localhost:8080/api/chat/text \
  -H "Content-Type: application/json" \
  -d '{"query": "What is Section 80C?"}'
```

**Response:** Plain text stream
```
Section 80C is a part of the Indian Income Tax Act...
```

## Features

- ✅ **Streaming Responses**: Chunk-by-chunk delivery every 100-200ms
- ✅ **ChatGPT-style Typing**: Simulates natural typing effect
- ✅ **Mock Responses**: Pre-configured responses for testing
- ✅ **Server-Sent Events**: Standard SSE format for frontend integration
- ✅ **Reactive**: Built on Spring WebFlux for high performance
- ✅ **Production-Ready**: Error handling, logging, validation

## Mock Responses

The service includes mock responses for:

- **Section 80C / Tax**: Information about Indian tax deductions
- **Onboarding**: Employee onboarding process
- **Leave / Time Off**: How to request time off
- **Generic**: Default response for other queries

## Frontend Integration

### JavaScript (EventSource)

```javascript
const eventSource = new EventSource('http://localhost:8080/api/chat', {
  method: 'POST',
  headers: {
    'Content-Type': 'application/json',
  },
  body: JSON.stringify({ query: 'What is Section 80C?' })
});

eventSource.onmessage = (event) => {
  if (event.data === '[DONE]') {
    eventSource.close();
    return;
  }
  // Append chunk to UI
  appendToChat(event.data);
};

eventSource.onerror = (error) => {
  console.error('SSE error:', error);
  eventSource.close();
};
```

### JavaScript (Fetch with Stream)

```javascript
async function streamChat(query) {
  const response = await fetch('http://localhost:8080/api/chat/text', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
    },
    body: JSON.stringify({ query })
  });

  const reader = response.body.getReader();
  const decoder = new TextDecoder();

  while (true) {
    const { done, value } = await reader.read();
    if (done) break;
    
    const chunk = decoder.decode(value);
    appendToChat(chunk);
  }
}
```

### React Example

```jsx
import { useState } from 'react';

function ChatComponent() {
  const [message, setMessage] = useState('');
  const [response, setResponse] = useState('');

  const handleSubmit = async (e) => {
    e.preventDefault();
    setResponse('');

    const response = await fetch('http://localhost:8080/api/chat/text', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ query: message })
    });

    const reader = response.body.getReader();
    const decoder = new TextDecoder();

    while (true) {
      const { done, value } = await reader.read();
      if (done) break;
      
      const chunk = decoder.decode(value);
      setResponse(prev => prev + chunk);
    }
  };

  return (
    <div>
      <form onSubmit={handleSubmit}>
        <input
          value={message}
          onChange={(e) => setMessage(e.target.value)}
          placeholder="Ask a question..."
        />
        <button type="submit">Send</button>
      </form>
      <div>{response}</div>
    </div>
  );
}
```

## Testing

### Using curl

```bash
# SSE endpoint
curl -N -X POST http://localhost:8080/api/chat \
  -H "Content-Type: application/json" \
  -d '{"query": "What is Section 80C?"}'

# Plain text endpoint
curl -N -X POST http://localhost:8080/api/chat/text \
  -H "Content-Type: application/json" \
  -d '{"query": "How do I request time off?"}'
```

### Using httpie

```bash
http POST http://localhost:8080/api/chat query="What is onboarding?" --stream
```

### Using Postman

1. Create POST request to `http://localhost:8080/api/chat`
2. Set body to JSON: `{"query": "Your question"}`
3. Enable "Stream" mode in Postman settings
4. Send request and watch chunks arrive

## Future LLM Integration

The service is designed to easily integrate with real LLMs:

1. **Replace ChatService.streamResponse()** with actual LLM call
2. **Use existing LLMService** (already in project) for Hugging Face integration
3. **Add RAG pipeline** by calling SearchService before LLM generation
4. **Stream LLM responses** chunk-by-chunk as they arrive

Example future implementation:

```java
public Flux<String> streamResponse(String query) {
    // 1. Get context from vector search
    return embeddingService.embed(query)
        .flatMap(vector -> searchService.search(vector, 5, 0.7))
        .flatMapMany(results -> {
            // 2. Build context from results
            String context = buildContext(results);
            
            // 3. Stream LLM response
            return llmService.streamGenerate(query, context);
        });
}
```

## Configuration

No additional configuration required. The service uses:

- Spring WebFlux for reactive streaming
- Server-Sent Events (SSE) for standard streaming protocol
- Validation for request validation
- Logging for debugging

## Performance

- **Latency**: First chunk arrives in ~100-200ms
- **Throughput**: Handles multiple concurrent streams
- **Memory**: Efficient streaming with backpressure support
- **Scalability**: Stateless design, horizontally scalable

## Error Handling

- **Invalid Request**: Returns 400 Bad Request with validation errors
- **Service Errors**: Returns error event in SSE stream
- **Network Issues**: Graceful connection closure

## Production Considerations

1. **Rate Limiting**: Add rate limiting for production
2. **Authentication**: Add JWT or API key authentication
3. **CORS**: Configure CORS for frontend domains
4. **Monitoring**: Add metrics for stream duration, chunk count
5. **Timeouts**: Configure appropriate timeouts for long-running streams
