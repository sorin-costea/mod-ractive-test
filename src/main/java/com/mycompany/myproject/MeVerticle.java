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
		final EventBus eb = vertx.eventBus();
		Handler<Message<String>> myHandler = new Handler<Message<String>>() {
			public void handle(Message<String> message) {
				System.out.println("I received a message " + message.body());
		    message.reply("Heres a reply: " + message.body());
			}
		};
		eb.registerHandler("test.address", myHandler);

		JsonObject config = container.config();
		container.deployModule("io.vertx~mod-web-server~2.0.0-final", config.getObject("web-server"));
		container.deployModule("io.vertx~mod-mongo-persistor~2.1.0", config.getObject("mongo-persistor"), deployHandler);
		System.out.println("started");
	};

	private void loadSampleData() {
		final EventBus eb = vertx.eventBus();
		JsonObject cmd = new JsonObject();
		cmd.putString("action", "count");
		cmd.putString("collection", "comments");
		cmd.putObject("matcher", new JsonObject());

		eb.send("vertx.mongopersistor", cmd,
		    new Handler<Message<JsonObject>>() {
			    public void handle(Message<JsonObject> message) {
				    if (0 != "ok".compareTo(message.body().getString("status"))
				        || 0 == message.body().getInteger("count")) {

				    	JsonObject cmd = new JsonObject();
					    cmd.putString("action", "save");
					    cmd.putString("collection", "comments");

					    Buffer jsonBuf = vertx.fileSystem().readFileSync("static_data.json");
					    JsonObject staticData = new JsonObject(jsonBuf.toString());
					    for (int i = 0; i < staticData.getArray("comments").size(); i++) {
						    cmd.putValue("document", staticData.getArray("comments").get(i));
						    eb.send("vertx.mongopersistor", cmd,
						        new Handler<Message<JsonObject>>() {
							        public void handle(Message<JsonObject> message) {
								        System.out.println("saved test comment: " + message.body().getString("status", "wot?"));
							        }
						        });
					    }

				    }
			    }
		    });

	}
}
