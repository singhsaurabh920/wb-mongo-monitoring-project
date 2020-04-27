package org.worldbuild.mongo.domain.entity;

import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.TypeAlias;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import java.util.Date;

@Document(collection = "db_op")
@TypeAlias("opCounter")
public class OpCounter {

    @Id
    private String id;
    @Field("qry")
    private long query;
    @Field("ins")
    private long insert;
    @Field("upd")
    private long update;
    @Field("del")
    private long delete;
    @Field("get")
    private long getmore;
    @Field("cmd")
    private long command;
    @Field("t_qry")
    private long totalQuery;
    @Field("t_ins")
    private long totalInsert;
    @Field("t_upd")
    private long totalUpdate;
    @Field("t_del")
    private long totalDelete;
    @Field("t_get")
    private long totalGetmore;
    @Field("t_cmd")
    private long totalCommand;
    @CreatedDate
    private Date add;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public long getQuery() {
        return query;
    }

    public void setQuery(long query) {
        this.query = query;
    }

    public long getInsert() {
        return insert;
    }

    public void setInsert(long insert) {
        this.insert = insert;
    }

    public long getUpdate() {
        return update;
    }

    public void setUpdate(long update) {
        this.update = update;
    }

    public long getDelete() {
        return delete;
    }

    public void setDelete(long delete) {
        this.delete = delete;
    }

    public long getGetmore() {
        return getmore;
    }

    public void setGetmore(long getmore) {
        this.getmore = getmore;
    }

    public long getCommand() {
        return command;
    }

    public void setCommand(long command) {
        this.command = command;
    }

    public long getTotalQuery() {
        return totalQuery;
    }

    public void setTotalQuery(long totalQuery) {
        this.totalQuery = totalQuery;
    }

    public long getTotalInsert() {
        return totalInsert;
    }

    public void setTotalInsert(long totalInsert) {
        this.totalInsert = totalInsert;
    }

    public long getTotalUpdate() {
        return totalUpdate;
    }

    public void setTotalUpdate(long totalUpdate) {
        this.totalUpdate = totalUpdate;
    }

    public long getTotalDelete() {
        return totalDelete;
    }

    public void setTotalDelete(long totalDelete) {
        this.totalDelete = totalDelete;
    }

    public long getTotalGetmore() {
        return totalGetmore;
    }

    public void setTotalGetmore(long totalGetmore) {
        this.totalGetmore = totalGetmore;
    }

    public long getTotalCommand() {
        return totalCommand;
    }

    public void setTotalCommand(long totalCommand) {
        this.totalCommand = totalCommand;
    }

    public Date getAdd() {
        return add;
    }

    public void setAdd(Date add) {
        this.add = add;
    }
}
