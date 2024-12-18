package com.mgm.services.booking.room.service.cache.rediscache.service.impl;


import com.mgm.services.booking.room.service.cache.rediscache.redis.JedisFactory;
import com.mgm.services.booking.room.util.CommonUtil;
import com.mgm.services.common.exception.ErrorCode;
import com.mgm.services.common.exception.SystemException;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.Pipeline;
import redis.clients.jedis.exceptions.JedisConnectionException;

import java.util.List;
import java.util.stream.Collectors;

@Log4j2
@Component
public abstract class BaseRedisReadService {
    @Autowired
    JedisFactory  jedisFactory;

    protected String getValuesByIndex(String byType, String IdCode){
        String values = null;
        if(StringUtils.isNotEmpty(IdCode)) {
            long start = System.currentTimeMillis();
            log.debug("Get Data from Redis Cache for {} - {}",byType,IdCode);
            Jedis jedis = null;
            try {
                jedis = jedisFactory.getJedis();
                values = jedis.get(CommonUtil.generateKey(byType, IdCode));
                long end = System.currentTimeMillis();
                if(StringUtils.isBlank(values)) {
                    log.debug("Data not found in Redis Cache for {} - {}", byType, IdCode);
                    log.info("Time taken for redis cache read -Miss {}- {} : {} ms",byType,IdCode, (end-start));
                } else {
                    log.debug("Data  found in Redis Cache for {} - {}", byType, IdCode);
                    log.info("Time taken for redis cache read - Hit {}- {} : {} ms",byType,IdCode, (end-start));
                }

            }finally {
                if(null != jedis) {
                    jedis.close();
                }
            }
         }
         return values;
    }
    protected List<String> getValuesByIndex(String byType, List<String> IdCodes){


        List<String> valueList = null;
        if(CollectionUtils.isNotEmpty(IdCodes)) {
            long start = System.currentTimeMillis();
            log.debug("Get Data from Redis Cache for {} - {}",byType,IdCodes);
            Jedis jedis = null;
            try {
                jedis = jedisFactory.getJedis();
                Pipeline pipeline = jedis.pipelined();
                for (String idCode : IdCodes) {
                    pipeline.get(CommonUtil.generateKey(byType, idCode));
                }
                List<Object> valueObjList = pipeline.syncAndReturnAll();
                if (CollectionUtils.isNotEmpty(valueObjList)) {
                    valueList = valueObjList.stream().filter(x -> null != x).map(Object::toString).collect(Collectors.toList());
                }
                long end = System.currentTimeMillis();
                if (CollectionUtils.isEmpty(valueList)) {
                    log.debug("Data not found in Redis Cache for {} - {}", byType, IdCodes);
                    log.info("Time taken for redis cache read -Miss  {}- {} : {} ms",byType,IdCodes, (end-start));
                } else {
                    log.debug("Data  found in Redis Cache for {} - {}", byType, IdCodes);
                    log.info("Time taken for redis cache read - Hit {}- {} : {} ms",byType,IdCodes, (end-start));
                }
            }  finally {
                if(null != jedis) {
                    jedis.close();
                }
            }
        }
        return valueList;
    }

}
