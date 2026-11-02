package com.softlaunch.safety.repository;

import com.softlaunch.safety.model.UserSafetyStatus;
import org.springframework.data.mongodb.repository.MongoRepository;


public interface UserSafetyStatusRepository extends MongoRepository<UserSafetyStatus, String> { }
