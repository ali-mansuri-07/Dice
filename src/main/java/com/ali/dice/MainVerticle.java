package com.ali.dice;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Promise;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.BodyHandler;
import io.vertx.mysqlclient.MySQLConnectOptions;
import io.vertx.sqlclient.Pool;
import io.vertx.sqlclient.PoolOptions;
import io.vertx.sqlclient.Row;
import io.vertx.sqlclient.RowSet;
import io.vertx.sqlclient.Tuple;

public class MainVerticle extends AbstractVerticle {

  private Pool client;

  @Override
  public void start(Promise<Void> startPromise) throws Exception {
    Router router = Router.router(vertx);
    router.route().handler(BodyHandler.create());

    vertx.createHttpServer().requestHandler(router).listen(9999).onComplete(res -> {
      if (res.succeeded()) {
        System.out.println("Server started...");
      } else {
        System.out.println("Error Occurred : " + res.cause().getMessage());
      }
    });

    MySQLConnectOptions connect = new MySQLConnectOptions();
    connect.setHost("localhost").setPort(3306).setDatabase("dice").setUser("root").setPassword("Ali@2002");

    PoolOptions poolOptions = new PoolOptions().setMaxSize(5);

    client = Pool.pool(vertx, connect, poolOptions);

    client.query("SELECT 1").execute(res -> {
      if (res.succeeded()) {
        System.out.println("Database Connection Successfull");
      } else {
        System.out.println("Error Occured : " + res.cause().getMessage());
      }
    });

    router.post("/signup").handler(this::handleSignup);
    router.post("/login").handler(this::handleLogin);
  }

  public void handleSignup(RoutingContext context) {
    JsonObject user = context.getBodyAsJson();
    String name = user.getString("name");
    String password = user.getString("password");

    if (isPalindromicSubstring(name, password)) {
      client.preparedQuery("INSERT INTO users(name, password) VALUES(?, ?)").execute(Tuple.of(name, password), res -> {
        if (res.succeeded()) {
          context.response().end("User Entered Successfully");
        } else {
          System.out.println("Error in saving user : " + res.cause().getMessage());
        }
      });
    } else {
      context.response().end("Password should be palindromic substring of name");
    }

  }

  private boolean isPalindrome(String str, int i, int j) {
    while (i <= j) {
      if (str.charAt(i) != str.charAt(j)) {
        return false;
      }
      i++;
      j--;
    }
    return true;
  }

  private boolean isPalindromicSubstring(String name, String password) {
    if (name.contains(password) && isPalindrome(password, 0, password.length() - 1)) {
      return true;
    }
    return false;
  }

  public void handleLogin(RoutingContext context) {
    JsonObject user = context.getBodyAsJson();
    String name = user.getString("name");
    String password = user.getString("password");

    client.preparedQuery("SELECT * FROM users WHERE name = ?").execute(Tuple.of(name), res -> {
      RowSet<Row> rows = res.result();
      for (Row row : rows) {
        String testName = row.getString("name");
        String testPassword = row.getString("password");
        if (name.equals(testName) && password.equals(testPassword)) {
          context.response().end("User Logged In Successfully");
        }
      }
    });

  }

  public static void main(String[] args) {
    Vertx vertx = Vertx.vertx();
    vertx.deployVerticle(new MainVerticle());
  }
}
