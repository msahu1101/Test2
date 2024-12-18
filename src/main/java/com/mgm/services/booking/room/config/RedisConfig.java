package com.mgm.services.booking.room.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.JdkSerializationRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import com.mgm.services.common.model.RedemptionValidationResponse;

@ConditionalOnProperty(
        prefix = "spring",
        name = "session.store-type",
        havingValue = "redis")
@Configuration
public class RedisConfig {
    
    @Autowired
    RedisConnectionFactory connectionFactory;

    @Bean
    public RedisTemplate<String, RedemptionValidationResponse> redisTemplate() {
        final RedisTemplate<String, RedemptionValidationResponse> redisTemplate = new RedisTemplate<>();
        redisTemplate.setConnectionFactory(connectionFactory);
        RedisSerializer<Object> serializer = new JdkSerializationRedisSerializer(this.getClass().getClassLoader());
        redisTemplate.setValueSerializer(serializer);
        redisTemplate.setDefaultSerializer(serializer);
        redisTemplate.setHashValueSerializer(serializer);
        redisTemplate.setHashKeySerializer(serializer);
        redisTemplate.setKeySerializer(new StringRedisSerializer());
        redisTemplate.afterPropertiesSet();
        return redisTemplate;
    }
}