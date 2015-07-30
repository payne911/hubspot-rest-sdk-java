package com.smartling.connector.hubspot.sdk.rest.token;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.redisson.Config;
import org.redisson.Redisson;
import org.redisson.core.RBucket;
import org.redisson.core.RLock;

import com.smartling.connector.hubspot.sdk.HubspotApiException;
import com.smartling.connector.hubspot.sdk.RefreshTokenData;
import com.smartling.connector.hubspot.sdk.rest.HubspotRestClient.Configuration;

public class RedisCachedTokenProvider implements TokenProvider
{
    public static final String REDIS_SINGLE_SERVER_ADDRESS = "redis.singleServer.address";

    private static final int TRY_DELAY = 10000;
    private static final String ACCESSKEY_NAME = "com.smartling.connector.hubspot.%s.accesskey";
    private static final String ACCESSKEY_LOCK_NAME = "com.smartling.connector.hubspot.%s.accesskey.lock";

    private final TokenProvider tokenProvider;
    private final String clientId;
    private final String redisSingleServerAddress;

    public RedisCachedTokenProvider(Configuration configuration, TokenProvider tokenProvider) throws HubspotApiException
    {
        this.clientId = configuration.getClientId();
        this.tokenProvider = tokenProvider;
        this.redisSingleServerAddress = configuration.getPropertyValue(REDIS_SINGLE_SERVER_ADDRESS);
        if (null == this.redisSingleServerAddress || this.redisSingleServerAddress.isEmpty())
            throw new HubspotApiException("TokenProvider decorator " + this.getClass().getName() + " cannot be initialized from the configuration "+ configuration);
    }

    @Override
    public RefreshTokenData getTokenData() throws HubspotApiException
    {
        RefreshTokenData token = null;
        Redisson redisson = createRedissonClient();
        try
        {
            RBucket<String> bucket = redisson.getBucket(String.format(ACCESSKEY_NAME, this.clientId));
            token = createRefreshToken(bucket);
            if (null == token)
            {
                RLock lock = redisson.getLock(String.format(ACCESSKEY_LOCK_NAME, this.clientId));
                if (lock.tryLock(TRY_DELAY, TRY_DELAY, TimeUnit.MILLISECONDS))
                {
                    try
                    {
                        token = createRefreshToken(bucket);
                        if (null == token)
                        {
                            token = this.tokenProvider.getTokenData();
                            bucket.set(token.getAccessToken(), expireCacheInSeconds(token.getExpiresIn()), TimeUnit.SECONDS);
                        }
                    }
                    finally
                    {
                        lock.unlock();
                    }
                }
                else
                {
                    throw new HubspotApiException("Cache locking is failed");
                }
            }
        }
        catch(InterruptedException e)
        {
            Thread.currentThread().interrupt();
            throw new HubspotApiException("Cache locking was interrupted", e);
        }
        finally
        {
            shutdown(redisson);
        }
        return token;
    }

    protected long expireCacheInSeconds(int expireTokenInSeconds)
    {
        return expireTokenInSeconds;
    }

    private RefreshTokenData createRefreshToken(RBucket<String> bucket)
    {
        RefreshTokenData token = null;
        if (bucket.exists())
        {
            token = new RefreshTokenData();
            token.setAccessToken(bucket.get());
            token.setExpiresIn((int)bucket.remainTimeToLive());
        }
        return token;
    }

    protected Redisson createRedissonClient()
    {
        Config config = new Config();
        config.useSingleServer().setAddress(this.redisSingleServerAddress);
        return Redisson.create(config);
    }

    protected ExecutorService createExecutorService()
    {
        return Executors.newSingleThreadExecutor();
    }

    protected void shutdown(Redisson redisson)
    {
        ExecutorService shutdownExecutor = createExecutorService();
        shutdownExecutor.execute(new Runnable()
        {
            @Override
            public void run()
            {
                redisson.shutdown();
            }
        });
        shutdownExecutor.shutdown();
    }
}
