--获取KEY
local setKey = KEYS[1]
local playerName = KEYS[2]
-- 获取ARGV
local matchCount = tonumber(ARGV[1])

--返回的变量
local result = {}

--如果有匹配队列就先移除旧的
local oldKey = redis.call("hget","PLAYER_MATCH",playerName)
if oldKey
then
    redis.call("srem",oldKey,playerName)
   end

redis.call("sadd",setKey,playerName)
redis.call("hset","PLAYER_MATCH",playerName,setKey)
local size = redis.call("scard",setKey)
if(size>=matchCount)
then
    local players = redis.call("spop",setKey,matchCount)
    for i, v in ipairs(players) do
        redis.call("hdel","PLAYER_MATCH",playerName)
    end
    return players
else
    return result
end