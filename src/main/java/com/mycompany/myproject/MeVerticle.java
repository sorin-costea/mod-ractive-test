package com.mycompany.myproject;

import org.vertx.java.core.AsyncResult;
import org.vertx.java.core.AsyncResultHandler;
import org.vertx.java.core.Handler;
import org.vertx.java.core.buffer.Buffer;
import org.vertx.java.core.eventbus.EventBus;
import org.vertx.java.core.eventbus.Message;
import org.vertx.java.core.json.JsonObject;
import org.vertx.java.platform.Verticle;

/*
 */
public class MeVerticle extends Verticle {

  private static String CFG_APPLICATION = "app-config";
  private static String CFG_WEBSERVER = "web-server";
  private static String CFG_SEARCHER = "elasticsearch";

  private AsyncResultHandler<String> deployHandler = new AsyncResultHandler<String>() {
    public void handle(AsyncResult<String> asyncResult) {
      if (asyncResult.succeeded()) {
        loadSampleData();
      } else {
        asyncResult.cause().printStackTrace();
      }
    }
  };

  public void start() {
    JsonObject config = container.config();
    container.deployModule("io.vertx~mod-web-server~2.0.0-final", config.getObject(CFG_WEBSERVER));
    container.deployModule("com.englishtown~vertx-mod-elasticsearch~1.2.0", config.getObject(CFG_SEARCHER),
        deployHandler);
  };

  private void loadSampleData() {
    final EventBus eb = vertx.eventBus();
    String index = container.config().getObject(CFG_APPLICATION).getString("index");
    JsonObject cmd = new JsonObject();
    cmd.putString("action", "search"); // index must exist, our module can't create
    cmd.putString("_index", index);
    cmd.putString("_type", "employee");
    // not supported cmd.putBoolean("_source", false);
    JsonObject query = new JsonObject();
    query.putObject("match_all", new JsonObject());
    query.putString("ignore_indices", "missing");
    cmd.putObject("query", query);

    String searcher = container.config().getObject(CFG_SEARCHER).getString("address");
    eb.send(searcher, cmd,
        new Handler<Message<JsonObject>>() {
          public void handle(Message<JsonObject> message) {

            if (0 != "ok".compareTo(message.body().getString("error", "ok"))
                || 0 == message.body().getObject("hits").getInteger("total")) {

              System.out.println("---because: " + message.body().getString("error", "no error"));
              System.out.println("---because: " + message.body().getObject("hits").getInteger("total"));
              String index = container.config().getObject(CFG_APPLICATION).getString("index");
              JsonObject cmd = new JsonObject();
              cmd.putString("action", "index");
              cmd.putString("_index", index);
              cmd.putString("_type", "employee");

              String searcher = container.config().getObject(CFG_SEARCHER).getString("address");
              Buffer jsonBuf = vertx.fileSystem().readFileSync("static_data.json");
              JsonObject staticData = new JsonObject(jsonBuf.toString());
              
              // could have used bulk but I hate that formatting
              for (int i = 0; i < staticData.getArray("employee").size(); i++) {
                cmd.putString("_id", String.valueOf(i));
                cmd.putObject("_source", (JsonObject) staticData.getArray("employee").get(i));
                eb.send(searcher, cmd,
                    new Handler<Message<JsonObject>>() {
                      public void handle(Message<JsonObject> message) {
                        System.out.println("saved employee: " + message.body().getString("error", "ok"));
                      }
                    });
              }

            }
          }
        });

  }
}