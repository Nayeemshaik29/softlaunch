package com.softlaunch.chat.repository;

import com.softlaunch.chat.model.Message;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface MessageRepository extends MongoRepository<Message, String> {

    List<Message> findTop50ByMatchIdOrderBySentAtDesc(String matchId);
}