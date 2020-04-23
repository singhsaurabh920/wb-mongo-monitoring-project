package org.worldbuild.project.domain.entity;

import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.TypeAlias;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import java.util.Date;

@Document(collection = "db_net")
@TypeAlias("netCounter")
public class NetCounter {

    @Id
    private String id;
    @Field("in")
    private long in;
    @Field("out")
    private long out;
    @Field("req")
    private long request;
    @Field("t_in")
    private long totalIn;
    @Field("t_out")
    private long totalOut;
    @Field("t_req")
    private long totalRequest;
    @CreatedDate
    private Date add;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public long getIn() {
        return in;
    }

    public void setIn(long in) {
        this.in = in;
    }

    public long getOut() {
        return out;
    }

    public void setOut(long out) {
        this.out = out;
    }

    public long getTotalIn() {
        return totalIn;
    }

    public void setTotalIn(long totalIn) {
        this.totalIn = totalIn;
    }

    public long getTotalOut() {
        return totalOut;
    }

    public void setTotalOut(long totalOut) {
        this.totalOut = totalOut;
    }

    public long getRequest() {
        return request;
    }

    public void setRequest(long request) {
        this.request = request;
    }

    public long getTotalRequest() {
        return totalRequest;
    }

    public void setTotalRequest(long totalRequest) {
        this.totalRequest = totalRequest;
    }

    public Date getAdd() {
        return add;
    }

    public void setAdd(Date add) {
        this.add = add;
    }
}
