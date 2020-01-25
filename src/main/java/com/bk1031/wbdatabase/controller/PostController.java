package com.bk1031.wbdatabase.controller;

import com.bk1031.wbdatabase.model.Event;
import com.bk1031.wbdatabase.model.Post;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.Message;
import com.google.firebase.messaging.Notification;
import com.google.gson.Gson;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Timestamp;
import java.util.ArrayList;

import static spark.Spark.*;

public class PostController {
    private Connection db;

    Gson gson = new Gson();

    public PostController(Connection db) {
        this.db = db;
        getAllPosts();
        getPost();
        updatePost();
        createPost();
    }

    private void getAllPosts() {
        get("/api/posts", (request, response) -> {
            ArrayList<Post> returnList = new ArrayList<>();
            String sql = "SELECT * FROM \"post\"";
            ResultSet rs = db.createStatement().executeQuery(sql);
            while(rs.next()) {
                Post post = new Post();
                post.setId(rs.getString("id"));
                post.setBody(rs.getString("body"));
                post.setDate(rs.getTimestamp("date").toString());
                post.setTitle(rs.getString("title"));
                returnList.add(post);
            }
            rs.close();
            return returnList;
        });
    }

    private void getPost() {
        get("/api/posts/:id", (request, response) -> {
            // Get post
            Post post = new Post();
            String sql = "SELECT * FROM \"post\" WHERE id='" + request.params(":id") + "'";
            ResultSet rs = db.createStatement().executeQuery(sql);
            while (rs.next()) {
                post.setId(rs.getString("id"));
                post.setBody(rs.getString("body"));
                post.setDate(rs.getTimestamp("date").toString());
                post.setTitle(rs.getString("title"));
            }
            rs.close();
            if (post.toString().contains("null")) {
                response.status(404);
                response.type("application/json");
                response.body("{\"message\": \"Requested post not found\"}");
                return response;
            }
            response.type("application/json");
            response.body(post.toString());
            return response;
        });
    }

    private void deletePost(){

    }

    private void createPost() {
        post("/api/posts", (req, res) -> {
            Post post = gson.fromJson(req.body(), Post.class);
            System.out.println("PARSED POST: " + post);
            if (post.toString().contains("null")) {
                res.status(400);
                res.type("application/json");
                res.body("{\"message\": \"Request missing or contains null values\"}");
                return res;
            }
            String existsSql = "SELECT COUNT(1) FROM \"post\" WHERE id = '" + post.getId() + "'";
            ResultSet rs = db.createStatement().executeQuery(existsSql);
            while (rs.next()) {
                if (rs.getInt("count") == 1) {
                    res.status(409);
                    res.type("application/json");
                    res.body("{\"message\": \"Post already exists with id: " + post.getId() + "\"}");
                    return res;
                }
            }
            String sql = "INSERT INTO \"post\" VALUES " +
                    "(" +
                    "'" + post.getId() + "'," +
                    "'" + post.getTitle() + "'," +
                    "'" + post.getDate() + "'," +
                    "'" + post.getBody() + "'" +
                    ")";
            db.createStatement().executeUpdate(sql);
            db.commit();
            System.out.println("Inserted records into the table...");
            FirebaseMessaging.getInstance().send(Message.builder().setTopic("ALL_DEVICES").setNotification(Notification.builder().setTitle("[NEW ANNOUNCEMENT] " + post.getTitle()).setBody(post.getBody()).build()).build());
            res.type("application/json");
            res.body(post.toString());
            return res;
        });
    }

    private void updatePost() {
        put("/api/posts/:id", (req, res) -> {
            Post post = gson.fromJson(req.body(), Post.class);
            post.setId(req.params(":id"));
            System.out.println("PARSED POST: " + post);
            if(post.toString().contains("null")){
                res.status(400);
                res.type("application/json");
                res.body("{\"message\": \"Request missing or contains null values\"}");
                return res;
            }
            String existsSql = "SELECT COUNT(1) FROM \"post\" WHERE id = '" + post.getId() + "'";
            ResultSet rs = db.createStatement().executeQuery(existsSql);
            while (rs.next()){
                if (rs.getInt("count") != 1){
                    res.status(400);
                    res.type("application/json");
                    res.body("{\"message\": \"No mapping for given id: " + post.getId() + "\"}");
                    return res;
                }
            }
            String sql = "UPDATE \"post\" SET " +
                    "body='" + post.getBody() + "'," +
                    "date='" + post.getDate() + "'," +
                    "title='" + post.getTitle() + "'," +
                    "WHERE id='" + post.getId() + "'";
            db.createStatement().executeUpdate(sql);
            db.commit();
            System.out.println("Inserted records into the table...");
            res.type("application/json");
            res.body(post.toString());
            return res;
        });
    }
}
