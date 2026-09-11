package com.felipejaner.quotes.submission;

import com.felipejaner.quotes.error.InsurerUnavailableException;
import java.io.IOException;
import java.net.URI;
import java.net.http.*;
import java.time.Duration;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class HttpInsurerGateway implements InsurerGateway {
  private final HttpClient client;
  private final URI endpoint;
  private final Duration timeout;

  public HttpInsurerGateway(
      @Value("${quotes.insurer.url}") URI endpoint,
      @Value("${quotes.insurer.timeout}") Duration timeout) {
    this.endpoint = endpoint;
    this.timeout = timeout;
    this.client =
        HttpClient.newBuilder()
            .connectTimeout(timeout)
            .followRedirects(HttpClient.Redirect.NEVER)
            .build();
  }

  @Override
  public void submit(UUID quoteId) {
    // The public stand-in receives no personal/health information.
    var request =
        HttpRequest.newBuilder(endpoint)
            .timeout(timeout)
            .header("Idempotency-Key", quoteId.toString())
            .GET()
            .build();
    try {
      var response = client.send(request, HttpResponse.BodyHandlers.discarding());
      if (response.statusCode() < 200 || response.statusCode() >= 300)
        throw new InsurerUnavailableException(
            "The insurer returned HTTP "
                + response.statusCode()
                + ". Your quote is saved; retry submission.");
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      throw new InsurerUnavailableException(
          "Submission was interrupted. Your quote is saved; retry submission.");
    } catch (IOException e) {
      throw new InsurerUnavailableException(
          "The insurer could not be reached in time. Your quote is saved; retry submission.");
    }
  }
}
