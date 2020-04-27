package org.worldbuild.mongo.domain.entity;

import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.TypeAlias;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.Date;

@Document(collection = "db_con")
@TypeAlias("Connection")
public class Connection {

    @Id
    private String id;
    private int current;
    private int available;
    private int totalCreated;
    @CreatedDate
    private Date add;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public int getCurrent() {
        return current;
    }

    public void setCurrent(int current) {
        this.current = current;
    }

    public int getAvailable() {
        return available;
    }

    public void setAvailable(int available) {
        this.available = available;
    }

    public int getTotalCreated() {
        return totalCreated;
    }

    public void setTotalCreated(int totalCreated) {
        this.totalCreated = totalCreated;
    }

    public Date getAdd() {
        return add;
    }

    public void setAdd(Date add) {
        this.add = add;
    }
}
