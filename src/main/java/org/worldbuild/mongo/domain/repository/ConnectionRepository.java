package org.worldbuild.mongo.domain.repository;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;
import org.worldbuild.mongo.domain.entity.Connection;

@Repository
public interface ConnectionRepository extends MongoRepository<Connection,String> {
}
