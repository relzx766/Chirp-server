package com.zyq.chirp.common.redis.util;

import com.zyq.chirp.common.redis.command.BloomCommand;
import com.zyq.chirp.common.redis.output.BooleanListOutput;
import io.lettuce.core.cluster.RedisClusterClient;
import io.lettuce.core.cluster.api.StatefulRedisClusterConnection;
import io.lettuce.core.cluster.api.sync.RedisAdvancedClusterCommands;
import io.lettuce.core.codec.StringCodec;
import io.lettuce.core.output.BooleanOutput;
import io.lettuce.core.output.StatusOutput;
import io.lettuce.core.protocol.CommandArgs;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class BloomUtil {

    private static final String OK = "OK";
    @Resource
    RedisClusterClient clusterClient;
    private StatefulRedisClusterConnection<String, String> connection;
    private RedisAdvancedClusterCommands<String, String> commands;

    @PostConstruct
    public void init() {
        connection = clusterClient.connect();
        commands = connection.sync();
    }

    @PreDestroy
    public void cleanup() {
        connection.close();
    }


    /**
     * 创建一个新的空布隆过滤器
     *
     * @param key          key
     * @param initCapacity 初始化容量
     * @param errorRate    容错率
     * @return
     */
    public Boolean createFilter(String key, long initCapacity, double errorRate) {
        StatusOutput<String, String> output = new StatusOutput<>(StringCodec.UTF8);
        commands.dispatch(BloomCommand.BF_RESERVE, output,
                new CommandArgs<>(StringCodec.UTF8)
                        .addKey(key)
                        .addValue(String.valueOf(errorRate))
                        .addValue(String.valueOf(initCapacity)));
        return output.get().equals(OK);
    }

    public Boolean add(String key, String value) {
        BooleanOutput<String, String> output = new BooleanOutput<>(StringCodec.UTF8);
        commands.dispatch(BloomCommand.BF_ADD, output,
                new CommandArgs<>(StringCodec.UTF8)
                        .addKey(key)
                        .addValue(value));
        return output.get();
    }

    public List<Boolean> multiAdd(String key, String... values) {
        BooleanListOutput<String, String> output = new BooleanListOutput<>(StringCodec.UTF8);
        commands.dispatch(BloomCommand.BF_MADD, output,
                new CommandArgs<>(StringCodec.UTF8)
                        .addKey(key)
                        .addValues(values));
        return output.get();
    }

    public Boolean exists(String key, String values) {
        BooleanOutput<String, String> output = new BooleanOutput<>(StringCodec.UTF8);
        commands.dispatch(BloomCommand.BF_EXISTS, output,
                new CommandArgs<>(StringCodec.UTF8)
                        .addKey(key)
                        .addValue(values));
        return output.get();
    }

    public List<Boolean> mExists(String key, String... values) {
        BooleanListOutput<String, String> output = new BooleanListOutput<>(StringCodec.UTF8);
        commands.dispatch(BloomCommand.BF_MEXISTS, output,
                new CommandArgs<>(StringCodec.UTF8)
                        .addKey(key)
                        .addValues(values));
        return output.get();
    }
}
