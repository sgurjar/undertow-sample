package testing;

import java.nio.charset.StandardCharsets;
import java.time.DateTimeException;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Arrays;
import java.util.Map;
import java.util.TimeZone;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import io.undertow.Undertow;
import io.undertow.server.HttpHandler;
import io.undertow.server.RoutingHandler;
import io.undertow.util.Headers;
import io.undertow.util.PathTemplateMatch;
import io.undertow.util.StatusCodes;

public class SampleApi {
  private static final Logger log = LoggerFactory.getLogger(SampleApi.class);

  public static void main(String[] args) {
    int    port = args.length > 0 ? Integer.parseInt(args[0]) : 8080;
    String host = args.length > 1 ? args[1] : "0.0.0.0";

    RoutingHandler routes = new RoutingHandler();
    routes.add("GET" , "/api/v1/list"         , listTimeZoneIds       ());
    routes.add("POST", "/api/v1/convert"      , convertCurrentDatetime());
    routes.add("GET" , "/api/v1/get/{zoneid}" , getCurrentDatetime    ());

    Undertow server = Undertow.builder()
        .setIoThreads(4)
        .setWorkerThreads(16)
        .addHttpListener(port, host)
        .setHandler(routes)
        .build();

    Runtime.getRuntime().addShutdownHook(new Thread(server::stop, "shutdownhook"));

    server.start();
  }

  static HttpHandler listTimeZoneIds() {
    return exchange -> {
      log.debug("received list request {}->{} ", exchange.getSourceAddress(), exchange.getDestinationAddress());

      exchange.getResponseHeaders().put(Headers.CONTENT_TYPE, "text/json");
      exchange.getResponseSender().send(
          new JSONObject().put("zoneids", Arrays.asList(TimeZone.getAvailableIDs())).toString(),
          StandardCharsets.UTF_8);
    };
  }

  static HttpHandler convertCurrentDatetime() {
    return exchange -> {
      log.debug("received convert request for {}->{} ", exchange.getSourceAddress(), exchange.getDestinationAddress());

      exchange.getRequestReceiver().receiveFullString((xch, msg) -> {
        try {
          JSONObject req = new JSONObject(msg);
          String fromtz = req.getString("from");
          String totz = req.getString("to");
          ZonedDateTime from = ZonedDateTime.now(ZoneId.of(fromtz, ZoneId.SHORT_IDS));
          ZonedDateTime to = from.withZoneSameInstant(ZoneId.of(totz, ZoneId.SHORT_IDS));
          JSONObject resp = new JSONObject();
          resp.put("from", from.toString());
          resp.put("to", to.toString());
          xch.getResponseHeaders().put(Headers.CONTENT_TYPE, "text/json");
          xch.getResponseSender().send(resp.toString());
        } catch (DateTimeException e) {
          xch.setStatusCode(StatusCodes.BAD_REQUEST);
          xch.getResponseHeaders().put(Headers.CONTENT_TYPE, "text/json");
          xch.getResponseSender().send(new JSONObject().put("error", e.getMessage()).toString());
        }
      }, StandardCharsets.UTF_8);
    };
  }

  static HttpHandler getCurrentDatetime() {
    return exchange -> {
      log.debug("received current request for {}->{} ", exchange.getSourceAddress(), exchange.getDestinationAddress());

      try {
        PathTemplateMatch match = exchange.getAttachment(PathTemplateMatch.ATTACHMENT_KEY);
        Map<String, String> parameters = match.getParameters();
        log.info("parameters={}", parameters);
        String zoneid = parameters.get("zoneid").toUpperCase();
        ZonedDateTime now = ZonedDateTime.now(ZoneId.of(zoneid, ZoneId.SHORT_IDS));
        exchange.getResponseHeaders().put(Headers.CONTENT_TYPE, "text/json");
        exchange.getResponseSender().send(new JSONObject().put("now", now.toString()).toString());
      } catch (Exception e) {
        exchange.setStatusCode(StatusCodes.BAD_REQUEST);
        exchange.getResponseHeaders().put(Headers.CONTENT_TYPE, "text/json");
        exchange.getResponseSender().send(new JSONObject().put("error", e.getMessage()).toString());
      }
    };
  }
}
