package com.zyq.chirp.common.mq.model;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.zyq.chirp.common.mq.enums.DefaultOperation;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Builder
@Data
@AllArgsConstructor
@NoArgsConstructor
@JsonTypeInfo(use = JsonTypeInfo.Id.CLASS, property = "@class")
public class Action<T, U> {
    private String actionType;
    private String operation;
    /**
     * 执行倍数，该操作应被执行几次
     */
    private Integer multi = 1;
    @JsonTypeInfo(use = JsonTypeInfo.Id.CLASS, property = "@class")
    private T operator;
    @JsonTypeInfo(use = JsonTypeInfo.Id.CLASS, property = "@class")
    private U target;
    private long actionTime;

    public Action(String actionType, String operation, T operator, U target, long actionTime) {
        this.actionType = actionType;
        this.operation = operation;
        this.operator = operator;
        this.target = target;
        this.actionTime = actionTime;
    }

    public static int getIncCount(List<? extends Action<?, ?>> actions) {
        var ref = new Object() {
            int counter = 0;
        };
        actions.forEach(action -> {
            if (DefaultOperation.INCREMENT.getOperation().equals(action.getOperation())) {
                ref.counter += action.getMulti();
            }
            if (DefaultOperation.DECREMENT.getOperation().equals(action.getOperation())) {
                ref.counter -= action.getMulti();
            }
        });
        return ref.counter;
    }
}
