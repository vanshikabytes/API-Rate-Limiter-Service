-- Rate Limiter Lua Script
-- KEYS[1]: bucket key
-- ARGV[1]: capacity
-- ARGV[2]: refillRate (tokens per window)
-- ARGV[3]: windowSeconds
-- ARGV[4]: currentTimeSeconds
-- ARGV[5]: requestedTokens

local key = KEYS[1]
local capacity = tonumber(ARGV[1])
local refillRate = tonumber(ARGV[2])
local windowSeconds = tonumber(ARGV[3])
local now = tonumber(ARGV[4])
local requested = tonumber(ARGV[5])

local refillRatePerSecond = refillRate / windowSeconds

local data = redis.call('HMGET', key, 'tokens', 'lastRefillTime')
local tokens = tonumber(data[1])
local lastRefillTime = tonumber(data[2])

if tokens == nil then
    tokens = capacity
    lastRefillTime = now
else
    local elapsed = now - lastRefillTime
    if elapsed > 0 then
        local tokensToAdd = elapsed * refillRatePerSecond
        tokens = math.min(capacity, tokens + tokensToAdd)
        lastRefillTime = now
    end
end

local allowed = 0
if tokens >= requested then
    tokens = tokens - requested
    allowed = 1
end

redis.call('HMSET', key, 'tokens', tokens, 'lastRefillTime', lastRefillTime)
redis.call('EXPIRE', key, math.floor(windowSeconds * 2))

return {allowed, math.floor(tokens)}
