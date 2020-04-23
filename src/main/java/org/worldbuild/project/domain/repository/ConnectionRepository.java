package org.worldbuild.project.domain.repository;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;
import org.worldbuild.project.domain.entity.Connection;

@Repository
public interface ConnectionRepository extends MongoRepository<Connection,String> {
}
