package com.zyq.chirp.common.redis.output;

import io.lettuce.core.codec.RedisCodec;
import io.lettuce.core.output.CommandOutput;

import java.util.ArrayList;
import java.util.List;

public class BooleanListOutput<K, V> extends CommandOutput<K, V, List<Boolean>> {
    public BooleanListOutput(RedisCodec<K, V> codec) {
        super(codec, new ArrayList<>());
    }

    @Override
    public void set(long integer) {
        output.add(integer == 1);
    }

    @Override
    public void set(boolean value) {
        output.add(value);
    }
}
